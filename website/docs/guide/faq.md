# FAQ

## What is the relationship between MySU and KernelSU?

MySU is a personal, heavily refactored, and rebranded fork of **[KernelSU](https://github.com/tiann/KernelSU)** maintained for personal use on custom Linux kernels (4.19.x, 5.10+, and 6.x). It features a streamlined userspace daemon (`mysud`), a clean `/data/adb/mysu/` directory layout with zero filesystem symlinks, and TheVoid adaptive Material 3 & Miuix manager theme.

All credit for the groundbreaking kernel-assisted root concept and core driver architecture goes to **weishu (tiann)** and the **KernelSU team**.

## Does MySU support my device?

MySU supports devices running Android with an unlocked bootloader across both modern GKI 2.0 (Linux 5.10–6.13+) and custom/legacy non-GKI kernels (4.14.x, 4.19.x, and 5.4.x).

You can easily check the support for your device through the MySU manager, which is available [here](https://github.com/kelexine/mysu/releases). 

If the app shows `Not installed`, it means your device kernel already includes MySU or is a compatible GKI image.

If the app shows `Unsupported`, it means your device's stock kernel does not currently have MySU integrated. You can compile your kernel source with MySU enabled or build a standalone LKM (`mysu.ko`). See [Integrate for non-GKI Devices](how-to-integrate-for-non-gki).

## Do I need to unlock the bootloader to use MySU?

Yes. MySU requires an unlocked bootloader.

## Does MySU support modules?

Yes, most Magisk modules work on MySU. However, if your module needs to modify `/system` files, you need to install a [metamodule](metamodule.md) (such as `meta-overlayfs`). Other module features work without a metamodule. Check [Module guide](module.md) for more info.

## Does MySU support Xposed?

Yes, you can use LSPosed (or other modern Xposed derivative) with [ZygiskNext](https://github.com/Dr-TSNG/ZygiskNext).

## Does MySU support Zygisk?

MySU has no built-in Zygisk support, but you can use a module like [ZygiskNext](https://github.com/Dr-TSNG/ZygiskNext) to support it.

## Is MySU compatible with Magisk?

MySU's module system conflicts with Magisk's magic mount. If any module is enabled in MySU, Magisk will stop working entirely.

However, if you only use the `su` of MySU, it will work well with Magisk. MySU modifies the `kernel`, while Magisk modifies the `ramdisk`, allowing both to work together.

## Will MySU substitute Magisk?

No. Replacing Magisk isn't our goal. Magisk is already an excellent userspace root solution. MySU focuses on exposing kernel interfaces to users instead of supplanting Magisk.

## Can MySU support non-GKI devices?

It's possible. But you should download the kernel source, integrate MySU into the source tree, and compile the kernel yourself.

## Can MySU support devices below Android 12?

It's the device's kernel that affects MySU's compatibility, and it has nothing to do with the Android version. The only restriction is that devices launched with Android 12 must have a kernel version of 5.10+ (GKI devices). So:

1. Devices launched with Android 12 must be supported.
2. Devices with an older kernel (some devices with Android 12 also have the older kernel) are compatible (you should build kernel yourself).

## Can MySU support old kernels?

Yes. MySU has been validated on Linux 4.14.x, 4.19.x, and 5.4.x trees as well as modern 5.10–6.13+ GKI kernels.

## How to integrate MySU for an older kernel?

Please check the [Integrate for non-GKI devices](how-to-integrate-for-non-gki) guide.

## Why my Android version is 13, and the kernel shows "android12-5.10"?

The kernel version has nothing to do with the Android version. If you need to flash kernel, always use the kernel version; the Android version isn't as important.

## I'm GKI 1.0, can I use this?

GKI 1.0 is completely different from GKI 2.0, you must compile kernel by yourself.

## How can I make `/system` RW?

We don't recommend that you modify the system partition directly. Please check [Module guide](module.md) to modify it systemlessly. If you insist on doing this, check [magisk_overlayfs](https://github.com/HuskyDG/magic_overlayfs).

## Can MySU modify hosts? How can I use AdAway？

Of course. But MySU doesn't have built-in hosts support, you can install a module like [systemless-hosts](https://github.com/symbuzzer/systemless-hosts-MySU-module) to do it.

## Why aren't my modules working after fresh install?

If your modules need to modify `/system` files, you need to install a [metamodule](metamodule.md) to mount the `system` directory. Other module features (scripts, sepolicy, system.prop) work without a metamodule.

**Solution**: See the [Metamodule Guide](metamodule.md) for installation instructions.

## What is a metamodule and why do I need one?

A metamodule is a special module that provides infrastructure for mounting regular modules. See the [Metamodule Guide](metamodule.md) for a complete explanation.
