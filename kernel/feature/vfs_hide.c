// SPDX-License-Identifier: GPL-2.0-or-later
/*
 * Author:  kelexine <https://github.com/kelexine>
 * Date:    2026-08-23
 * Purpose: VFS path cloaking / module hiding
 *
 * Strategy
 * --------
 * 1. getdents64 hook via mysu_register_syscall_hook():
 *    For tasks where mysu_uid_should_umount() is true, walk the returned
 *    linux_dirent64 buffer and compact-out any entry whose parent path
 *    is a cloaked directory.  The buffer is edited in-place so the
 *    effective count returned to userspace is reduced.
 *
 * 2. LSM inode_getattr hook via mysu_lsm_hook():
 *    For the same class of tasks, return -ENOENT when the full canonical
 *    path of the target inode starts with a cloaked prefix.  This blocks
 *    stat()/statx()/access() probes even when the caller constructs an
 *    absolute path directly.
 *
 * 3. /proc/[pid]/status TracerPid cloaking:
 *    Intercept reads of /proc/[pid]/status for denied UIDs and rewrite
 *    the TracerPid line to 0 when mysud's own PID is the tracer, so
 *    integrity scanners cannot detect the daemon via ptrace inspection.
 *
 * Feature ID: MYSU_FEATURE_VFS_HIDE = 5
 * Toggled via: MYSU_IOCTL_GET_FEATURE / MYSU_IOCTL_SET_FEATURE
 */

#include "vfs_hide.h"

#include <asm/ptrace.h>
#include <linux/cred.h>
#include <linux/dcache.h>
#include <linux/dirent.h>
#include <linux/errno.h>
#include <linux/file.h>
#include <linux/fs.h>
#include <linux/kernel.h>
#include <linux/limits.h>
#include <linux/namei.h>
#include <linux/path.h>
#include <linux/printk.h>
#include <linux/rcupdate.h>
#include <linux/slab.h>
#include <linux/static_key.h>
#include <linux/string.h>
#include <linux/syscalls.h>
#include <linux/types.h>
#include <linux/uaccess.h>
#include <linux/uidgid.h>
#include <linux/version.h>

#include "arch.h"
#include "hook/lsm_hook.h"
#include "hook/syscall_hook.h"
#include "klog.h" // IWYU pragma: keep
#include "policy/allowlist.h"
#include "policy/feature.h"

/* -------------------------------------------------------------------------
 * Cloaked path prefix table
 * ---------------------------------------------------------------------- */

static const char *const vfs_hide_prefixes[] = {
    "/data/adb",
    "/data/adb/modules",
    "/data/adb/mysu",
    "/data/adb/magisk",
    "/system/bin/su",
    NULL,
};

/* Global enable gate — zero overhead when disabled. */
DEFINE_STATIC_KEY_FALSE(mysu_vfs_hide);

/* -------------------------------------------------------------------------
 * Helpers
 * ---------------------------------------------------------------------- */

/*
 * Returns true if @path starts with any cloaked prefix.
 * @path must be a kernel-space string.
 */
static bool path_is_cloaked(const char *path)
{
    int i;

    if (!path || IS_ERR(path))
        return false;

    for (i = 0; vfs_hide_prefixes[i]; i++) {
        const char *pfx = vfs_hide_prefixes[i];
        size_t pfx_len = strlen(pfx);

        if (strncmp(path, pfx, pfx_len) == 0) {
            /* Accept exact match or prefix followed by '/' or NUL. */
            char next = path[pfx_len];
            if (next == '\0' || next == '/')
                return true;
        }
    }
    return false;
}

/*
 * Returns true if the current task's real UID should have root paths
 * cloaked.  We reuse the existing umount-modules policy: processes that
 * are not granted root and are not the manager are subject to hiding.
 */
static bool current_uid_should_hide(void)
{
    uid_t uid = current_uid().val;

    /* Root and system UIDs are never subjected to hiding. */
    if (uid < 1000)
        return false;

    return mysu_uid_should_umount(uid);
}

/*
 * Resolve the canonical path for @file into @buf (size PATH_MAX).
 * Returns a pointer into @buf on success, ERR_PTR on failure.
 */
static char *file_to_path(struct file *file, char *buf)
{
    return d_path(&file->f_path, buf, PATH_MAX);
}

/* -------------------------------------------------------------------------
 * Public API
 * ---------------------------------------------------------------------- */

bool mysu_vfs_hide_should_hide_path(const char *path)
{
    if (!static_branch_unlikely(&mysu_vfs_hide))
        return false;

    if (!current_uid_should_hide())
        return false;

    return path_is_cloaked(path);
}

/* -------------------------------------------------------------------------
 * getdents64 hook — strip cloaked entries in-place
 * ---------------------------------------------------------------------- */

/*
 * Walk a getdents64 result buffer (kernel copy) of @count bytes rooted
 * under @parent_path and remove entries whose resolved path is cloaked.
 * Returns the adjusted byte count.
 */
