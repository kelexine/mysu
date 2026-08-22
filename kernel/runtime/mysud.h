#ifndef __MYSU_H_MYSUD
#define __MYSU_H_MYSUD

#define MYSUD_PATH "/data/adb/mysud"

struct pt_regs;
struct user_arg_ptr;

extern bool mysu_no_custom_rc;

void mysu_mysud_init(void);
void mysu_mysud_exit(void);

void mysu_execve_hook_mysud(const struct pt_regs *regs);
void mysu_execveat_hook_mysud(const struct pt_regs *regs);
void mysu_handle_execveat_mysud(const char *path, struct user_arg_ptr *argv);
void mysu_stop_input_hook_runtime(void);

#endif
