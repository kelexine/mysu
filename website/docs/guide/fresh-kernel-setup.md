# Fresh Kernel Tree Integration

This guide walks you through integrating **MySU** and the **MYSUFS Stealth Engine** into a completely clean, pristine Android kernel source tree with zero prior modifications.

---

## Overview

Unlike standard userspace root managers or generic GKI-only frameworks, MySU can be embedded directly into vendor kernel trees (such as Linux 4.14, 4.19, and 5.4) using our automated setup utility.

When integrating into a fresh kernel tree, you can choose between two deployment profiles:
1. **Standard MySU**: Core root access, sucompat, kprobe/tracepoint hooks, and app sandboxing.
2. **MySU + MYSUFS Stealth Engine**: Core root access combined with deep VFS path cloaking, isolated mount namespace scrubbing (`mnt_id >= 2,000,000,000`), inode stat spoofing, open redirection, hidden VMA maps, and SELinux audit log cloaking.

---

## 1-Liner Automated Setup

Navigate to your kernel tree root directory and execute the setup command matching your desired profile.

::: code-group

```sh [MySU + MYSUFS Stealth (Recommended)]
curl -LSs "https://raw.githubusercontent.com/kelexine/mysu/main/kernel/setup.sh" | bash -s -- --mysufs
```

```sh [Standard MySU]
curl -LSs "https://raw.githubusercontent.com/kelexine/mysu/main/kernel/setup.sh" | bash -
```

:::

### What `setup.sh` Automates

When you run the command above in a fresh kernel repository, `setup.sh` performs the following operations automatically:

1. **Tree Layout Detection**: Identifies whether your kernel tree uses `drivers/` (standard monolithic kernel) or `common/drivers/` (GKI / split-tree repos).
2. **Repository Cloning**: Clones the latest release of `MySU` directly into `$KERNEL_ROOT/MySU`.
3. **Driver Symlinking**: Creates a relative symlink from `drivers/mysu` pointing to `MySU/kernel`.
4. **Build System Wiring**:
   - Injects `obj-$(CONFIG_MYSU) += mysu/` into `drivers/Makefile`.
   - Injects `source "drivers/mysu/Kconfig"` into `drivers/Kconfig` right before the closing `endmenu`.
5. **MYSUFS Stealth Patch Application (if `--mysufs` is passed)**:
   - Locates the standalone `MySU/kernel/patches/mysufs-kernel-4.19.patch`.
   - Performs a dry-run check using `git apply --check` or `patch -p1 -N --dry-run`.
   - Patches `fs/`, `security/selinux/`, `kernel/`, and `mm/` cleanly to enable low-level VFS cloaking.

---

## Configure Target Defconfig

After running the setup script, open your device's defconfig (typically located in `arch/arm64/configs/<device>_defconfig` or `arch/arm64/configs/vendor/<device>_defconfig`).

### Baseline MySU Configuration

Append the following core flags:

```kconfig
# MySU Core Root Subsystem
CONFIG_MYSU=y
CONFIG_KPROBES=y
CONFIG_HAVE_KPROBES=y
CONFIG_KPROBE_EVENTS=y
```

::: tip KPROBES REQUIRE MODULES
If your build system fails with unknown symbol errors or cannot select `CONFIG_KPROBES`, ensure that `CONFIG_MODULES=y` is present in your defconfig.
:::

### MYSUFS Stealth Subsystem Configuration

If you ran the script with `--mysufs`, append the MYSUFS feature flags to your defconfig:

```kconfig
# MYSUFS Kernel Stealth & VFS Cloaking
CONFIG_MYSU_MYSUFS=y
CONFIG_MYSU_MYSUFS_SUS_PATH=y
CONFIG_MYSU_MYSUFS_SUS_MOUNT=y
CONFIG_MYSU_MYSUFS_SUS_KSTAT=y
CONFIG_MYSU_MYSUFS_OPEN_REDIRECT=y
CONFIG_MYSU_MYSUFS_SUS_MAP=y
CONFIG_MYSU_MYSUFS_SPOOF_UNAME=y
CONFIG_MYSU_MYSUFS_SPOOF_CMDLINE_OR_BOOTCONFIG=y
CONFIG_MYSU_MYSUFS_HIDE_ROOT_SYMBOLS=y
```

---

## Reversible Operations & Cleanup

If you ever wish to revert all modifications made by `setup.sh` and return your kernel tree to its original state, run:

```sh
./MySU/kernel/setup.sh --cleanup
```

This will:
- Remove the `drivers/mysu` symlink.
- Remove the injected lines from `drivers/Makefile` and `drivers/Kconfig`.
- Delete the `MySU/` checkout directory.

*(Note: If you applied MYSUFS core kernel patches with `--mysufs`, use `git checkout fs/ security/ kernel/ mm/` to discard kernel source modifications).*

---

## Manual Patching & Non-4.19 Kernels

If your kernel tree is based on a different LTS version (such as 4.14 or 5.4), or if your vendor tree has conflicting OEM modifications in `fs/namespace.c` or `fs/namei.c`, `setup.sh` will notify you that manual review is required.

In this scenario:
1. Inspect the patch file at `MySU/kernel/patches/mysufs-kernel-4.19.patch`.
2. Review the line-by-line manual diff guide in the [MYSUFS Kernel Integration Guide](mysufs-integration.md).
3. Verify that your kernel's `fs/notify/fdinfo.c` uses `(mark->mask & IN_ALL_EVENTS)` on pre-5.10 kernels instead of `inotify_mark_user_mask(mark)`.

---

## Build Verification

Compile your kernel image using your standard toolchain:

```sh
make O=out ARCH=arm64 <device>_defconfig
make O=out ARCH=arm64 -j$(nproc)
```

Once booted:
1. Install the MySU manager APK or connect via ADB root.
2. Check the kernel log (`dmesg | grep -i mysu`):
   ```
   [    1.234567] mysu: core initialized
   [    1.234890] mysufs: initialized
   ```
3. Query MYSUFS status via CLI:
   ```sh
   mysud mysufs version
   mysud mysufs features
   ```

<!-- generated: antigravity-cli | gemini-3.5-flash | 2026-09-16 -->
