# Difference with Magisk

Although MySU and Magisk modules have many similarities, there are inevitably some differences due to their completely different implementation mechanisms. If you want your module to work on both Magisk and MySU, it's essential to understand these differences.

## Similarities

- Module file format: Both use the ZIP format to organize modules, and the module format is practically the same.
- Module installation directory: Both are located at `/data/adb/modules`.
- Systemless: Both support modifying `/system` in a systemless way through modules.
- post-fs-data.sh: Execution time and semantics are exactly the same.
- service.sh: Execution time and semantics are exactly the same.
- system.prop: Completely the same.
- sepolicy.rule: Completely the same.
- BusyBox: Scripts are run in BusyBox with "Standalone Mode" enabled in both cases.

## Differences

Before understanding the differences, it's important to know how to identify whether your module is running in MySU or Magisk. You can use the environment variable `MYSU` to differentiate it in all places where you can run module scripts (`customize.sh`, `post-fs-data.sh`, `service.sh`). In MySU, this environment variable will be set to `true`.

Here are some differences:

- MySU modules cannot be installed in Recovery mode.
- MySU modules don't have built-in support for Zygisk, but you can use Zygisk modules through [ZygiskNext](https://github.com/Dr-TSNG/ZygiskNext).
- **Module mounting architecture**: MySU uses a [metamodule system](metamodule.md) where mounting is delegated to pluggable metamodules (e.g., `meta-overlayfs`), while Magisk has mounting built into its core. MySU requires installing a metamodule to enable module mounting.
- The method for replacing or deleting files in MySU modules is completely different from Magisk. MySU doesn't support the `.replace` method. Instead, you need to create a same-named file with `mknod filename c 0 0` to delete the corresponding file.
- The directories for BusyBox are different. The built-in BusyBox in MySU is located at `/data/adb/mysu/bin/busybox`, while in Magisk it is at `/data/adb/magisk/busybox`. **Note that this is an internal behavior of MySU and may change in the future!**
- MySU doesn't support `.replace` files, but it supports the `REMOVE` and `REPLACE` variables to remove or replace files and folders.
- MySU adds the `boot-completed` stage to run scripts after the boot process is finished.
- MySU adds the `post-mount` stage to run scripts after module mounting is complete.
