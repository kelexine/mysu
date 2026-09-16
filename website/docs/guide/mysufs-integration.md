# Integrating MYSUFS into Custom Kernels

This guide provides step-by-step instructions for integrating the **MYSUFS (SU File System)** stealth engine into custom Linux kernel trees (specifically targeting Linux 4.14, 4.19, 5.4, and modern GKI trees).

---

## Architecture Requirements

MYSUFS operates as a hybrid system:
1. **In-Kernel VFS Hooks (`kernel/`)**: Deep filesystem, memory management, and security subsystem intercepts applied directly to the kernel tree.
2. **MySU Control Plane (`drivers/mysu/`)**: Supercall IOCTL handlers (`MYSU_IOCTL_MYSUFS_*`) dispatched through the anonymous inode descriptor (`[mysu_driver]`).
3. **Userspace Runtime (`mysud`)**: Automatic rule configuration and interactive CLI commands (`mysud mysufs`).

---

## 1. Kernel Configuration (defconfig)

Enable the master switch and desired sub-features in your kernel defconfig (e.g. `arch/arm64/configs/p50_defconfig` or `arch/arm64/configs/vendor/your_defconfig`):

```txt
# Core MySU & Hook Infrastructure
CONFIG_MYSU=y
CONFIG_KPROBES=y
CONFIG_HAVE_KPROBES=y
CONFIG_KPROBE_EVENTS=y

# MYSUFS Stealth Subsystem
CONFIG_MYSU_MYSUFS=y
CONFIG_MYSU_MYSUFS_SUS_PATH=y
CONFIG_MYSU_MYSUFS_SUS_MOUNT=y
CONFIG_MYSU_MYSUFS_SUS_KSTAT=y
CONFIG_MYSU_MYSUFS_OPEN_REDIRECT=y
CONFIG_MYSU_MYSUFS_SUS_MAP=y
CONFIG_MYSU_MYSUFS_SPOOF_UNAME=y
CONFIG_MYSU_MYSUFS_SPOOF_CMDLINE_OR_BOOTCONFIG=y
CONFIG_MYSU_MYSUFS_HIDE_ROOT_SYMBOLS=y

# Set to 'y' only when debugging VFS hooks via dmesg
# CONFIG_MYSU_MYSUFS_ENABLE_LOG is not set
```

---

## 2. Core Files Addition

Place the MYSUFS core headers and implementation into your kernel tree:

- `include/linux/mysufs_def.h`: Inode state bits, mount ID limits (`DEFAULT_MYSU_MNT_ID = 2000000000`), and inline helper macros.
- `include/linux/mysufs.h`: Data structures and forward function prototypes.
- `fs/mysufs.c`: Core engine containing hashtables, path traversal worker, stat cache, and open redirection lookup.

Add `mysufs.o` to `fs/Makefile`:
```diff
--- a/fs/Makefile
+++ b/fs/Makefile
@@ -14,6 +14,7 @@ obj-y :=	open.o read_write.o file_table.o super.o \
 		pnode_vfs.o statfs.o buffer.o file.o fs_types.o \
 		fs_context.o fs_parser.o fs-writeback.o
 
+obj-$(CONFIG_MYSU_MYSUFS) += mysufs.o
```

---

## 3. Kernel VFS Subsystem Interceptions

### A. Mount Namespace & Procfs (`fs/namespace.c` & `fs/proc_namespace.c`)

Mounts created by MySU or root processes are allocated synthetic IDs $\ge 2,000,000,000$. Unprivileged processes querying procfs have these entries filtered out.

**In `fs/namespace.c`**:
```diff
@@ -957,6 +957,11 @@ static struct mount *alloc_vfsmnt(const char *name)
 {
 	struct mount *mnt = kmem_cache_zalloc(mnt_cache, GFP_KERNEL);
 	if (mnt) {
+#ifdef CONFIG_MYSU_MYSUFS_SUS_MOUNT
+		if (current->mm && is_mysu_domain()) {
+			mnt->mnt_id = mnt_alloc_id(mnt, DEFAULT_MYSU_MNT_ID);
+		} else
+#endif
 		mnt->mnt_id = mnt_alloc_id(mnt, 0);
```

**In `fs/proc_namespace.c`**:
```diff
@@ -470,6 +470,12 @@ static int mounts_open(struct inode *inode, struct file *file)
 {
+#ifdef CONFIG_MYSU_MYSUFS_SUS_MOUNT
+	if (static_branch_likely(&mysufs_is_hide_sus_mnts_for_non_su_procs_enabled)) {
+		if (likely(!is_mysu_domain()))
+			return mounts_open_common(inode, file, mysufs_show_vfsmnt);
+	}
+#endif
 	return mounts_open_common(inode, file, show_vfsmnt);
 }
```
*(Apply identical checks to `mountinfo_open` and `mountstats_open`).*

---

### B. Path & Dcache Interception (`fs/namei.c`)

Blocks `lookup_fast()`, `lookup_dcache()`, and `__lookup_hash()` when an unprivileged app probes for cloaked paths:

```diff
@@ -1550,6 +1550,13 @@ static struct dentry *lookup_fast(struct nameidata *nd,
 	struct dentry *dentry;
 
+#ifdef CONFIG_MYSU_MYSUFS_SUS_PATH
+	if (d_backing_inode(parent) &&
+	    unlikely(test_bit(AS_FLAGS_SUS_PATH, &d_backing_inode(parent)->i_state)) &&
+	    mysufs_is_current_proc_umounted_app())
+		return ERR_PTR(-ENOENT);
+#endif
```

