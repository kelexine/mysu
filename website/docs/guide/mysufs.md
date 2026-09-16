# MYSUFS (SU File System) Stealth Engine

**MYSUFS** is an advanced kernel-level stealth and anti-detection subsystem designed for MySU. It bridges deep kernel VFS interception hooks with the MySU driver control plane (`[mysu_driver]`) and the `mysud` userspace daemon to conceal root presence, mounts, modules, and diagnostic traces from unprivileged applications, integrity auditors, and security suites.

---

## Architecture Overview

Traditional root-hiding solutions rely heavily on userspace hooks (such as Zygisk modules, `ptrace`, or LD_PRELOAD shims), which can be detected through timing analysis, direct syscall invocation, procfs inspection, or memory scraping.

MYSUFS operates at **Ring 0 (Kernel Space)**, intercepting filesystem, memory, and security subsystems prior to data reaching userspace:

```
+---------------------------------------------------------------+
|                Userspace Apps / Detection Engines             |
|   (Banking apps, Play Integrity probes, RootBeer, Detectors)  |
+-------------------------------+-------------------------------+
                                │ Read / Stat / Open / Readdir
                                ▼
+---------------------------------------------------------------+
|                    VFS Interception Layer                     |
|  - fs/namespace.c:      Isolates mount IDs (>= 2,000,000,000) |
|  - fs/proc_namespace.c: Redacts /proc/[pid]/mounts, mountinfo |
|  - fs/namei.c:          Cloaks dcache lookups & fast path     |
|  - fs/stat.c & statfs:  Spoofs inode, dev_t, nlink, size      |
|  - fs/readdir.c:        Filters directory entries in-place    |
|  - fs/proc/task_mmu.c:  Conceals VMA mappings in /proc/maps    |
|  - fs/notify/fdinfo.c:  Masks inotify watch descriptors       |
|  - kernel/kallsyms.c:   Filters mysu_*, mysufs_*, ksu_* syms |
|  - security/selinux/avc.c: Spoofs target SIDs in audit denials|
+-------------------------------+-------------------------------+
                                ▲
                                │ Configuration / IOCTLs
+-------------------------------+-------------------------------+
|                MySU Driver Control Plane                      |
|  - Anonymous inode ioctl (`MYSU_IOCTL_MYSUFS_*`)              |
|  - Governed by root and manager privilege checks              |
+-------------------------------+-------------------------------+
                                ▲
                                │ Commands
+-------------------------------+-------------------------------+
|                mysud Daemon & CLI Subcommands                 |
|  - Configures rules on `post-fs-data` boot stage              |
|  - CLI interface: `mysud mysufs <subcommand>`                 |
+---------------------------------------------------------------+
```

---

## Core Capabilities

### 1. Mount Point Isolation (`sus_mount`)
- **Mount ID Partitioning**: Any mount created by MySU or userspace daemons with elevated permissions is assigned a synthetic `mnt_id` ($\ge 2,000,000,000$).
- **Procfs Scrubbing**: When non-root processes query `/proc/mounts`, `/proc/net/mounts`, `/proc/self/mountinfo`, or `/proc/self/mountstats`, the kernel automatically skips mounts whose ID satisfies the isolation threshold.
- **Unprivileged Immunity**: Prevents detection apps from discovering systemless overlays, Magisk/MySU tmpfs mirrors, or active module bind mounts.

### 2. Deep Path Cloaking (`sus_path`)
- **Direct VFS Interception**: Intercepts `lookup_dcache()`, `lookup_fast()`, and `__lookup_hash()`.
- If an unprivileged process attempts to query a cloaked path (e.g., `/data/adb`, `/system/bin/su`), the kernel returns `-ENOENT` directly from the VFS layer, bypassing userspace entirely.
- **Path Loops**: Periodically traverses subdirectories of monitored roots to mark inode address space flags (`AS_FLAGS_SUS_PATH`).

### 3. Inode & Stat Spoofing (`sus_kstat`)
- Bypasses raw `stat()`, `lstat()`, `fstatat()`, and `statx()` probes.
- Inodes corresponding to cloaked or redirected files can report fake `st_ino`, `st_dev`, `st_size`, `st_nlink`, and timestamps matching stock OEM files.

### 4. Memory Mapping Masking (`sus_map`)
- Probes often scan `/proc/self/maps` or `/proc/self/smaps` for injected libraries (e.g. `libmysu.so`, `libzygisk.so`).
- Intercepts `show_map_vma()`, `show_smap()`, `show_smaps_rollup()`, and `pagemap_read()` to hide designated memory segments.

### 5. Open Redirection (`open_redirect`)
- Transparently routes unprivileged open attempts (`do_sys_openat`) from a sensitive path to a benign target (e.g., redirecting access from a custom binary to stock `/system/bin/sh`).
- Supports granular UID scheme filtering (`UID_NON_APP_PROC`, `UID_ROOT_PROC_EXCEPT_SU_PROC`, `UID_NON_SU_PROC`, `UID_UMOUNTED_APP_PROC`).

