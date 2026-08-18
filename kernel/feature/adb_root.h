#ifndef __MYSU_H_ADB_ROOT
#define __MYSU_H_ADB_ROOT
#include <asm/ptrace.h>

long mysu_adb_root_handle_execve(struct pt_regs *regs);
long mysu_adb_root_handle_execveat(struct pt_regs *regs);

void mysu_adb_root_init(void);

void mysu_adb_root_exit(void);

#endif
