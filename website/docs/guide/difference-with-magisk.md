# Difference with Magisk

While **MySU** and **Magisk** both provide systemless modification environments and superuser management for Android, their fundamental architectures and threat models differ substantially.

---

## Architectural Comparison

| Dimension | Magisk | MySU |
|---|---|---|
| **Execution Space** | Userspace (`app_process` / Zygote hijacking) | **Ring 0 Kernel Space** (Syscall & LSM hooks) |
| **Root Interception** | Replaces `/system/bin/app_process` to inject into Zygote | Dynamic kernel dispatcher intercepts `execve`/`setresuid` |
| **Root Detection Resistance** | Relies on DenyList / Zygisk unmounting in userspace | **Kernel-enforced isolation**: unauthorized apps never see `su` or mounts |
| **SELinux Strategy** | Live reload of policy database using `magiskpolicy` | **In-memory live-patching** of `avtab`/`policydb` (Enforcing mode retained) |
| **Manager Communication** | Local UNIX domain sockets (`/dev/socket/...`) | **Anonymous inode supercalls** (`[mysu_driver]` ioctls) |
| **Manager Verification** | Package name check & userspace keystore checks | **In-kernel APK v2 signature verification** (`EXPECTED_HASH`/`SIZE`) |
| **Module Mounting** | Hardcoded magic mount loopback devices | **Pluggable Metamodules** (OverlayFS / Magic Mount) |
| **Userspace Footprint** | `/data/adb/magisk/` with daemon symlinks | `/data/adb/mysud` (Rust 2024 binary, zero symlinks) |
| **Recovery Mode** | Supports installation via TWRP / custom recovery | Installed via boot image patching, fastboot, or kernel flashing |

---

## Technical Differences in Detail

### 1. Privilege Escalation Mechanism
- **Magisk**: Hooks Android's `Zygote` process to intercept app forks and inject credentials. Superuser shells rely on `/system/bin/su` binaries or socket communication with `magiskd`.
- **MySU**: Operates entirely in kernel space. When an authorized process requests root via `su` or ioctl, the kernel driver directly switches the process credentials (`prepare_creds()` & `commit_creds()`). Unauthorized apps have no way to reach the supercall interface.

### 2. SELinux Policy Enforcement
- **Magisk**: Compiles rules into SELinux policy files and reloads the active policy using `selinux_setenforce` or policy injection.
- **MySU**: Does not reload SELinux or toggle enforce state. It alters access vector table entries directly in kernel RAM, granting root execution rights to `u:r:mysu:s0` without triggering audit tamper alerts.

### 3. Namespace Isolation & `kernel_umount`
- **Magisk**: Unmounts module overlays when spawning apps configured in the DenyList. However, detection tools can probe mount namespace timing or trace userspace hooks.
- **MySU**: Employs `kernel_umount` inside the kernel's process creation path. For any non-root application, all `/data/adb` and module mount descriptors are cleanly stripped before user code executes.

---

## Module Compatibility & Porting

### Identifying the Environment
Modules can detect MySU using the `MYSU` environment variable, which is automatically set to `true` during script executions (`customize.sh`, `post-fs-data.sh`, `post-mount.sh`, `service.sh`, `boot-completed.sh`):

```bash
if [ "$MYSU" = "true" ]; then
    ui_print "- Running under MySU (Version: $MYSU_VERSION, Kernel: $MYSU_KERNEL_VERSION)"
fi
```

### Module Script Lifecycle Stages
MySU supports all standard Magisk stages, plus dedicated post-mount and boot-completion triggers:
1. **`post-fs-data.sh`**: Runs before `/data` is fully decrypted or Zygote spawns (BLOCKING).
2. **`post-mount.sh`**: Runs immediately after the metamodule finishes mounting systemless overlays (BLOCKING).
3. **`service.sh`**: Runs in late-start service mode in the background (NON-BLOCKING).
4. **`boot-completed.sh`**: Runs once the Android system broadcasts `ACTION_BOOT_COMPLETED` (NON-BLOCKING).

### Automated Porting Tool
To convert legacy Magisk/KernelSU modules to native MySU modules without manual editing, use the bundled migration script:

```bash
# Convert a legacy zip package
./scripts/mysu-module-port.sh legacy_module.zip output_mysu.zip

# Or using the justfile shortcut:
just port_module legacy_module.zip
```

The porter automatically rewrites `/data/adb/ksu` and `/data/adb/magisk` paths to `/data/adb/mysu`, updates daemon calls from `ksud`/`magisk` to `mysud`, and preserves binary integrity.
