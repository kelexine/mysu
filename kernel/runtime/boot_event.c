#include "feature/selinux_hide.h"
#include <linux/err.h>
#include <linux/fs.h>
#include <linux/namei.h>
#include <linux/printk.h>

#include "policy/allowlist.h"
#include "klog.h" // IWYU pragma: keep
#include "runtime/mysud_boot.h"
#include "runtime/mysud.h"
#include "manager/manager_observer.h"
#include "manager/throne_tracker.h"

bool mysu_module_mounted __read_mostly = false;
bool mysu_boot_completed __read_mostly = false;

void on_post_fs_data(void)
{
    static bool done = false;

    if (done) {
        pr_info("on_post_fs_data already done\n");
        return;
    }

    done = true;
    pr_info("on_post_fs_data!\n");

    mysu_load_allow_list();
    mysu_observer_init();
    // Sanity check for safe mode only needs early-boot input samples.
    mysu_stop_input_hook_runtime();
    mysu_selinux_hide_handle_post_fs_data();
}

extern void ext4_unregister_sysfs(struct super_block *sb);

int nuke_ext4_sysfs(const char *mnt)
{
    struct path path;
    int err = kern_path(mnt, 0, &path);

    if (err) {
        pr_err("nuke path err: %d\n", err);
        return err;
    }

    if (strcmp(path.dentry->d_inode->i_sb->s_type->name, "ext4") != 0) {
        pr_info("nuke but module aren't mounted\n");
        path_put(&path);
        return -EINVAL;
    }

    ext4_unregister_sysfs(path.dentry->d_inode->i_sb);
    path_put(&path);
    return 0;
}

void on_module_mounted(void)
{
    pr_info("on_module_mounted!\n");
    mysu_module_mounted = true;
}

void on_boot_completed(void)
{
    mysu_boot_completed = true;
    pr_info("on_boot_completed!\n");
    track_throne(true);
    mysu_selinux_hide_drop_backup_if_unused();
}
