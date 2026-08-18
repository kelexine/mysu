#ifndef __MYSU_H_SUCOMPAT
#define __MYSU_H_SUCOMPAT
#include <asm/ptrace.h>
#include <linux/types.h>

extern bool mysu_su_compat_enabled;

void mysu_sucompat_init(void);
void mysu_sucompat_exit(void);

// Handler functions exported for hook_manager
long mysu_handle_faccessat_sucompat(int orig_nr, struct pt_regs *regs);
long mysu_handle_stat_sucompat(int orig_nr, struct pt_regs *regs);
long mysu_handle_execve_sucompat(const char __user **filename_user, int orig_nr, struct pt_regs *regs);
long mysu_handle_execveat_sucompat(const char __user **filename_user, int orig_nr, struct pt_regs *regs);

#endif
