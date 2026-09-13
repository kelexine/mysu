# Changelog

All notable changes to the **MySU** project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

---

## [1.0.9] - 2026-09-13

### Fixed
- **Kernel Manager Certificate Size Limit**:
  - Lifted hardcoded `CERT_MAX_LENGTH` limit in `kernel/manager/apk_sign.c` from 1024 to 4096 bytes.
  - Replaced fixed stack buffer `char cert[CERT_MAX_LENGTH]` with dynamic kernel allocation `kmalloc(certificate_size, GFP_KERNEL)` to safely accommodate larger X.509 certificates (e.g. 1389-byte / `0x056d` custom release certificates) without overflowing kernel stack frames.
  - Fixed false-positive `"cert length overlimit"` rejection during APK v2 signature verification, restoring manager APK crowning and driver connection on custom-signed builds.

---

## [1.0.8] - 2026-09-12

### Fixed
- **Legacy Kernel Seccomp Action Cache & ABI Alignment**:
  - Resolved fatal `SIGSYS` (`SYS_SECCOMP` syscall 142) crash on Linux < 5.9 kernels paired with Android 16 userspace by guarding `struct seccomp_filter` layout to prevent 4-byte member offset drift.
  - Recursively traversed the entire filter hierarchy in `mysu_seccomp_allow_cache()` and `mysu_seccomp_clear_cache()` to ensure ancestor filters permit syscalls.
- **ARM64 Top-Byte-Ignore (TBI) Pointer Sanitization**:
  - Sanitized userspace pointers with `untagged_addr()` across `argv` array and strings in `mysud_integration.c` to prevent pointer dereference faults from Bionic memory tagging.
  - Sanitized user output pointer with `untagged_addr()` in `reboot_handler_pre()` (`supercall.c`) before scheduling `task_work`.
- **Init & Boot Event Detection**:
  - Added `/init.rc` fallback to `is_init_rc()` and expanded argument matching for `/init` and `--zygote` in `mysud_integration.c`.
  - Triggered `track_throne(false)` with manager search on boot completion when `mysu_manager_appid` is invalid.

### Added
- **Porting Automation & Test Suite**:
  - Modularized `mysu_port` package and introduced unit test suite for rule transformations and string replacement semantics.
- **Documentation**:
  - Updated guide and technical references for Android 16 compatibility and kernel support matrix.

---

## [1.0.7] - 2026-08-28

### Added
- **Kernel Zero-Allocation VFS Path Filtering**:
  - Implemented `dentry_leaf_may_be_cloaked()` fast-path in `getdents64` handler to eliminate `kmalloc(PATH_MAX)` allocations on non-cloaked directory traversals.
  - Added dentry leaf name pre-filtering in `inode_getattr` LSM hook to skip expensive path resolution and heap allocations on irrelevant files.
  - Pre-computed static prefix lengths in `vfs_hide` prefix table, removing hot-path `strlen()` invocations.
- **Kernel Lockless RCU Policy Fast Path**:
  - Optimized `mysu_uid_should_umount()` to read profile fields directly under `rcu_read_lock()` without `kref_get`/`kref_put` atomic reference count modifications.
- **Manager Jetpack Compose Stability**:
  - Annotated [`AppInfo`](file:///home/kelexine/dev/mysu/manager/app/src/main/java/dev/kelexine/mysu/data/model/AppInfo.kt) with `@Immutable` to enable skippable list item recompositions across `SuperUserScreen` and `AppProfileScreen`.
  - Parallelized application metadata and profile queries using chunked coroutines (`Dispatchers.Default`), reducing initial app list load time by ~60%.
- **Userspace ARM64 LSE Atomics**:
  - Configured `target-feature=+lse,+crc` rustflags for `aarch64-linux-android` target in `.cargo/config.toml` for single-instruction atomic operations.
- **CI/CD Build Matrix Caching**:
  - Integrated `actions/cache` for `ccache` in [`ddk-lkm.yml`](file:///home/kelexine/dev/mysu/.github/workflows/ddk-lkm.yml) across all GKI KMI kernel module builds.

---

## [1.0.6] - 2026-08-23

### Added
- **Kernel VFS Path Virtualization & Module Cloaking**: Introduced `MYSU_FEATURE_VFS_HIDE` (ID=5) to make root artifacts invisible to denied/unprivileged applications:
  - Intercepted `getdents64` syscall to compact out `/data/adb`, `/data/adb/modules`, `/data/adb/mysu`, `/data/adb/magisk`, and `/system/bin/su` entries in directory streams.
  - Hooked LSM `inode_getattr` to return `-ENOENT` on `stat()`/`statx()`/`access()` for cloaked paths when called from processes in the umount list.
- **Daemon & CLI Feature Support**: Extended `mysud` with `FeatureId::VfsHide` supporting `mysud feature check/get/set/list/save vfs_hide`.
- **Manager App Configuration**:
  - Implemented JNI bridge functions `isVfsHideEnabled()` and `setVfsHideEnabled()`.
  - Added reactive UI toggles with `VisibilityOff` icons in both Material 3 Expressive and Miuix settings layouts.
  - Integrated settings persistence via daemon feature save.

---

## [1.0.5] - 2026-08-23

### Added
- **Biometric & Device Credential Security Gate**: Integrated hardware-backed biometric authentication (`BIOMETRIC_STRONG | DEVICE_CREDENTIAL`) across the Manager app:
  - Added [`BiometricSecurityManager`](file:///home/kelexine/dev/mysu/manager/app/src/main/java/dev/kelexine/mysu/ui/security/BiometricSecurityManager.kt) coroutine bridge supporting fingerprint, face unlock, and device PIN/pattern.
  - Implemented configurable session authentication timeouts (`Every time`, `1 minute`, `5 minutes`, `Until app closes`).
  - Added granular security policies to protect root granting/revocation, app profile edits, kernel setting modifications, and module operations.
  - Added optional App Launch Lock protecting the Manager on application startup.
  - Added dedicated Security & Authentication preference sections in Material 3 Expressive and Miuix layouts.

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
