#include <asm/current.h>
#include <linux/compat.h>
#include <linux/cred.h>
#include <linux/gfp.h>
#include <linux/version.h>
#if LINUX_VERSION_CODE >= KERNEL_VERSION(5, 12, 0)
#include <linux/minmax.h>
#else
#include <linux/kernel.h>
#endif
#include <linux/overflow.h>
#include <linux/sched/signal.h>
#include <linux/slab.h>
#include <linux/string.h>
#include <linux/uaccess.h>

#if defined(__x86_64__) && LINUX_VERSION_CODE < KERNEL_VERSION(6, 2, 0)
#include <linux/mm.h>
#endif

#include "feature/sulog.h"
#include "infra/event_queue.h"
#include "klog.h" // IWYU pragma: keep
#include "sulog/event.h"
#include "util.h"

#define MYSU_SULOG_MAX_QUEUED 256U
#define MYSU_SULOG_MAX_PAYLOAD_LEN 2048U
#define MYSU_SULOG_MAX_ARG_STRINGS 0x7FFFFFFF
#define MYSU_SULOG_MAX_ARG_CHUNK 256U
#define MYSU_SULOG_MAX_FILENAME_LEN 256U

struct user_arg_ptr {
#ifdef CONFIG_COMPAT
    bool is_compat;
#endif
    union {
        const char __user *const __user *native;
#ifdef CONFIG_COMPAT
        const compat_uptr_t __user *compat;
#endif
    } ptr;
};

static struct mysu_event_queue sulog_queue;

struct mysu_sulog_pending_event {
    __u16 event_type;
    void *payload;
    __u32 payload_len;
};

struct mysu_sulog_identity {
    __u32 uid;
    __u32 euid;
};

static struct user_arg_ptr mysu_sulog_user_argv(const char __user *const __user *argv_user)
{
    struct user_arg_ptr argv;

#ifdef CONFIG_COMPAT
    if (unlikely(in_compat_syscall())) {
        argv.is_compat = true;
        argv.ptr.compat = (const compat_uptr_t __user *)argv_user;
        return argv;
    }

    argv.is_compat = false;
#endif
    argv.ptr.native = argv_user;
    return argv;
}

static const char __user *mysu_sulog_get_user_arg_ptr(struct user_arg_ptr argv, int nr)
{
    const char __user *native;

#ifdef CONFIG_COMPAT
    if (unlikely(argv.is_compat)) {
        compat_uptr_t compat;

        if (get_user(compat, argv.ptr.compat + nr))
            return ERR_PTR(-EFAULT);

        return compat_ptr(compat);
    }
#endif

    if (get_user(native, argv.ptr.native + nr))
        return ERR_PTR(-EFAULT);

    return native;
}

static void mysu_sulog_fill_task_info(struct mysu_sulog_event *event, __u16 event_type, int retval)
{
    event->version = MYSU_SULOG_EVENT_VERSION;
    event->event_type = event_type;
    event->retval = retval;
    event->pid = task_pid_nr(current);
    event->tgid = task_tgid_nr(current);
    event->ppid = task_ppid_nr(current);
    event->uid = current_uid().val;
    event->euid = current_euid().val;
    get_task_comm(event->comm, current);
}

static void mysu_sulog_set_identity(struct mysu_sulog_event *event, const struct mysu_sulog_identity *identity)
{
    if (!identity)
        return;

    event->uid = identity->uid;
    event->euid = identity->euid;
}

static __u32 mysu_sulog_copy_empty_string(char *dst)
{
    dst[0] = '\0';
    return 1;
}

static __u32 mysu_sulog_copy_filename(const char __user *filename_user, char *dst, __u32 dst_len)
{
    long ret;

    if (!dst_len)
        return 0;

    if (!filename_user)
        return mysu_sulog_copy_empty_string(dst);

    ret = strncpy_from_user_nofault(dst, (const void __user *)untagged_addr((unsigned long)filename_user), dst_len);
    if (ret <= 0)
        return mysu_sulog_copy_empty_string(dst);

    if (ret >= dst_len) {
        dst[dst_len - 1] = '\0';
        return dst_len;
    }

    return ret + 1;
}

static __u32 mysu_sulog_flatten_argv(const char __user *const __user *argv_user, char *dst, __u32 dst_len)
{
    struct user_arg_ptr argv = mysu_sulog_user_argv(argv_user);
    char arg[MYSU_SULOG_MAX_ARG_CHUNK];
    __u32 used = 0;
    int i;

    if (!dst_len)
        return 0;

    if (!argv_user)
        return mysu_sulog_copy_empty_string(dst);

    for (i = 0; i < MYSU_SULOG_MAX_ARG_STRINGS; i++) {
        const char __user *arg_user;
        long copied;
        size_t arg_len;

        if (fatal_signal_pending(current))
            break;

        arg_user = mysu_sulog_get_user_arg_ptr(argv, i);
        if (!arg_user)
            break;
        if (IS_ERR(arg_user))
            return mysu_sulog_copy_empty_string(dst);

        copied =
            strncpy_from_user_nofault(arg, (const void __user *)untagged_addr((unsigned long)arg_user), sizeof(arg));
        if (copied <= 0)
            return mysu_sulog_copy_empty_string(dst);

        if (copied >= sizeof(arg))
            arg[sizeof(arg) - 1] = '\0';

        arg_len = strnlen(arg, sizeof(arg));
        if (!arg_len)
            continue;

        if (used && used < dst_len - 1)
            dst[used++] = ' ';

        if (used >= dst_len - 1)
            break;

        arg_len = min_t(size_t, arg_len, dst_len - used - 1);
        memcpy(dst + used, arg, arg_len);
        used += arg_len;

        if (used >= dst_len - 1)
            break;
    }

    dst[used] = '\0';
    return used + 1;
}

