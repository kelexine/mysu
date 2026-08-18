# What is MySU?

::: tip Project Context & Upstream Attribution
**MySU** is a personal, heavily refactored, and rebranded fork of the upstream **[KernelSU](https://github.com/tiann/KernelSU)** project, maintained by [kelexine](https://github.com/kelexine) primarily for personal use on custom Linux kernels (`TheVoid-Kernel`, 4.19.x, 5.10+, 6.x).

All core architecture, kernel-level hooking paradigms, and root mechanics are based on the pioneering foundation created by **weishu (tiann)** and the **KernelSU Contributors**.
:::

MySU is a kernel-based root and privilege management framework for Android. Operating at ring 0 inside the Linux kernel, it grants and isolates superuser capabilities to userspace applications directly within kernel space.

## Key Features

1. **Kernel-Native Elevation**: Operating in kernel mode allows MySU to provide kernel-level security boundaries, anonymous inode ioctl messaging (`[mysu_driver]`), and tamper-proof permission verification.
2. **Pure Userspace Daemon (`mysud`)**: A modern userspace daemon managing `/data/adb/mysu/` with programmatic module compatibility and zero filesystem symlink clutter.
3. **Per-App Sandboxing & App Profiles**: Fine-grained root delegation where only explicitly permitted apps perceive `su`. All other processes observe an unmodified stock Android system.
4. **Metamodule Architecture**: Pluggable overlayfs and mount virtualization infrastructure for clean, systemless partition modification.
5. **TheVoid Adaptive Material 3**: Sleek management dashboard featuring deep dark cosmic aesthetics and adaptive Android theming.

## How to use MySU?

See [Installation](installation.md).

## How to build MySU?

See [How to build](how-to-build.md).

## Upstream & Credits

- **[KernelSU](https://github.com/tiann/KernelSU)**: Upstream project created by weishu and contributors.
- **[Kernel-Assisted Superuser](https://git.zx2c4.com/kernel-assisted-superuser/about/)**: Foundational concept.
- **[Magisk](https://github.com/topjohnwu/Magisk)**: Systemless Android modification pioneer.
