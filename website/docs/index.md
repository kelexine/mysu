---
layout: home
title: Home

hero:
  name: MySU
  text: Kernel-Assisted Root & Systemless Framework for Android
  tagline: Engineered for custom kernels (4.19.x through 6.13+ GKI) with anonymous inode ioctls, live SELinux patching, and zero filesystem symlinks.
  image:
    src: /logo.png
    alt: MySU Logo
  actions:
    - theme: brand
      text: Get Started
      link: /guide/what-is-mysu
    - theme: alt
      text: Installation Guide
      link: /guide/installation
    - theme: alt
      text: GitHub Repository
      link: https://github.com/kelexine/mysu

features:
  - icon: ⚡
    title: Ring 0 Kernel Security Engine
    details: Operates inside the Linux kernel with dynamic syscall & LSM hooking, anonymous inode supercalls ([mysu_driver]), and hardware-level isolation.
  - icon: 🛡️
    title: Per-App Granular Sandboxing
    details: Strict allowlisting ensures only authorized apps see root. Non-root apps observe an unmodified stock Android environment with auto-unmounted modules.
  - icon: ⚙️
    title: Pure Userspace Daemon (mysud)
    details: Clean /data/adb/mysud binary with programmatic module adaptation, magic mount overlay virtualization, and zero symlink footprint.
  - icon: 🔒
    title: Granular App Profiles & Capabilities
    details: Configure root mount namespaces, Linux capability bitmasks, custom UID/GID mappings, and seccomp filters per application.
  - icon: 🧩
    title: Live In-Memory SELinux Patching
    details: Modifies runtime avtab and policydb to grant root execution privileges while keeping system SELinux fully in Enforcing mode.
  - icon: 🎨
    title: TheVoid Adaptive Material 3 Manager
    details: Android management app built with Jetpack Compose, featuring obsidian dark themes, Void violet accents, and isolated App Zygote services.
---

<div class="showcase-container">
  <div class="showcase-header">
    <div class="showcase-dots">
      <div class="showcase-dot dot-red"></div>
      <div class="showcase-dot dot-yellow"></div>
      <div class="showcase-dot dot-green"></div>
    </div>
    <div class="showcase-title">mysud — terminal quickstart</div>
  </div>
  <div class="showcase-body">

```bash
# Integrate MySU into your kernel source tree
curl -LSs "https://raw.githubusercontent.com/kelexine/mysu/main/kernel/setup.sh" | bash -

# Verify kernel driver and supercall initialization
mysud su -c "id; getprop ro.product.model"
# Output: uid=0(root) gid=0(root) groups=0(root) context=u:r:mysu:s0
```

  </div>
</div>

