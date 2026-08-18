#ifndef __MYSU_H_SELINUX_HIDE
#define __MYSU_H_SELINUX_HIDE

void mysu_selinux_hide_init();
void mysu_selinux_hide_exit();
void mysu_selinux_hide_drop_backup_if_unused();
void mysu_selinux_hide_handle_second_stage();
void mysu_selinux_hide_handle_post_fs_data();

#endif
