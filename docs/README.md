<div align="center">
  <img src="logo.png" style="width: 96px;" alt="MySU Logo" />
  <h1>MySU: A Personal Fork</h1>
  <p><strong>Kernel-Assisted Superuser & Systemless Framework for TheVoid & Custom Kernels</strong></p>
</div>

---

> [!NOTE] Personal Project Context & Attribution
> **MySU** is a personal, heavily refactored, and rebranded fork of upstream **[KernelSU](https://github.com/tiann/KernelSU)**, maintained by **[kelexine](https://github.com/kelexine)** primarily for personal use on custom Linux kernels (`TheVoid-Kernel`, 4.19.x legacy, 5.10+, and 6.x).
>
> All core architectural concepts, kernel hooking foundations, and superuser paradigms originate from the pioneering work of **weishu (tiann)** and the **KernelSU Contributors**.

## Overview

**MySU** is engineered for custom kernel environments, focusing on older 4.19.x kernel versions as well as modern GKI 2.0+ across `arm64` and `x86_64`. It provides fine-grained root access control, dynamic in-memory SELinux live-patching, and an isolated userspace environment without relying on filesystem symlinks.

For the full interactive documentation and guides, visit the [official documentation](https://kelexine.github.io/mysu/) or run the local documentation server:

```bash
cd website
bun install
bun run docs:dev
```

---

## Key Features

1. **Kernel-Level Privilege Escalation**: Direct syscall and LSM hook infrastructure paired with anonymous inode ioctl communication (`[mysu_driver]`).
2. **Dynamic In-Memory SELinux Live-Patching**: Modifies runtime access vectors (`avtab`) to grant root domain permissions without disabling enforcing mode.
3. **Pure Rust Userspace (`mysud`)**: Multi-call daemon managing `/data/adb/mysu/` with programmatic module compatibility and zero filesystem symlink clutter.
4. **Per-App Sandboxing & App Profiles**: Granular privilege delegation (UID/GID, capability bitmasks, seccomp filters, custom SELinux domains).
5. **Pluggable Metamodule Architecture**: Decoupled overlayfs and mount virtualization infrastructure for clean, systemless modifications.
6. **TheVoid Adaptive Material 3 Manager**: Android management app with deep obsidian dark themes, Void violet accents, and isolated App Zygote services.

---

## Documentation Sitemap

- **Getting Started**:
  - [What is MySU?](https://kelexine.github.io/mysu/guide/what-is-mysu.html)
  - [Difference with Magisk](https://kelexine.github.io/mysu/guide/difference-with-magisk.html)
  - [App Profile & Sandboxing](https://kelexine.github.io/mysu/guide/app-profile.html)
- **Installation & Integration**:
  - [Installation Guide](https://kelexine.github.io/mysu/guide/installation.html)
  - [How to Build](https://kelexine.github.io/mysu/guide/how-to-build.html)
  - [Integrate for non-GKI Devices](https://kelexine.github.io/mysu/guide/how-to-integrate-for-non-gki.html)
  - [x86_64 Architecture Support](https://kelexine.github.io/mysu/guide/x86_64-support.html)
- **Module Development**:
  - [Module Development Guide](https://kelexine.github.io/mysu/guide/module.html)
  - [Metamodule Architecture](https://kelexine.github.io/mysu/guide/metamodule.html)
  - [Module WebUI](https://kelexine.github.io/mysu/guide/module-webui.html)
  - [Module Configuration](https://kelexine.github.io/mysu/guide/module-config.html)
- **Reference & Troubleshooting**:
  - [Rescue from Bootloop](https://kelexine.github.io/mysu/guide/rescue-from-bootloop.html)
  - [Hidden Features & Advanced CLI](https://kelexine.github.io/mysu/guide/hidden-features.html)
  - [Frequently Asked Questions](https://kelexine.github.io/mysu/guide/faq.html)

---

## License

- Files under the `kernel/` directory: [GPL-2.0-only](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)
- Userspace (`mysud`/`mysuinit`) and Manager: [GPL-3.0-or-later](https://www.gnu.org/licenses/gpl-3.0.html)

---

## Credits & Upstream Attribution

> [!NOTE]
> MySU is a personal, heavily refactored, and rebranded fork of [KernelSU](https://github.com/tiann/KernelSU), engineered for custom kernel environments.

- **[KernelSU](https://github.com/tiann/KernelSU)**: Pioneered by **weishu (tiann)** and the KernelSU contributors.
- **[Kernel-Assisted Superuser](https://git.zx2c4.com/kernel-assisted-superuser/about/)**: Foundational concept.
- **[Magisk](https://github.com/topjohnwu/Magisk)**: Systemless Android root pioneer.
- **[genuine](https://github.com/brevent/genuine/)**: APK v2 signature validation.