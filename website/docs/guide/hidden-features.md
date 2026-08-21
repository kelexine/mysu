# Hidden Features & Advanced CLI

MySU includes a variety of advanced features, developer switches, and kernel controls that can be utilized directly from the command line or configuration files.

---

## 1. Shell Customization (`.mysurc`)

By default, Android's `/system/bin/sh` loads `/system/etc/mkshrc`. When entering a root shell via `mysud su` or `su`, MySU checks for a custom initialization file.

Create `/data/adb/mysu/.mysurc` to set custom aliases, exports, and functions:

```bash
# Example /data/adb/mysu/.mysurc
alias ll='ls -la'
alias cls='clear'
export PATH="/data/adb/mysu/bin:$PATH"
export HISTFILE="/data/adb/mysu/.sh_history"
```

To prevent loading custom rc files, pass the `--norc` flag:
```bash
mysud su --norc
```

---

## 2. Advanced `mysud` CLI Reference

The userspace daemon provides a multi-call interface for administration, debugging, and systemless integration:

### Superuser Invocation (`mysud su`)
```bash
# Standard interactive root shell:
mysud su

# Execute a single command with root privileges:
mysud su -c "id; whoami"

# Spawn root shell in global mount namespace (unisolated):
mysud su -g

# Spawn root shell with custom UID/GID:
mysud su -u 2000 -g 2000
```

### Module Management (`mysud module`)
```bash
# List all active and disabled modules:
mysud module list

# Install a module zip package:
mysud module install /path/to/module.zip

# Enable or disable a module by ID:
mysud module enable <module_id>
mysud module disable <module_id>

# Uninstall a module:
mysud module uninstall <module_id>
```

### System Properties (`mysud resetprop`)
MySU embeds a Magisk-compatible `resetprop` utility to read, modify, and delete Android system properties directly:

```bash
# Read a system property:
mysud resetprop ro.build.type

# Set or spoof a read-only property:
mysud resetprop ro.boot.verifiedbootstate green
mysud resetprop ro.boot.flash.locked 1

# Delete a property from property_service:
mysud resetprop --delete persist.sys.test
```

### SELinux Live-Testing (`mysud sepolicy`)
```bash
# Check syntax of a custom SELinux rule:
mysud sepolicy check "allow su * * *"

# Dynamically apply a rule at runtime:
mysud sepolicy apply "allow system_server mysu_daemon process getattr"
```

---

## 3. Kernel Feature Toggles

MySU features can be queried and modified at runtime via `mysud feature`:

```bash
# List all kernel features and their active states:
mysud feature list

# Toggle sucompat (interception of /system/bin/su):
mysud feature set su_compat 1

# Toggle kernel audit logging (sulog):
mysud feature set sulog 1

# Toggle adb_root integration:
mysud feature set adb_root 1

# Toggle SELinux rule hiding:
mysud feature set selinux_hide 1
```

---

## 4. Kernel Module Late-Loading (`mysud late-load`)

For devices using the Loadable Kernel Module (LKM) workflow:
- `mysud late-load` dynamically loads `mysu.ko` into the running kernel using `init_module` syscalls.
- Automatically handles symbol resolution, mounts necessary runtime nodes, and executes late-load stage scripts.

```bash
# Perform late load with automatic KMI detection:
mysud late-load

# Force allow shell permissions at init:
mysud late-load --allow-shell
```

---

## 5. Audit Logging (`mysud sulog`)

Track and monitor superuser privilege escalation in real time:

```bash
# Stream live su execution audit events from the kernel:
mysud sulog
```
