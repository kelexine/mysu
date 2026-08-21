# Changelog

All notable changes to the **MySU** project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

---

## [1.0.2] - 2026-08-21

### Added
- **In-App Update Dialog**: Replaced browser redirects with an adaptive in-app update dialog in [`AppUpdateDialog.kt`](file:///home/kelexine/dev/mysu/manager/app/src/main/java/dev/kelexine/mysu/ui/component/dialog/AppUpdateDialog.kt) supporting both Material 3 and Miuix layouts.
- **Scrollable Markdown Changelog**: Native rendering of GitHub release notes via Compose `MarkdownContent`.
- **Live Download Progress Bar**: Real-time linear progress indicator with percentage display during background APK download.
- **Public Downloads Destination**: Update APKs are saved directly to the device `Downloads/` directory (`/sdcard/Download/MySU_<versionCode>.apk`).
- **Automatic Package Installation**: Direct APK install dispatch via [`ApkInstaller.kt`](file:///home/kelexine/dev/mysu/manager/app/src/main/java/dev/kelexine/mysu/ui/util/ApkInstaller.kt), `FileProvider`, and `REQUEST_INSTALL_PACKAGES` permission check.
- **Automated GitHub Release Categorization**: Added `.github/release.yml` with Conventional Commit classification rules.

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
