/* SPDX-License-Identifier: GPL-2.0-or-later */
/*
 * Author:  kelexine <https://github.com/kelexine>
 * Date:    2026-09-16
 * Purpose: MYSUFS (SU File System for MySU) Userspace API Definitions
 */
#ifndef __MYSU_UAPI_MYSUFS_H
#define __MYSU_UAPI_MYSUFS_H

#include <linux/types.h>

#define MYSUFS_MAX_LEN_PATHNAME 256
#define MYSUFS_FAKE_CMDLINE_OR_BOOTCONFIG_SIZE 8192
#define MYSUFS_ENABLED_FEATURES_SIZE 8192
#define MYSUFS_MAX_VERSION_BUFSIZE 16
#define MYSUFS_MAX_VARIANT_BUFSIZE 16
#define MYSUFS_UTS_LEN 65

/* kstat spoof flags */
#ifndef KSTAT_SPOOF_INO
#define KSTAT_SPOOF_INO (1 << 0)
#define KSTAT_SPOOF_DEV (1 << 1)
#define KSTAT_SPOOF_NLINK (1 << 2)
#define KSTAT_SPOOF_SIZE (1 << 3)
#define KSTAT_SPOOF_ATIME_TV_SEC (1 << 4)
#define KSTAT_SPOOF_ATIME_TV_NSEC (1 << 5)
#define KSTAT_SPOOF_MTIME_TV_SEC (1 << 6)
#define KSTAT_SPOOF_MTIME_TV_NSEC (1 << 7)
#define KSTAT_SPOOF_CTIME_TV_SEC (1 << 8)
#define KSTAT_SPOOF_CTIME_TV_NSEC (1 << 9)
#define KSTAT_SPOOF_BLOCKS (1 << 10)
#define KSTAT_SPOOF_BLKSIZE (1 << 11)
#endif

/* UID schemes for open redirect */
#ifndef _MYSUFS_UID_SCHEME_DEFINED
#define _MYSUFS_UID_SCHEME_DEFINED
enum mysufs_uid_scheme {
    MYSUFS_UID_NON_APP_PROC = 0,
    MYSUFS_UID_ROOT_PROC_EXCEPT_SU_PROC,
    MYSUFS_UID_NON_SU_PROC,
    MYSUFS_UID_UMOUNTED_APP_PROC,
    MYSUFS_UID_UMOUNTED_PROC,
};
#endif

#ifndef _MYSUFS_STRUCTS_DEFINED
#define _MYSUFS_STRUCTS_DEFINED

struct st_mysufs_sus_path {
    char target_pathname[MYSUFS_MAX_LEN_PATHNAME];
    int err;
};

struct st_mysufs_hide_sus_mnts_for_non_su_procs {
    __u8 enabled;
    int err;
};

struct st_mysufs_sus_kstat {
    int is_statically;
    unsigned long target_ino;
    char target_pathname[MYSUFS_MAX_LEN_PATHNAME];
    unsigned long spoofed_ino;
    unsigned long spoofed_dev;
    unsigned int spoofed_nlink;
    long long spoofed_size;
    long spoofed_atime_tv_sec;
    unsigned long spoofed_atime_tv_nsec;
    long spoofed_mtime_tv_sec;
    unsigned long spoofed_mtime_tv_nsec;
    long spoofed_ctime_tv_sec;
    unsigned long spoofed_ctime_tv_nsec;
    long long spoofed_blocks;
    long spoofed_blksize;
    int flags;
    int err;
};

struct st_mysufs_uname {
    char release[MYSUFS_UTS_LEN];
    char version[MYSUFS_UTS_LEN];
    int err;
};

struct st_mysufs_log {
    __u8 enabled;
    int err;
};

struct st_mysufs_spoof_cmdline_or_bootconfig {
    char fake_cmdline_or_bootconfig[MYSUFS_FAKE_CMDLINE_OR_BOOTCONFIG_SIZE];
    int err;
};

struct st_mysufs_open_redirect {
    char target_pathname[MYSUFS_MAX_LEN_PATHNAME];
    char redirected_pathname[MYSUFS_MAX_LEN_PATHNAME];
    int uid_scheme;
    int err;
};

struct st_mysufs_sus_map {
    char target_pathname[MYSUFS_MAX_LEN_PATHNAME];
    int err;
};

struct st_mysufs_avc_log_spoofing {
    __u8 enabled;
    int err;
};

struct st_mysufs_enabled_features {
    char enabled_features[MYSUFS_ENABLED_FEATURES_SIZE];
    int err;
};

struct st_mysufs_variant {
    char mysufs_variant[MYSUFS_MAX_VARIANT_BUFSIZE];
    int err;
};

struct st_mysufs_version {
    char mysufs_version[MYSUFS_MAX_VERSION_BUFSIZE];
    int err;
};

#endif /* _MYSUFS_STRUCTS_DEFINED */

#endif /* __MYSU_UAPI_MYSUFS_H */
