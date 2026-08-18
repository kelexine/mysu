#ifndef __MYSU_H_MANAGER_IDENTITY
#define __MYSU_H_MANAGER_IDENTITY

#include <linux/cred.h>
#include <linux/types.h>

#define MYSU_INVALID_APPID -1
#define MYSU_PER_USER_RANGE 100000

#ifdef CONFIG_MYSU_DISABLE_MANAGER
static inline bool mysu_is_manager_appid_valid()
{
    return true;
}

static inline bool is_manager()
{
    return current_uid().val == 0;
}

static inline bool is_uid_manager(uid_t uid)
{
    return uid == 0;
}

static inline uid_t mysu_get_manager_appid()
{
    return 0;
}

static inline void mysu_set_manager_appid(uid_t appid)
{
    (void)appid;
}

static inline void mysu_invalidate_manager_uid()
{
}
#else
extern uid_t mysu_manager_appid; // DO NOT DIRECT USE

static inline bool mysu_is_manager_appid_valid()
{
    return mysu_manager_appid != MYSU_INVALID_APPID;
}

static inline bool is_manager()
{
    return unlikely(mysu_manager_appid == current_uid().val % MYSU_PER_USER_RANGE);
}

static inline bool is_uid_manager(uid_t uid)
{
    return unlikely(mysu_manager_appid == uid % MYSU_PER_USER_RANGE);
}

static inline uid_t mysu_get_manager_appid()
{
    return mysu_manager_appid;
}

static inline void mysu_set_manager_appid(uid_t appid)
{
    mysu_manager_appid = appid;
}

static inline void mysu_invalidate_manager_uid()
{
    mysu_manager_appid = MYSU_INVALID_APPID;
}
#endif

#endif // __MYSU_H_MANAGER_IDENTITY
