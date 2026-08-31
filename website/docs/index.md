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
    title: Per-App Sandboxing & VFS Cloaking
    details: Strict allowlisting, automatic kernel module unmounting, and in-kernel VFS path cloaking (/data/adb & TracerPid hiding) for unauthorized applications.
  - icon: ⚙️
    title: Pure Rust Userspace (mysud)
    details: Modular multi-call daemon at /data/adb/mysu/ with programmatic module adaptation, overlayfs management, and zero symlink clutter.
  - icon: 🔒
    title: Granular App Profiles & Capabilities
    details: Configure root mount namespaces, Linux capability bitmasks, custom UID/GID mappings, and seccomp filters per application.
  - icon: 🧩
    title: Live In-Memory SELinux Patching
    details: Modifies runtime avtab and policydb to grant root execution privileges while keeping system SELinux fully in Enforcing mode.
  - icon: 🛡️
    title: Hardware Biometric Security Gate
    details: AndroidX Biometric-gated root authorizations, profile management, and kernel configuration changes with configurable timeouts.
  - icon: 🎨
    title: TheVoid Adaptive M3 & Miuix Manager
    details: Android management app built with Jetpack Compose, featuring obsidian AMOLED pitch black themes and Void violet accents.
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

