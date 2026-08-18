#ifndef __MYSU_H_SUPERCALL
#define __MYSU_H_SUPERCALL

#include <linux/types.h>
#include <linux/uaccess.h>

// IOCTL handler types
typedef int (*mysu_ioctl_handler_t)(void __user *arg);
typedef bool (*mysu_perm_check_t)(void);

// IOCTL command mapping
struct mysu_ioctl_cmd_map {
    unsigned int cmd;
    const char *name;
    mysu_ioctl_handler_t handler;
    mysu_perm_check_t perm_check; // Permission check function
};

// Install MYSU fd to current process
int mysu_install_fd(void);

void mysu_supercalls_init(void);
void mysu_supercalls_exit(void);
#endif // __MYSU_H_SUPERCALL
