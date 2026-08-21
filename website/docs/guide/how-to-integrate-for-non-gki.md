# Integrate for non-GKI devices

MySU can be integrated into non-GKI custom kernels and has been validated across 4.14.x, 4.19.x, and 5.4.x trees (such as `TheVoid-Kernel`).

Due to the device-specific nature of non-GKI Android kernels, universal prebuilt boot images cannot be provided. However, integrating MySU into your device's kernel source tree is straightforward.

---

## Integration Methods

Depending on your kernel configuration and architecture, choose one of two methods:
1. **Automatic Integration via Kprobes / Tracepoints** (Recommended)
2. **Manual Kernel Source Patching** (For legacy trees with broken or disabled kprobes)

---

## Method 1: Integrate with Kprobes / Tracepoints

If your kernel supports Kprobes and Syscall Tracepoints, MySU automatically hooks system calls at runtime.

### 1. Add MySU to Kernel Source
In your kernel source root directory:

```sh
curl -LSs "https://raw.githubusercontent.com/kelexine/mysu/main/kernel/setup.sh" | bash -
```

### 2. Enable Kernel Configuration Flags
Ensure the following options are enabled in your device's `defconfig`:

```txt
CONFIG_MYSU=y
CONFIG_KPROBES=y
CONFIG_HAVE_KPROBES=y
CONFIG_KPROBE_EVENTS=y
```

If `CONFIG_KPROBES` cannot be selected directly, ensure `CONFIG_MODULES=y` is set. Rebuild your kernel image, and MySU will initialize automatically upon boot.

::: tip HOW TO CHECK IF KPROBE IS BROKEN？
Comment out `mysu_sucompat_init()` and `mysu_mysud_init()` in `MySU/kernel/mysu.c`. If the device boots normally, kprobe may be broken.
:::

::: info HOW TO GET MODULE UMOUNT FEATURE WORKING ON PRE-GKI?
If your kernel is older than 5.9, you should backport `path_umount` to `fs/namespace.c`. This is required to get "Umount module" feature work correctly. If you don't backport `path_umount`, "Umount module" feature won't work. You can get more info on how to achieve this at the end of this page.
:::

## Manually modify the kernel source

If kprobe doesn't work on your kernel—either because of an upstream bug or because your kernel is older than 4.8—you can try the following approach:

First, add MySU to your kernel source tree:

```sh
curl -LSs "https://raw.githubusercontent.com/kelexine/mysu/main/kernel/setup.sh" | bash -
```

Keep in mind that, on some devices, your defconfig may be located at `arch/arm64/configs` or in other cases, it may be at `arch/arm64/configs/vendor/your_defconfig`. Regardless of the defconfig you're using, make sure to enable `CONFIG_MYSU` with `y` to enable or `n` to disable it. For example, if you choose to enable it, your defconfig should contain the following string:

```txt
# MySU
CONFIG_MYSU=y
```

Next, add MySU calls to the kernel source. Below are some patches for reference:

::: code-group

