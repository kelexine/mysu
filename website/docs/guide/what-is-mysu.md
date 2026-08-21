# What is MySU?

::: tip Project Context & Upstream Attribution
**MySU** is a heavily refactored, customized, and rebranded fork of the upstream **[KernelSU](https://github.com/tiann/KernelSU)** project, maintained by [kelexine](https://github.com/kelexine) primarily for personal use across custom Linux kernels (`TheVoid-Kernel`, 4.19.x legacy, and modern GKI 2.0+).

All core architecture, kernel-level hooking paradigms, and root mechanics originate from the foundational work created by **weishu (tiann)** and the **KernelSU Contributors**.
:::

**MySU** is a next-generation kernel-assisted root solution and systemless modification framework for Android devices. By operating directly at **Ring 0 (Kernel Space)**, MySU fundamentally redesigns how superuser privileges, SELinux policies, and systemless overlays are managed on Android.

---

## Why Kernel-Assisted Root?

Traditional root implementations operate primarily in user space by replacing the `/system/bin/app_process` binary or hooking `Zygote`. This creates several inherent detection vectors:
- **Filesystem Artifacts**: Su binaries located in `/system/bin` or `/system/xbin`.
- **Mount Namespace Leakage**: Systemless overlays and module directories exposed globally across all process namespaces.
- **SELinux Permissive Degradation**: Requiring SELinux to be globally set to permissive or leaving observable audit logs.

MySU resolves these structural flaws by executing inside the Linux kernel itself:

```
                      +---------------------------------------+
                      |         MySU Manager (Android)        |
                      |          dev.kelexine.mysu            |
                      |  (Compose M3 / JNI / App Zygote)      |
                      +-------------------+-------------------+
                                          |
                                          | JNI / Direct IOCTL
                                          v
+-----------------------+     +-------------------------------+
|     Applications      |     |         mysud Daemon          |
|  (/system/bin/su, CLI)| <-> |  (Rust 2024 / /data/adb/mysud)|
+-----------+-----------+     +---------------+---------------+
            |                                 |
            | Syscall / sucompat              | Reboot Magic / Task Work
            v                                 v
+-------------------------------------------------------------+
|                      Linux Kernel Space                     |
|                                                             |
|  +------------------------+     +------------------------+  |
|  |     Hook Subsystem     |     |   Supercall / Driver   |  |
|  | (Syscall / LSM / Trace)|     |   ([mysu_driver] ioctl)|  |
|  +-----------+------------+     +-----------+------------+  |
|              |                              |               |
|  +-----------v------------+     +-----------v------------+  |
|  |  SELinux Live-Patcher  |     |   Throne & Allowlist   |  |
|  |    (avtab / rules)     |     | (APK v2 Sig / Profiles)|  |
|  +------------------------+     +------------------------+  |
+-------------------------------------------------------------+
```

---

## Core Architectural Pillars

### 1. Anonymous Inode Supercalls (`[mysu_driver]`)
Rather than maintaining world-readable `/dev` character devices or accessible sockets, MySU establishes user-to-kernel communication dynamically. A process initiates a reboot syscall with magic register constants (`0xDEADBEEF`, `0xCAFEBABE`). The kernel schedules a `task_work` callback to install an anonymous inode file descriptor (`[mysu_driver]`) directly into the calling process's file table.

### 2. Dual-Engine Kernel Hooking
MySU dynamically intercepts critical syscalls (`setresuid`, `execve`, `execveat`, `newfstatat`, `faccessat`) via:
- **Tracepoints & Syscall Dispatcher**: High-priority `sys_enter` tracepoint interception (`INT_MIN`) redirecting target syscalls to `mysu_dispatcher_nr`.
- **LSM Hooking**: Direct memory patching of `security_hook_heads` (pre-Linux 6.12) or the static calls table (`lsm_static_call` on 6.12+).
- **Kprobes Fallback**: Automatic kprobe fallback for non-GKI trees lacking custom tracepoint hooks.

### 3. In-Memory SELinux Live-Patching
MySU modifies the kernel's live access vector table (`avtab`) and policy database (`policydb`) in RAM. It grants root processes and daemons permissive domains (`u:r:mysu:s0`, `u:r:mysud:s0`) while keeping the system in strict **Enforcing** mode.

### 4. Per-App Granular Sandboxing & `kernel_umount`
Only explicitly authorized applications can see `su` or invoke superuser privileges. For any unauthorized app, MySU's `kernel_umount` subsystem automatically strips all module mount points and `/data/adb` paths from the application's mount namespace upon process creation.

### 5. Pure Rust Userspace (`mysud` & `mysuinit`)
The userspace runtime is written entirely in modern Rust (Edition 2024). It operates at `/data/adb/mysud` without symlink clutter, providing built-in boot image patching, magic mounting, module management, and CLI tooling.

---

## Getting Started

- **[Installation Guide](installation.md)**: How to install via GKI, LKM, or custom kernel compilation.
- **[Difference with Magisk](difference-with-magisk.md)**: Deep dive into architectural differences and migration tips.
- **[How to Build](how-to-build.md)**: Compiling the kernel driver, userspace daemons, and Manager APK.
- **[Module Development](module.md)**: Creating systemless modules, metamodules, and WebUIs.

---

## Acknowledgments & Credits

- **[KernelSU](https://github.com/tiann/KernelSU)**: Upstream project created by **weishu (tiann)** and contributors.
- **[Kernel-Assisted Superuser (zx2c4)](https://git.zx2c4.com/kernel-assisted-superuser/about/)**: Foundational concept.
- **[Magisk (topjohnwu)](https://github.com/topjohnwu/Magisk)**: Pioneered modern systemless Android root.
- **[genuine (brevent)](https://github.com/brevent/genuine)**: APK v2 signature verification techniques.
- **[Diamorphine (m0nad)](https://github.com/m0nad/Diamorphine)**: LKM and hooking inspiration.
