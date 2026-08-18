#ifndef __MYSU_H_MYSUD_BOOT
#define __MYSU_H_MYSUD_BOOT

#include <linux/types.h>

void on_post_fs_data(void);
void on_module_mounted(void);
void on_boot_completed(void);

bool mysu_is_safe_mode(void);

int nuke_ext4_sysfs(const char *mnt);

extern bool mysu_module_mounted;
extern bool mysu_boot_completed;

#endif // __MYSU_H_MYSUD_BOOT