```diff[exec.c]
diff --git a/fs/exec.c b/fs/exec.c
index ac59664eaecf..bdd585e1d2cc 100644
--- a/fs/exec.c
+++ b/fs/exec.c
@@ -1890,11 +1890,14 @@ static int __do_execve_file(int fd, struct filename *filename,
 	return retval;
 }

+#ifdef CONFIG_MYSU
+extern bool mysu_execveat_hook __read_mostly;
+extern int mysu_handle_execveat(int *fd, struct filename **filename_ptr, void *argv,
+			void *envp, int *flags);
+extern int mysu_handle_execveat_sucompat(int *fd, struct filename **filename_ptr,
+				 void *argv, void *envp, int *flags);
+#endif
 static int do_execveat_common(int fd, struct filename *filename,
 			      struct user_arg_ptr argv,
 			      struct user_arg_ptr envp,
 			      int flags)
 {
+   #ifdef CONFIG_MYSU
+	if (unlikely(mysu_execveat_hook))
+		mysu_handle_execveat(&fd, &filename, &argv, &envp, &flags);
+	else
+		mysu_handle_execveat_sucompat(&fd, &filename, &argv, &envp, &flags);
+   #endif
 	return __do_execve_file(fd, filename, argv, envp, flags, NULL);
 }
```
```diff[open.c]
diff --git a/fs/open.c b/fs/open.c
index 05036d819197..965b84d486b8 100644
--- a/fs/open.c
+++ b/fs/open.c
@@ -348,6 +348,8 @@ SYSCALL_DEFINE4(fallocate, int, fd, int, mode, loff_t, offset, loff_t, len)
 	return ksys_fallocate(fd, mode, offset, len);
 }

+#ifdef CONFIG_MYSU
+extern int mysu_handle_faccessat(int *dfd, const char __user **filename_user, int *mode,
+			 int *flags);
+#endif
 /*
  * access() needs to use the real uid/gid, not the effective uid/gid.
  * We do this by temporarily clearing all FS-related capabilities and
@@ -355,6 +357,7 @@ SYSCALL_DEFINE4(fallocate, int, fd, int, mode, loff_t, offset, loff_t, len)
  */
 long do_faccessat(int dfd, const char __user *filename, int mode)
 {
 	const struct cred *old_cred;
 	struct cred *override_cred;
 	struct path path;
 	struct inode *inode;
 	struct vfsmount *mnt;
 	int res;
 	unsigned int lookup_flags = LOOKUP_FOLLOW;
+   #ifdef CONFIG_MYSU
+	mysu_handle_faccessat(&dfd, &filename, &mode, NULL);
+   #endif
 
 	if (mode & ~S_IRWXO)	/* where's F_OK, X_OK, W_OK, R_OK? */
 		return -EINVAL;
```
```diff[read_write.c]
diff --git a/fs/read_write.c b/fs/read_write.c
index 650fc7e0f3a6..55be193913b6 100644
--- a/fs/read_write.c
+++ b/fs/read_write.c
@@ -434,10 +434,14 @@ ssize_t kernel_read(struct file *file, void *buf, size_t count, loff_t *pos)
 }
 EXPORT_SYMBOL(kernel_read);

+#ifdef CONFIG_MYSU
+extern bool mysu_vfs_read_hook __read_mostly;
+extern int mysu_handle_vfs_read(struct file **file_ptr, char __user **buf_ptr,
+			size_t *count_ptr, loff_t **pos);
+#endif
 ssize_t vfs_read(struct file *file, char __user *buf, size_t count, loff_t *pos)
 {
 	ssize_t ret;
+   #ifdef CONFIG_MYSU 
+	if (unlikely(mysu_vfs_read_hook))
+		mysu_handle_vfs_read(&file, &buf, &count, &pos);
+   #endif
+
 	if (!(file->f_mode & FMODE_READ))
 		return -EBADF;
 	if (!(file->f_mode & FMODE_CAN_READ))
```
```diff[stat.c]
diff --git a/fs/stat.c b/fs/stat.c
index 376543199b5a..82adcef03ecc 100644
--- a/fs/stat.c
+++ b/fs/stat.c
@@ -148,6 +148,8 @@ int vfs_statx_fd(unsigned int fd, struct kstat *stat,
 }
 EXPORT_SYMBOL(vfs_statx_fd);

+#ifdef CONFIG_MYSU
+extern int mysu_handle_stat(int *dfd, const char __user **filename_user, int *flags);
+#endif
+
 /**
  * vfs_statx - Get basic and extra attributes by filename
  * @dfd: A file descriptor representing the base dir for a relative filename
@@ -170,6 +172,7 @@ int vfs_statx(int dfd, const char __user *filename, int flags,
 	int error = -EINVAL;
 	unsigned int lookup_flags = LOOKUP_FOLLOW | LOOKUP_AUTOMOUNT;

+   #ifdef CONFIG_MYSU
+	mysu_handle_stat(&dfd, &filename, &flags);
+   #endif
 	if ((flags & ~(AT_SYMLINK_NOFOLLOW | AT_NO_AUTOMOUNT |
 		       AT_EMPTY_PATH | KSTAT_QUERY_FLAGS)) != 0)
 		return -EINVAL;
```