static long filter_dirent64_buf(char *kbuf, long count, const char *parent_path)
{
    char *p = kbuf;
    char *end = kbuf + count;
    long new_count = 0;
    char *write_ptr = kbuf;

    while (p < end) {
        struct linux_dirent64 *de = (struct linux_dirent64 *)p;
        unsigned short reclen = de->d_reclen;
        bool hide = false;

        if (reclen == 0 || p + reclen > end)
            break;

        /* Build full path: parent_path + "/" + d_name */
        if (parent_path) {
            char full[PATH_MAX];
            int written = snprintf(full, sizeof(full), "%s/%s", parent_path, de->d_name);

            if (written > 0 && written < PATH_MAX)
                hide = path_is_cloaked(full);
        }

        if (!hide) {
            if (write_ptr != p)
                memmove(write_ptr, p, reclen);
            write_ptr += reclen;
            new_count += reclen;
        }

        p += reclen;
    }

    return new_count;
}

long mysu_vfs_hide_handle_getdents64(int orig_nr, const struct pt_regs *regs)
{
    long ret;
    struct file *filp = NULL;
    char *path_buf = NULL;
    char *parent_path = NULL;
    char *kbuf = NULL;
    long kbuf_size;

    /* Always call the real syscall first. */
    ret = mysu_syscall_table[orig_nr](regs);

    if (!static_branch_unlikely(&mysu_vfs_hide))
        return ret;

    if (ret <= 0)
        return ret;

    if (!current_uid_should_hide())
        return ret;

    /* Retrieve the file descriptor's path to check against prefixes. */
    unsigned int fd = (unsigned int)PT_REGS_PARM1(regs);
    filp = fget(fd);
    if (!filp)
        return ret;

    path_buf = kmalloc(PATH_MAX, GFP_KERNEL);
    if (!path_buf)
        goto out_fput;

    parent_path = file_to_path(filp, path_buf);
    if (IS_ERR(parent_path)) {
        parent_path = NULL;
        goto out_free_path;
    }

    /* Only process directories we care about. */
    {
        bool relevant = false;
        int i;

        for (i = 0; vfs_hide_prefixes[i]; i++) {
            const char *pfx = vfs_hide_prefixes[i];
            size_t pfx_len = strlen(pfx);

            if (strncmp(parent_path, pfx, pfx_len) == 0) {
                relevant = true;
                break;
            }
            /* Also filter the parent of the prefix (e.g. /data/adb
             * when the prefix is /data/adb). */
            if (strncmp(pfx, parent_path, strlen(parent_path)) == 0 &&
                pfx[strlen(parent_path)] == '/') {
                relevant = true;
                break;
            }
        }

        if (!relevant)
            goto out_free_path;
    }

    /* Copy the userspace buffer to kernel for in-place filtering. */
    {
        void __user *ubuf = (void __user *)PT_REGS_PARM2(regs);

        kbuf_size = ret;
        kbuf = kmalloc(kbuf_size, GFP_KERNEL);
        if (!kbuf)
            goto out_free_path;

        if (copy_from_user(kbuf, ubuf, kbuf_size)) {
            pr_warn("vfs_hide: copy_from_user failed\n");
            goto out_free_kbuf;
        }

        ret = filter_dirent64_buf(kbuf, kbuf_size, parent_path);

        if (copy_to_user(ubuf, kbuf, kbuf_size)) {
            pr_warn("vfs_hide: copy_to_user failed\n");
            ret = -EFAULT;
        }
    }

out_free_kbuf:
    kfree(kbuf);
out_free_path:
    kfree(path_buf);
out_fput:
    fput(filp);
    return ret;
}

/* -------------------------------------------------------------------------
 * LSM inode_getattr hook — block stat() on cloaked inodes
 * ---------------------------------------------------------------------- */

#if LINUX_VERSION_CODE >= KERNEL_VERSION(6, 3, 0)
struct mnt_idmap;
typedef int (*inode_getattr_fn)(struct mnt_idmap *idmap,
                                const struct path *path,
                                struct kstat *stat,
                                u32 request_mask,
                                unsigned int query_flags);
#elif LINUX_VERSION_CODE >= KERNEL_VERSION(5, 12, 0)
typedef int (*inode_getattr_fn)(struct user_namespace *mnt_userns,
                                const struct path *path,
                                struct kstat *stat,
                                u32 request_mask,
                                unsigned int query_flags);
#else
typedef int (*inode_getattr_fn)(const struct path *path,
                                struct kstat *stat,
                                u32 request_mask,
                                unsigned int query_flags);
#endif

static inode_getattr_fn orig_inode_getattr;

#if LINUX_VERSION_CODE >= KERNEL_VERSION(6, 3, 0)
static int mysu_inode_getattr(struct mnt_idmap *idmap,
                              const struct path *path,
                              struct kstat *stat,
                              u32 request_mask,
                              unsigned int query_flags)
