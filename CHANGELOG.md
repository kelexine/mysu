# Changelog

All notable changes to the **MySU** project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

---

## [1.0.4] - 2026-08-22

### Added
- **Legacy Kernel Compatibility (4.19 / 5.4)**: Integrated backward compatibility layers across kernel subsystems:
  - `ksys_umount` fallback via `set_fs(KERNEL_DS)` for Linux `< 5.9.0`.
  - `selinux_status_lock` / `selinux_status_page` macros and `fake_state.ss` support for `< 5.7.0` & `< 5.10.0`.
  - `probe_kernel_read` / `write` mappings, `TWA_RESUME` / `TWA_NONE` compatibility definitions in `util.h`.
  - Legacy `fsnotify` event handler fallback for `< 5.3.0`.
  - Linux 4.19 `policydb` `rwlock` synchronization, `flex_array` operations, and legacy `add_type` / `handle_sepolicy`.
  - `do_mount` fallback for private mount namespace setup on `< 5.8.0`.
- **AI Agent Guidelines**: Added [`AGENTS.md`](file:///home/kelexine/dev/mysu/AGENTS.md) defining architecture mapping, kernel/userspace standards, and agent operational constraints.

### Fixed
- **String Helper Macro Guard**: Restricted `strscpy_pad` inline fallback strictly to Linux `< 4.20.0` in `util.h` to resolve conflicting static declaration errors on Android GKI 5.10–6.6.
- **Kernel Signature Hash Synchronization**: Updated default `MYSU_EXPECTED_SIZE` (`0x056d`) and `MYSU_EXPECTED_HASH` in `kernel/Kbuild` to match the official GitHub release signing keystore certificate.
- **Manager App Scroll Glitch**: Resolved double-nested `verticalScroll` modifier collision in `AppUpdateDialog.kt`.

---

## [1.0.3] - 2026-08-21

### Added
- **Unified Master Release Pipeline**: Orchestrated `build-manager`, `build-userspace`, and `build-lkm` in `.github/workflows/release.yml` to produce consolidated GitHub release assets.
- **Kernel LKM Matrix Compilation**: Added `ddk-lkm.yml` and `build-lkm.yml` matrix builds supporting Android GKI versions `android12-5.10` through `android16-6.12` across `aarch64` and `x86_64`.
- **Automated Conventional Changelog**: Configured `.github/release.yml` with semantic commit classification rules.

### Changed
- **Reusable Workflow Triggers**: Added `workflow_call:` triggers to `build-manager.yml` and `build-userspace.yml`.
- **Standardized Artifact Pipeline**: Aligned artifact names for Manager APKs, LKM kernel modules, `mysud`, and `mysuinit` binaries.
- **Kernel Build Flag Alignment**: Updated DDK LKM compiler flags to `CONFIG_MYSU=m`, `MYSU_EXPECTED_SIZE2`, `MYSU_EXPECTED_HASH2`, and `CONFIG_MYSU_X86_PATCH_SYSCALL_DISPATCHER=y`.

---

## [1.0.2] - 2026-08-21

### Added
- **In-App Update Dialog**: Replaced browser redirects with an adaptive in-app update dialog in [`AppUpdateDialog.kt`](file:///home/kelexine/dev/mysu/manager/app/src/main/java/dev/kelexine/mysu/ui/component/dialog/AppUpdateDialog.kt) supporting both Material 3 and Miuix layouts.
- **Scrollable Markdown Changelog**: Native rendering of GitHub release notes via Compose `MarkdownContent`.
- **Live Download Progress Bar**: Real-time linear progress indicator with percentage display during background APK download.
- **Public Downloads Destination**: Update APKs are saved directly to the device `Downloads/` directory (`/sdcard/Download/MySU_<versionCode>.apk`).
- **Automatic Package Installation**: Direct APK install dispatch via [`ApkInstaller.kt`](file:///home/kelexine/dev/mysu/manager/app/src/main/java/dev/kelexine/mysu/ui/util/ApkInstaller.kt), `FileProvider`, and `REQUEST_INSTALL_PACKAGES` permission check.

---

## [1.0.1] - 2026-08-21

### Added
- **True AMOLED Black Palette**: Pure pitch black (`#000000`) background and surfaces with elevated container contrast (`#070707`, `#0C0C0C`, `#141414`) for battery efficiency.
- **TheVoid Obsidian Theme Tokens**: Synchronized dark mode background tokens (`#08090E`, `#0E1017`, `#131622`).
- **Non-GKI Integration Diagnostic Hints**: Clear kernel state diagnostics and action tag linking to Non-GKI integration documentation.
- **Developer Support Link**: Integrated Buy Me a Coffee support card linking to [@kelexine](https://buymeacoffee.com/kelexine).
- **Minimalist Vector Brand Identity**: Redesigned clean geometric `#` vector launcher icons (`ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`), banners, and documentation branding.

### Changed
- **Hosting Migration**: Migrated all documentation, template repository endpoints, and sitemap configuration from `mysu.org` to [`kelexine.github.io/mysu/`](https://kelexine.github.io/mysu/).
- **Strings & Localization**: Updated `home_learn_mysu_url` across 38 localization files.

---

## [1.0.0] - 2026-08-19

### Added
- **Kernel-Level Privilege Escalation**: Anonymous inode (`[mysu_driver]`) supercalls triggered via privileged `sys_reboot` trap.
- **In-Memory Dynamic SELinux Live-Patching**: Runtime `avtab` and `policydb` modification granting `u:r:mysu:s0` domain while preserving system Enforcing status.
- **Pure Rust Userspace (`mysud` & `mysuinit`)**: Standalone binary daemons managing `/data/adb/mysu/` without symlink pollution.
- **Per-App Sandboxing & App Profiles**: Granular privilege delegation (UID/GID, capability bitmasks, seccomp filters, custom SELinux domains).
- **Pluggable Metamodule Architecture**: Decoupled overlayfs and mount virtualization infrastructure.
- **TheVoid Adaptive Material 3 Manager**: Android app supporting both Material 3 Expressive and Miuix design languages with App Zygote isolation.
- **Comprehensive Documentation Suite**: VitePress documentation with architectural diagrams, CLI references, and non-GKI / x86_64 integration guides.

---

<!-- generated: antigravity-cli | gemini-3.7-flash | 2026-08-21 -->