:::

You should find the four functions in kernel source:

1. `do_faccessat`, usually in `fs/open.c`
2. `do_execveat_common`, usually in `fs/exec.c`
3. `vfs_read`, usually in `fs/read_write.c`
4. `vfs_statx`, usually in `fs/stat.c`

If your kernel doesn't have the `vfs_statx` function, use `vfs_fstatat` instead:

```diff
diff --git a/fs/stat.c b/fs/stat.c
index 068fdbcc9e26..5348b7bb9db2 100644
--- a/fs/stat.c
+++ b/fs/stat.c
@@ -87,6 +87,8 @@ int vfs_fstat(unsigned int fd, struct kstat *stat)
 }
 EXPORT_SYMBOL(vfs_fstat);

+#ifdef CONFIG_MYSU
+extern int mysu_handle_stat(int *dfd, const char __user **filename_user, int *flags);
+#endif
 int vfs_fstatat(int dfd, const char __user *filename, struct kstat *stat,
 		int flag)
 {
@@ -94,6 +96,8 @@ int vfs_fstatat(int dfd, const char __user *filename, struct kstat *stat,
 	int error = -EINVAL;
 	unsigned int lookup_flags = 0;
+   #ifdef CONFIG_MYSU 
+	mysu_handle_stat(&dfd, &filename, &flag);
+   #endif
+
 	if ((flag & ~(AT_SYMLINK_NOFOLLOW | AT_NO_AUTOMOUNT |
 		      AT_EMPTY_PATH)) != 0)
 		goto out;
```

For kernels eariler than 4.17, if you cannot find `do_faccessat`, just go to the definition of the `faccessat` syscall and place the call there:

```diff
diff --git a/fs/open.c b/fs/open.c
index 2ff887661237..e758d7db7663 100644
--- a/fs/open.c
+++ b/fs/open.c
@@ -355,6 +355,9 @@ SYSCALL_DEFINE4(fallocate, int, fd, int, mode, loff_t, offset, loff_t, len)
 	return error;
 }

+#ifdef CONFIG_MYSU
+extern int mysu_handle_faccessat(int *dfd, const char __user **filename_user, int *mode,
+			        int *flags);
+#endif
+
 /*
  * access() needs to use the real uid/gid, not the effective uid/gid.
  * We do this by temporarily clearing all FS-related capabilities and
@@ -370,6 +373,8 @@ SYSCALL_DEFINE3(faccessat, int, dfd, const char __user *, filename, int, mode)
 	int res;
 	unsigned int lookup_flags = LOOKUP_FOLLOW;
+   #ifdef CONFIG_MYSU
+	mysu_handle_faccessat(&dfd, &filename, &mode, NULL);
+   #endif
+
 	if (mode & ~S_IRWXO)	/* where's F_OK, X_OK, W_OK, R_OK? */
 		return -EINVAL;
```

### Safe Mode

To enable MySU's built-in Safe Mode, you should modify the `input_handle_event` function in `drivers/input/input.c`:

::: tip
It's strongly recommended to enable this feature, it's very useful for preventing bootloops!
:::

