/* SPDX-License-Identifier: GPL-2.0-or-later */
/*
 * Author:  kelexine <https://github.com/kelexine>
 * Date:    2026-08-23
 * Purpose: VFS path cloaking / module hiding — public API
 *
 * Hides /data/adb and related root paths from processes in the
 * umount/deny list by intercepting getdents64 and inode_getattr.
 */
#ifndef __MYSU_H_VFS_HIDE
#define __MYSU_H_VFS_HIDE

#include <linux/types.h>
#include <linux/fs.h>
#include <asm/ptrace.h>

/* Initialise the VFS-hide subsystem and register the feature handler. */
void __init mysu_vfs_hide_init(void);

/* Tear down hooks and unregister on module exit. */
void __exit mysu_vfs_hide_exit(void);

/*
 * Returns true if @path should be hidden from the current task.
 * Safe to call from any context that can sleep (uses RCU + allowlist check).
 */
bool mysu_vfs_hide_should_hide_path(const char *path);

/* getdents64 syscall handler — strips cloaked entries from the result. */
long mysu_vfs_hide_handle_getdents64(int orig_nr, const struct pt_regs *regs);

#endif /* __MYSU_H_VFS_HIDE */
