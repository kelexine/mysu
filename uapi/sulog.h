#ifndef __MYSU_UAPI_SULOG_H
#define __MYSU_UAPI_SULOG_H

#include <linux/sched.h>
#include <linux/types.h>

#define MYSU_SULOG_EVENT_VERSION 1
#ifndef TASK_COMM_LEN
#define TASK_COMM_LEN 16
#endif

enum mysu_sulog_event_type {
    MYSU_SULOG_EVENT_ROOT_EXECVE = 1,
    MYSU_SULOG_EVENT_SUCOMPAT = 2,
    MYSU_SULOG_EVENT_IOCTL_GRANT_ROOT = 3,
};

struct mysu_sulog_event {
    __u16 version;
    __u16 event_type;
    __s32 retval;
    __u32 pid;
    __u32 tgid;
    __u32 ppid;
    __u32 uid;
    __u32 euid;
    char comm[TASK_COMM_LEN];
    __u32 filename_len;
    __u32 argv_len;
} __packed;

/* Compatibility aliases */
#define MYSU_SULOG_EVENT_VERSION MYSU_SULOG_EVENT_VERSION
#define mysu_sulog_event_type mysu_sulog_event_type
#define MYSU_SULOG_EVENT_ROOT_EXECVE MYSU_SULOG_EVENT_ROOT_EXECVE
#define MYSU_SULOG_EVENT_SUCOMPAT MYSU_SULOG_EVENT_SUCOMPAT
#define MYSU_SULOG_EVENT_IOCTL_GRANT_ROOT MYSU_SULOG_EVENT_IOCTL_GRANT_ROOT
#define mysu_sulog_event mysu_sulog_event

#endif