```diff
diff --git a/drivers/input/input.c b/drivers/input/input.c
index 45306f9ef247..815091ebfca4 100755
--- a/drivers/input/input.c
+++ b/drivers/input/input.c
@@ -367,10 +367,13 @@ static int input_get_disposition(struct input_dev *dev,
 	return disposition;
 }

+#ifdef CONFIG_MYSU
+extern bool mysu_input_hook __read_mostly;
+extern int mysu_handle_input_handle_event(unsigned int *type, unsigned int *code, int *value);
+#endif
+
 static void input_handle_event(struct input_dev *dev,
 			       unsigned int type, unsigned int code, int value)
 {
	int disposition = input_get_disposition(dev, type, code, &value);
+   #ifdef CONFIG_MYSU
+	if (unlikely(mysu_input_hook))
+		mysu_handle_input_handle_event(&type, &code, &value);
+   #endif
 
 	if (disposition != INPUT_IGNORE_EVENT && type != EV_SYN)
 		add_input_randomness(type, code, value);
```

::: info ENTERING SAFE MODE ACCIDENTALLY?
If you're using manual integration and don't disable `CONFIG_KPROBES`, the user will be able to trigger Safe Mode by pressing the volume down button after booting! Therefore, if you're using manual integration, it's necessary to disable `CONFIG_KPROBES`!
:::

### Failed to execute `pm` in terminal?

You should modify `fs/devpts/inode.c`. Reference:

```diff
diff --git a/fs/devpts/inode.c b/fs/devpts/inode.c
index 32f6f1c68..d69d8eca2 100644
--- a/fs/devpts/inode.c
+++ b/fs/devpts/inode.c
@@ -602,6 +602,8 @@ struct dentry *devpts_pty_new(struct pts_fs_info *fsi, int index, void *priv)
        return dentry;
 }

+#ifdef CONFIG_MYSU
+extern int mysu_handle_devpts(struct inode*);
+#endif
+
 /**
  * devpts_get_priv -- get private data for a slave
  * @pts_inode: inode of the slave
@@ -610,6 +612,7 @@ struct dentry *devpts_pty_new(struct pts_fs_info *fsi, int index, void *priv)
  */
 void *devpts_get_priv(struct dentry *dentry)
 {
+       #ifdef CONFIG_MYSU
+       mysu_handle_devpts(dentry->d_inode);
+       #endif
        if (dentry->d_sb->s_magic != DEVPTS_SUPER_MAGIC)
                return NULL;
        return dentry->d_fsdata;
```

### How to backport path_umount

You can make the "Umount modules" feature work on pre-GKI kernels by manually backporting `path_umount` from 5.9. You can use this patch as reference:

```diff
--- a/fs/namespace.c
+++ b/fs/namespace.c
@@ -1739,6 +1739,39 @@ static inline bool may_mandlock(void)
 }
 #endif

+static int can_umount(const struct path *path, int flags)
+{
+	struct mount *mnt = real_mount(path->mnt);
+
+	if (flags & ~(MNT_FORCE | MNT_DETACH | MNT_EXPIRE | UMOUNT_NOFOLLOW))
+		return -EINVAL;
+	if (!may_mount())
+		return -EPERM;
+	if (path->dentry != path->mnt->mnt_root)
+		return -EINVAL;
+	if (!check_mnt(mnt))
+		return -EINVAL;
+	if (mnt->mnt.mnt_flags & MNT_LOCKED) /* Check optimistically */
+		return -EINVAL;
+	if (flags & MNT_FORCE && !capable(CAP_SYS_ADMIN))
+		return -EPERM;
+	return 0;
+}
+
+int path_umount(struct path *path, int flags)
+{
+	struct mount *mnt = real_mount(path->mnt);
+	int ret;
+
+	ret = can_umount(path, flags);
+	if (!ret)
+		ret = do_umount(mnt, flags);
+
+	/* we mustn't call path_put() as that would clear mnt_expiry_mark */
+	dput(path->dentry);
+	mntput_no_expire(mnt);
+	return ret;
+}
 /*
  * Now umount can handle mount points as well as block devices.
  * This is important for filesystems which use unnamed block devices.
```

Finally, build your kernel again, and MySU should work correctly.