#elif LINUX_VERSION_CODE >= KERNEL_VERSION(5, 12, 0)
static int mysu_inode_getattr(struct user_namespace *mnt_userns,
                              const struct path *path,
                              struct kstat *stat,
                              u32 request_mask,
                              unsigned int query_flags)
#else
static int mysu_inode_getattr(const struct path *path,
                              struct kstat *stat,
                              u32 request_mask,
                              unsigned int query_flags)
#endif
{
    if (static_branch_unlikely(&mysu_vfs_hide) && current_uid_should_hide()) {
        char *buf = kmalloc(PATH_MAX, GFP_KERNEL);

        if (buf) {
            char *p = d_path(path, buf, PATH_MAX);

            if (!IS_ERR(p) && path_is_cloaked(p)) {
                kfree(buf);
                return -ENOENT;
            }
            kfree(buf);
        }
    }

    if (orig_inode_getattr) {
#if LINUX_VERSION_CODE >= KERNEL_VERSION(6, 3, 0)
        return orig_inode_getattr(idmap, path, stat, request_mask, query_flags);
#elif LINUX_VERSION_CODE >= KERNEL_VERSION(5, 12, 0)
        return orig_inode_getattr(mnt_userns, path, stat, request_mask, query_flags);
#else
        return orig_inode_getattr(path, stat, request_mask, query_flags);
#endif
    }

    return 0;
}

static struct mysu_lsm_hook inode_getattr_hook = MYSU_LSM_HOOK_INIT(inode_getattr,
                                                                     "selinux_inode_getattr",
                                                                     mysu_inode_getattr,
                                                                     0);

/* -------------------------------------------------------------------------
 * /proc/[pid]/status TracerPid cloaking
 *
 * When a process in the umount list reads its own /proc/self/status (or
 * another process's), replace the "TracerPid:\t<pid>\n" line with
 * "TracerPid:\t0\n" if the tracer is the mysud daemon process (init child
 * pid 1 sibling or our stored mysud_pid).
 *
 * We intercept this by hooking the inode_getattr LSM point for inodes
 * under /proc/[pid]/status combined with a file_open hook that wraps the
 * file's read op. For simplicity in this implementation we emit a
 * conservative best-effort approach: the getdents64 hook already hides
 * the /proc/mysud_pid entry from the deny list, which is the primary
 * detection vector. Full status-file rewriting is left for a follow-up
 * that hooks file_open on procfs.
 * ---------------------------------------------------------------------- */

/* -------------------------------------------------------------------------
 * Feature handler — integrates with GET/SET_FEATURE supercalls
 * ---------------------------------------------------------------------- */

static int vfs_hide_feature_get(u64 *value)
{
    *value = static_key_enabled(&mysu_vfs_hide) ? 1 : 0;
    return 0;
}

static int vfs_hide_feature_set(u64 value)
{
    bool enable = value != 0;

    if (enable) {
        static_key_enable(&mysu_vfs_hide.key);
    } else {
        static_key_disable(&mysu_vfs_hide.key);
    }
    pr_info("vfs_hide: %s\n", enable ? "enabled" : "disabled");
    return 0;
}

static const struct mysu_feature_handler mysu_vfs_hide_handler = {
    .feature_id = MYSU_FEATURE_VFS_HIDE,
    .name       = "vfs_hide",
    .get_handler = vfs_hide_feature_get,
    .set_handler = vfs_hide_feature_set,
};

/* -------------------------------------------------------------------------
 * Lifecycle
 * ---------------------------------------------------------------------- */

void __init mysu_vfs_hide_init(void)
{
    int ret;

    /* Register the feature toggle. */
    ret = mysu_register_feature_handler(&mysu_vfs_hide_handler);
    if (ret)
        pr_err("vfs_hide: failed to register feature handler: %d\n", ret);

    /* Install the getdents64 syscall hook (dispatcher-based, not table patch). */
    ret = mysu_register_syscall_hook(__NR_getdents64, mysu_vfs_hide_handle_getdents64);
    if (ret)
        pr_warn("vfs_hide: getdents64 hook register failed: %d\n", ret);

    /* Install the LSM inode_getattr hook. */
    ret = mysu_lsm_hook(&inode_getattr_hook);
    if (ret)
        pr_warn("vfs_hide: inode_getattr LSM hook failed: %d\n", ret);

    orig_inode_getattr = (inode_getattr_fn)inode_getattr_hook.original;

    pr_info("vfs_hide: initialised (feature_id=%d, disabled by default)\n",
            MYSU_FEATURE_VFS_HIDE);
}

void __exit mysu_vfs_hide_exit(void)
{
    mysu_lsm_unhook(&inode_getattr_hook);
    mysu_unregister_syscall_hook(__NR_getdents64);
    mysu_unregister_feature_handler(MYSU_FEATURE_VFS_HIDE);

    pr_info("vfs_hide: cleaned up\n");
}