---

### C. Inode Metadata & Stat Spoofing (`fs/stat.c`)

Spoofs inode attributes during `vfs_statx()` and `vfs_statfs()`:

```diff
@@ -120,6 +120,12 @@ int vfs_statx(int dfd, const char __user *filename, int flags,
 	if (!error) {
 		error = vfs_getattr(&path, stat, request_mask, query_flags);
+#ifdef CONFIG_MYSU_MYSUFS_SUS_KSTAT
+		if (!error && mysufs_is_current_app_uid()) {
+			mysufs_sus_kstat_spoof(d_backing_inode(path.dentry), stat);
+		}
+#endif
 		path_put(&path);
 	}
```

---

### D. Memory Map Masking (`fs/proc/task_mmu.c` & `mm/memory.c`)

Hides sensitive memory allocations from unprivileged access:

```diff
@@ -350,6 +350,11 @@ static int show_map_vma(struct seq_file *m, struct vm_area_struct *vma)
 	struct file *file = vma->vm_file;
 
+#ifdef CONFIG_MYSU_MYSUFS_SUS_MAP
+	if (file && MYSUFS_IS_INODE_SUS_MAP(file_inode(file)))
+		return 0;
+#endif
```

---

### E. Inotify Watch Descriptors (`fs/notify/fdinfo.c`)

Masks active inotify watches monitoring cloaked directories:

::: warning Linux 4.19 vs 5.x+ API Difference
On Linux 5.x+, use `inotify_mark_user_mask(mark)`. On Linux 4.19 and older trees, use `(mark->mask & IN_ALL_EVENTS)`.
:::

```c
#ifdef CONFIG_MYSU_MYSUFS_SUS_KSTAT
if (mysufs_is_current_app_uid()) {
    bool is_fuse = false;
    if (mysufs_is_inode_sus_kstat(inode, &is_fuse)) {
        unsigned long ino = inode->i_ino;
        dev_t dev = inode->i_sb->s_dev;
        mysufs_sus_kstat_spoof_inotify_fdinfo(&ino, &dev);
        seq_printf(m, "inotify wd:%x ino:%lx sdev:%x mask:%x ignored_mask:0 ",
                   inode_mark->wd, ino, dev,
                   (mark->mask & IN_ALL_EVENTS));
        show_mark_fhandle(m, inode);
        seq_putc(m, '\n');
        iput(inode);
        return;
    }
}
#endif
```

---

### F. SELinux AVC Audit Log Spoofing (`security/selinux/avc.c`)

Rewrites target SID in denial logs to prevent attackers from discovering the `u:r:mysu:s0` domain:

```diff
@@ -200,6 +200,16 @@ static void avc_audit_post_callback(struct audit_buffer *ab, void *a)
 	rc = security_sid_to_context(state, tsid, &scontext, &scontext_len);
 
+#ifdef CONFIG_MYSU_MYSUFS
+	if (static_branch_likely(&mysufs_is_avc_log_spoofing_enabled)) {
+		if (unlikely(tsid == mysufs_ksu_sid)) {
+			if (rc)
+				audit_log_format(ab, " tsid=%d", mysufs_priv_app_sid);
+			else
+				audit_log_format(ab, " tcontext=%s", "u:r:priv_app:s0:c512,c768");
+			goto bypass_orig_flow;
+		}
+	}
+#endif
```

---

## 4. Driver Wiring (`drivers/mysu/`)

1. **Kernel Supercall Dispatch (`supercall/dispatch.c`)**:
   Include `<linux/mysufs.h>` and register `MYSU_IOCTL_MYSUFS_*` command entries in `mysu_ioctl_handlers[]` with `.perm_check = manager_or_root`.

2. **Core Driver Init (`core/init.c`)**:
   Invoke `mysufs_init()` during `mysu_init()`:
   ```c
   #ifdef CONFIG_MYSU_MYSUFS
   extern void mysufs_init(void);
   #endif
   
   // Inside mysu_init():
   #ifdef CONFIG_MYSU_MYSUFS
       mysufs_init();
   #endif
   ```

3. **Asynchronous Worker (`hook/setuid_hook.c`)**:
   Schedule `mysufs_extra_works` immediately after `mysu_handle_umount()` in `mysu_handle_setresuid()`:
   ```c
   #ifdef CONFIG_MYSU_MYSUFS
   extern struct work_struct mysufs_extra_works;
   #endif
   
   // Inside mysu_handle_setresuid():
   mysu_handle_umount(old_uid, new_uid);
   #ifdef CONFIG_MYSU_MYSUFS
   schedule_work(&mysufs_extra_works);
   #endif
   ```

---

## 5. Verification & Testing

After building the kernel and flashing `boot.img`:

```bash
# Verify kernel recognized the driver
adb shell su -c "mysud mysufs version"
# Expected output: MYSUFS Version: v2.3.0

# Verify active variant
adb shell su -c "mysud mysufs variant"
# Expected output: MYSUFS Variant: NON-GKI (or GKI)

# Test mount hiding
adb shell su -c "mysud mysufs hide-mnts true"

# Verify non-root process cannot find /data/adb
adb shell "ls -ld /data/adb"
# Expected output: ls: /data/adb: No such file or directory
```
