# MySU -: A personal fork

<img src="https://mysu.org/logo.png" style="width: 96px;" alt="logo"> (To be Updated)

A kernel-based root solution for Android devices.

## Features

1. Kernel-based `su` and root access management.
2. Module system based on [metamodules](https://mysu.org/guide/metamodule.html): Pluggable infrastructure for systemless modifications. (To be Updated)
3. [App Profile](https://mysu.org/guide/app-profile.html): Lock up the root power in a cage. (To be Updated)

## Compatibility state

MySU officially supports Android GKI 2.0 devices (kernel 5.10+). Older kernels (4.14+) are also supported, but the kernel will need to be built manually. this fork focuses on older 4.19.x kernel versions for personal use.

With this, WSA, ChromeOS, and container-based Android are all supported.

Currently, the `arm64-v8a` and `x86_64` architectures are supported.

> [!CAUTION]
> Recent kernel versions have implemented a breaking change causing MySU to fail and potentially trigger a kernel panic on `x86_64`! Check the website for more info!


## Security

For information on reporting security vulnerabilities in MySU, see [SECURITY.md](/SECURITY.md).

## License

- Files under the `kernel` directory are [GPL-2.0-only](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html).
- All other parts except the `kernel` directory are [GPL-3.0-or-later](https://www.gnu.org/licenses/gpl-3.0.html).

## Credits & Attribution

> [!NOTE]
> MySU is a personal, heavily rebranded and refactored fork of [KernelSU](https://github.com/tiann/KernelSU), engineered for personal use across custom kernel environments.

- **[KernelSU](https://github.com/tiann/KernelSU)**: The groundbreaking kernel root framework by weishu and contributors.
- **[Kernel-Assisted Superuser](https://git.zx2c4.com/kernel-assisted-superuser/about/)**: The foundational kernel-assisted root concept.
- **[Magisk](https://github.com/topjohnwu/Magisk)**: The legendary Android root solution.
- **[genuine](https://github.com/brevent/genuine/)**: APK v2 signature validation.
- **[Diamorphine](https://github.com/m0nad/Diamorphine)**: LKM and hooking techniques.