#ifndef MYSU_EVENT_QUEUE_H
#define MYSU_EVENT_QUEUE_H

#include <linux/fs.h>
#include <linux/gfp.h>
#include <linux/list.h>
#include <linux/mutex.h>
#include <linux/poll.h>
#include <linux/spinlock.h>
#include <linux/types.h>
#include <linux/wait.h>

#define MYSU_EVENT_RECORD_FLAG_INTERNAL (1U << 0)
#define MYSU_EVENT_QUEUE_TYPE_DROPPED ((__u16)0xFFFF)

struct mysu_event_record_hdr {
    __u16 type;
    __u16 flags;
    __u32 len;
    __u64 seq;
    __u64 ts_ns;
};

struct mysu_event_queue_dropped_info {
    __u64 dropped;
    __u64 first_seq;
    __u64 last_seq;
};

struct mysu_event_queue {
    spinlock_t lock;
    /* The first implementation supports a single reader. */
    struct mutex read_lock;
    struct list_head pending;
    wait_queue_head_t read_wait;
    __u32 queued;
    __u32 max_queued;
    __u32 max_payload_len;
    __u64 next_seq;
    __u64 dropped_total;
    __u64 dropped_pending;
    __u64 dropped_first_seq;
    __u64 dropped_last_seq;
    __u64 dropped_inflight;
    __u64 dropped_inflight_first_seq;
    __u64 dropped_inflight_last_seq;
    bool closed;
};

void mysu_event_queue_init(struct mysu_event_queue *queue, __u32 max_queued, __u32 max_payload_len);
void mysu_event_queue_destroy(struct mysu_event_queue *queue);

int mysu_event_queue_push(struct mysu_event_queue *queue, __u16 type, __u16 flags, const void *payload, __u32 len,
                         gfp_t gfp);
void mysu_event_queue_drop(struct mysu_event_queue *queue);

ssize_t mysu_event_queue_read(struct mysu_event_queue *queue, char __user *buf, size_t count, int file_flags);
__poll_t mysu_event_queue_poll(struct mysu_event_queue *queue, struct file *file, poll_table *wait);

void mysu_event_queue_close(struct mysu_event_queue *queue);
bool mysu_event_queue_has_data(struct mysu_event_queue *queue);

#endif // MYSU_EVENT_QUEUE_H
