---
layout: home
title: Home

hero:
  name: MySU
  text: Kernel-Level Root & Privilege Framework for Android
  tagline: Custom-engineered for TheVoid & modern Linux kernels with native userspace daemon and zero symlink footprint.
  image:
    src: /logo.png
    alt: MySU Logo
  actions:
    - theme: brand
      text: Get started
      link: /guide/what-is-mysu
    - theme: alt
      text: View on GitHub
      link: https://github.com/kelexine/mysu

features:
  - title: Kernel-Native Security Engine
    details: MySU executes at ring 0 inside the Linux kernel, providing seamless hardware privilege enforcement with untampered LSM and seccomp integration.
  - title: Per-App Granular Sandboxing
    details: Only explicitly authorized apps can invoke su or detect root. All unauthorized processes perceive a completely stock environment.
  - title: Pure Userspace Architecture (mysud)
    details: Clean /data/adb/mysu/ layout with programmatic module compatibility transformation and no polluting filesystem symlinks.
  - title: Custom Root Profiles & Capabilities
    details: Fine-tune UID, GID, supplementary groups, Linux capabilities, and dynamic SELinux domains on a per-package basis.
  - title: Extensible Metamodule Ecosystem
    details: Pluggable overlayfs and mount virtualization infrastructure for clean, systemless /system and vendor partitions.
  - title: TheVoid Adaptive Material 3
    details: First-class modern management dashboard crafted with fluid Material 3 design and deep void cosmic aesthetics.