static struct mysu_sulog_pending_event *mysu_sulog_capture(__u16 event_type, const char __user *filename_user,
                                                         const char __user *const __user *argv_user, gfp_t gfp)
{
    struct mysu_sulog_pending_event *pending = NULL;
    struct mysu_sulog_event *event;
    void *payload = NULL;
    __u32 payload_len;
    __u32 filename_len;
    __u32 argv_len;
    __u32 remaining;
    char *filename_buf;
    char *argv_buf;
    if (!mysu_sulog_is_enabled())
        return NULL;

    pending = kzalloc(sizeof(*pending), gfp);
    if (!pending)
        goto out_drop;

    payload = kzalloc(MYSU_SULOG_MAX_PAYLOAD_LEN, gfp);
    if (!payload)
        goto out_free_pending;

    event = payload;
    mysu_sulog_fill_task_info(event, event_type, 0);

    remaining = MYSU_SULOG_MAX_PAYLOAD_LEN - sizeof(*event);
    filename_buf = (char *)payload + sizeof(*event);
    filename_len = mysu_sulog_copy_filename(filename_user, filename_buf, min(remaining, MYSU_SULOG_MAX_FILENAME_LEN));
    if (!filename_len)
        goto out_free_payload;

    remaining -= filename_len;
    argv_buf = filename_buf + filename_len;
    argv_len = mysu_sulog_flatten_argv(argv_user, argv_buf, remaining);
    if (!argv_len)
        goto out_free_payload;

    event->filename_len = filename_len;
    event->argv_len = argv_len;

    if (check_add_overflow((__u32)sizeof(*event), filename_len, &payload_len) ||
        check_add_overflow(payload_len, argv_len, &payload_len))
        goto out_free_payload;

    pending->event_type = event_type;
    pending->payload = payload;
    pending->payload_len = payload_len;
    return pending;

out_free_payload:
    kfree(payload);
out_free_pending:
    kfree(pending);
out_drop:
    mysu_event_queue_drop(&sulog_queue);
    return NULL;
}

static struct mysu_sulog_pending_event *mysu_sulog_capture_grant_root(const struct mysu_sulog_identity *identity,
                                                                    gfp_t gfp)
{
    struct mysu_sulog_pending_event *pending;
    struct mysu_sulog_event *event;

    pending = mysu_sulog_capture(MYSU_SULOG_EVENT_IOCTL_GRANT_ROOT, NULL, NULL, gfp);
    if (!pending)
        return NULL;

    event = pending->payload;
    mysu_sulog_set_identity(event, identity);
    return pending;
}

int __init mysu_sulog_events_init(void)
{
    mysu_event_queue_init(&sulog_queue, MYSU_SULOG_MAX_QUEUED, MYSU_SULOG_MAX_PAYLOAD_LEN);
    return 0;
}

void __exit mysu_sulog_events_exit(void)
{
    mysu_event_queue_destroy(&sulog_queue);
}

static void mysu_sulog_free_pending(struct mysu_sulog_pending_event *pending)
{
    if (!pending)
        return;
    kfree(pending->payload);
    kfree(pending);
}

struct mysu_sulog_pending_event *mysu_sulog_capture_root_execve(const char __user *filename_user,
                                                              const char __user *const __user *argv_user, gfp_t gfp)
{
    return mysu_sulog_capture(MYSU_SULOG_EVENT_ROOT_EXECVE, filename_user, argv_user, gfp);
}

struct mysu_sulog_pending_event *mysu_sulog_capture_sucompat(const char __user *filename_user,
                                                           const char __user *const __user *argv_user, gfp_t gfp)
{
    return mysu_sulog_capture(MYSU_SULOG_EVENT_SUCOMPAT, filename_user, argv_user, gfp);
}

void mysu_sulog_emit_pending(struct mysu_sulog_pending_event *pending, int retval, gfp_t gfp)
{
    struct mysu_sulog_event *event;

    if (!pending)
        return;

    event = pending->payload;
    event->retval = retval;
    mysu_event_queue_push(&sulog_queue, pending->event_type, 0, pending->payload, pending->payload_len, gfp);
    mysu_sulog_free_pending(pending);
}

int mysu_sulog_emit_grant_root(int retval, __u32 uid, __u32 euid, gfp_t gfp)
{
    struct mysu_sulog_pending_event *pending;
    struct mysu_sulog_identity identity = {
        .uid = uid,
        .euid = euid,
    };

    pending = mysu_sulog_capture_grant_root(&identity, gfp);
    if (!pending)
        return 0;

    mysu_sulog_emit_pending(pending, retval, gfp);
    return 0;
}

struct mysu_event_queue *mysu_sulog_get_queue(void)
{
    return &sulog_queue;
}
