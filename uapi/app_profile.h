#ifndef __MYSU_UAPI_APP_PROFILE_H
#define __MYSU_UAPI_APP_PROFILE_H

#include <linux/types.h>

#define MYSU_APP_PROFILE_VER 4
#define MYSU_MAX_PACKAGE_NAME 256
/* NGROUPS_MAX for Linux is 65535 generally, but we only supports 32 groups. */
#define MYSU_MAX_GROUPS 32
#define MYSU_SELINUX_DOMAIN 64

#define FLAG_MYSU_NO_NEW_PRIVS (1ULL << 0)

/* Compatibility aliases */

struct root_profile {
    __s32 uid;
    __s32 gid;

    __u32 groups_count;
    __s32 groups[MYSU_MAX_GROUPS];

    /* kernel_cap_t is u32[2] for capabilities v3 */
    struct {
        __u64 effective;
        __u64 permitted;
        __u64 inheritable;
    } capabilities;

    char selinux_domain[MYSU_SELINUX_DOMAIN];

    __s32 namespaces;

    __u64 flags;
};

struct non_root_profile {
    bool umount_modules;
};

struct app_profile {
    /*
     * It may be utilized for backward compatibility, although we have never
     * explicitly made any promises regarding this.
     */
    __u32 version;

    /* this is usually the package of the app, but can be other value for special apps */
    char key[MYSU_MAX_PACKAGE_NAME];
    __s32 curr_uid;
    bool allow_su;

    union {
        struct {
            bool use_default;
            char template_name[MYSU_MAX_PACKAGE_NAME];

            struct root_profile profile;
        } rp_config;

        struct {
            bool use_default;

            struct non_root_profile profile;
        } nrp_config;
    };
};

#endif
