#ifndef __MYSU_H_HOOK_MANAGER
#define __MYSU_H_HOOK_MANAGER

#include <asm/ptrace.h>

// Hook manager initialization and cleanup
void mysu_syscall_hook_manager_init(void);
void mysu_syscall_hook_manager_exit(void);

#endif
