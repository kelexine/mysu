#ifndef __MYSU_H_SULOG_EVENT
#define __MYSU_H_SULOG_EVENT

#include <linux/compiler_types.h>
#include <linux/gfp.h>
#include <linux/types.h>
#include "uapi/sulog.h" // IWYU pragma: keep

struct mysu_event_queue;
struct mysu_sulog_pending_event;

int mysu_sulog_events_init(void);
void mysu_sulog_events_exit(void);

struct mysu_sulog_pending_event *mysu_sulog_capture_root_execve(const char __user *filename_user,
                                                              const char __user *const __user *argv_user, gfp_t gfp);
struct mysu_sulog_pending_event *mysu_sulog_capture_sucompat(const char __user *filename_user,
                                                           const char __user *const __user *argv_user, gfp_t gfp);
void mysu_sulog_emit_pending(struct mysu_sulog_pending_event *pending, int retval, gfp_t gfp);
int mysu_sulog_emit_grant_root(int retval, __u32 uid, __u32 euid, gfp_t gfp);

struct mysu_event_queue *mysu_sulog_get_queue(void);

#endif
