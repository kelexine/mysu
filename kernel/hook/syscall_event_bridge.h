#ifndef __MYSU_H_SYSCALL_EVENT_BRIDGE
#define __MYSU_H_SYSCALL_EVENT_BRIDGE

#include <asm/ptrace.h>

long mysu_hook_newfstatat(int orig_nr, const struct pt_regs *regs);
long mysu_hook_faccessat(int orig_nr, const struct pt_regs *regs);
long mysu_hook_execve(int orig_nr, const struct pt_regs *regs);
long mysu_hook_execveat(int orig_nr, const struct pt_regs *regs);
long mysu_hook_setresuid(int orig_nr, const struct pt_regs *regs);

void mysu_stop_mysud_execve_hook(void);

#endif // __MYSU_H_SYSCALL_EVENT_BRIDGE
