#ifndef __MYSU_H_SETUID_HOOK
#define __MYSU_H_SETUID_HOOK

#include <linux/init.h>
#include <linux/types.h>

void mysu_setuid_hook_init(void);
void mysu_setuid_hook_exit(void);

// Handler functions for hook_manager
int mysu_handle_setresuid(uid_t old_uid, uid_t new_uid);

#endif