### 6. SELinux AVC Audit Log Spoofing
- When an untrusted app probes for MySU contexts, any resulting SELinux audit denial log is intercepted.
- Replaces target SID references (`tsid`) to emulate benign contexts like `u:r:priv_app:s0:c512,c768` instead of revealing `u:r:mysu:s0`.

### 7. Kernel Symbol Redaction (`kallsyms`)
- Redacts symbols matching `mysu_*`, `__mysu_*`, `mysufs_*`, `ksu_*`, and related markers from `/proc/kallsyms` for non-root users.

### 8. System Environment Spoofing
- **Uname**: Dynamically spoofs `sys_uname` output (`release` and `version`).
- **Cmdline**: Cloaks `/proc/cmdline` to remove telltale boot flags like `androidboot.selinux=permissive`.

---

## CLI Reference (`mysud mysufs`)

The `mysud` userspace daemon provides direct commands to inspect and configure MYSUFS at runtime:

### Diagnostics & Information
```bash
# Query active engine version
mysud mysufs version
# Output: MYSUFS Version: v2.3.0

# Query engine variant (e.g. NON-GKI or GKI)
mysud mysufs variant
# Output: MYSUFS Variant: NON-GKI

# List active kernel feature flags
mysud mysufs features
```

### Path & Mount Cloaking
```bash
# Enable mount hiding for unprivileged apps
mysud mysufs hide-mnts true

# Add a directory or file to the stealth cloaking list
mysud mysufs add-sus-path /data/adb

# Add with recursive loop tracking
mysud mysufs add-sus-path /data/adb/modules --loop-mode

# Cloak VMA memory mappings
mysud mysufs add-sus-map /data/adb/modules/test/libspoof.so
```

### Path Redirection & Stat Spoofing
```bash
# Add open redirect rule: <target> <redirected> [uid_scheme]
mysud mysufs add-open-redirect /system/bin/su /system/bin/sh 2

# Add custom inode kstat spoofing:
# mysud mysufs add-sus-kstat <path> <target_ino> <spoofed_ino> <spoofed_dev> <spoofed_size> [flags]
mysud mysufs add-sus-kstat /system/bin/su 1000 2000 64770 12560 0
```

### Audit & System Cloaking
```bash
# Enable SELinux AVC audit denial spoofing
mysud mysufs enable-avc-log-spoofing true

# Spoof kernel uname release and version
mysud mysufs set-uname "4.19.127-stock" "#1 SMP PREEMPT"

# Spoof /proc/cmdline
mysud mysufs set-cmdline "bootopt=64S3,32N2,64N2 androidboot.selinux=enforcing"

# Toggle MYSUFS kernel debug logging
mysud mysufs log false
```

---

## Kernel Configuration Reference

When compiling a kernel with MYSUFS support, the following `Kconfig` options are available:

| Config Option | Description | Recommended |
|---|---|---|
| `CONFIG_MYSU_MYSUFS` | Master toggle for the MYSUFS subsystem | `y` |
| `CONFIG_MYSU_MYSUFS_SUS_PATH` | Enables deep VFS path lookup interception | `y` |
| `CONFIG_MYSU_MYSUFS_SUS_MOUNT` | Isolates and hides mounts with IDs $\ge 2,000,000,000$ | `y` |
| `CONFIG_MYSU_MYSUFS_SUS_KSTAT` | Inode statx and statfs spoofing engine | `y` |
| `CONFIG_MYSU_MYSUFS_OPEN_REDIRECT` | Transparent path redirection on `openat()` | `y` |
| `CONFIG_MYSU_MYSUFS_SUS_MAP` | Cloaks memory maps in `/proc/[pid]/maps` | `y` |
| `CONFIG_MYSU_MYSUFS_SPOOF_UNAME` | Uname release and version spoofing | `y` |
| `CONFIG_MYSU_MYSUFS_SPOOF_CMDLINE_OR_BOOTCONFIG` | `/proc/cmdline` spoofing | `y` |
| `CONFIG_MYSU_MYSUFS_HIDE_ROOT_SYMBOLS` | Redacts symbols from `/proc/kallsyms` | `y` |
| `CONFIG_MYSU_MYSUFS_ENABLE_LOG` | Verbose dmesg kernel logging (debug builds only) | `n` |

---

## Boot Sequence Integration

MYSUFS rules are initialized automatically by `mysud` during the `post-fs-data` boot event:
1. Mount point isolation is engaged (`mysufs_set_hide_sus_mnts(true)`).
2. Standard sensitive paths (`/data/adb`, `/data/adb/modules`, `/data/adb/mysu`, `/data/adb/magisk`, `/system/bin/su`) are registered.
3. SELinux AVC audit denial spoofing is activated.
4. Custom user rules located in `/data/adb/mysu/mysufs.rc` or post-fs-data scripts are executed.
