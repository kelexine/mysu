# App Profile & Sandboxing

The **App Profile** is MySU's granular security management mechanism, allowing fine-grained policy customization on a per-application basis.

Rather than treating root access as a binary all-or-nothing switch, MySU follows the **Principle of Least Privilege**:
- **For Rooted Apps (Root Profile)**: Restrict UID, GID, supplementary groups, Linux capability bitmasks, mount namespace behavior, and custom SELinux domains when `su` is invoked.
- **For Non-Root Apps (Non-Root Profile)**: Control kernel module visibility, automatically strip sensitive mount points via `kernel_umount`, and enforce strict namespace isolation.

---

## 1. Root Profile (Privilege Sandboxing)

When an application is granted root, MySU enforces kernel-level boundaries whenever that application executes `su`.

### UID, GID, and Supplementary Groups
In Android, every application runs under an isolated UID (e.g., `10000`–`19999` for user apps, `2000` for ADB shell, `0` for root).

With MySU's Root Profile:
- You can constrain a root shell to run under UID `2000` (shell permissions) instead of UID `0`.
- You can strip supplementary groups (e.g., removing GID `3003` `inet` to prevent the root process from accessing network sockets).
- Enforced directly by the kernel's `commit_creds()` path—the app cannot voluntarily bypass this restriction.

### Linux Capabilities
Capabilities partition the traditional root power into distinct privileges:
- **`CAP_DAC_OVERRIDE` / `CAP_DAC_READ_SEARCH`**: Bypass file read/write permission checks.
- **`CAP_NET_ADMIN` / `CAP_NET_RAW`**: Configure network interfaces and routing tables.
- **`CAP_SYS_ADMIN`**: Perform mount operations and advanced system administration.

If an application only requires file inspection, you can grant `CAP_DAC_READ_SEARCH` while revoking `CAP_NET_ADMIN` and `CAP_SYS_ADMIN`.

::: tip Linux Capabilities Reference
Refer to the official Linux [capabilities(7) manual page](https://man7.org/linux/man-pages/man7/capabilities.7.html) for detailed descriptions of each capability flag.
:::

### Custom SELinux Domains
By default, root processes transition to the unrestricted `u:r:mysu:s0` domain. The Root Profile allows defining custom target domains (e.g., `u:r:app_sandboxed:s0`) with dedicated rules injected into the kernel's policy database:

```txt
type app_sandboxed;
enforce app_sandboxed;
typeattribute app_sandboxed mlstrustedsubject;
allow app_sandboxed system_file file read;
```

### Privilege Escalation Prevention (`NO_NEW_PRIVS`)
If you grant root access to UID `2000` (ADB shell) and configure a restricted app to run under UID `2000`, the app could attempt to execute `su` a second time to escape the sandbox.

To prevent this:
- Enable the **`NO_NEW_PRIVS`** flag in the App Profile.
- The kernel prevents any subsequent `su` invocations or `setuid` transitions from elevating privileges further.

---

## 2. Non-Root Profile (Detection Resistance)

### Automatic Mount Unmounting (`kernel_umount`)
Applications that are not granted root should not be able to detect root artifacts, modules, or `/data/adb` partitions.

- **Umount Modules by Default**: Enabled by default in Manager settings. Whenever a non-root application process is forked by Zygote, the kernel unmounts all custom module overlays, loopback mounts, and `/data/adb` paths from that process's mount namespace.
- **Per-App Toggle**: If a specific non-root app requires access to modified system libraries, you can explicitly uncheck "Umount modules" for that app.

::: info Kernel Version Compatibility
On Linux kernels 5.10+ (GKI 2.0), `kernel_umount` is natively supported. On legacy non-GKI kernels (such as 4.19.x), `path_umount` must be backported to `fs/namespace.c` for automatic unmounting to function. See [Integrate for non-GKI devices](how-to-integrate-for-non-gki.md).
:::
