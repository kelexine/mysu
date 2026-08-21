# How to Build

This guide provides instructions for building all components of MySU:
1. **Kernel Driver** (`kernel/`) — In-tree kernel driver or standalone Loadable Kernel Module (`mysu.ko`).
2. **Userspace Daemons** (`userspace/mysud` & `mysuinit`) — Rust multi-call binary and early-init injector.
3. **Android Manager** (`manager/`) — Jetpack Compose management APK (`dev.kelexine.mysu`).
4. **Repacking & Signing** (`repack_apk.py`) — Embedding native binaries into the APK and performing APK v2 signing.

---

## 1. Building the Kernel Driver

### In-Tree Kernel Integration
To build MySU directly into your kernel source tree:

```bash
# In your kernel source tree root:
curl -LSs "https://raw.githubusercontent.com/kelexine/mysu/main/kernel/setup.sh" | bash -

# Verify configuration in defconfig or menuconfig:
# CONFIG_MYSU=y
```

Then build your kernel image as usual using your toolchain (Clang/LLVM, GCC, or Bazel/Kleaf for Android 13+ GKI).

### Standalone LKM (`mysu.ko`)
To build MySU as an out-of-tree loadable module:

```bash
cd kernel
# Set KDIR to your configured kernel build tree / headers
make -C $KDIR M=$(pwd) modules
```

---

## 2. Building Userspace Daemons (`mysud` & `mysuinit`)

### Prerequisites
- **Rust Toolchain**: Rust 1.85+ (Edition 2024 support)
- **Cross**: `cargo install cross --git https://github.com/cross-rs/cross`
- **Android NDK**: NDK r25c+ (configured in `ANDROID_NDK_HOME`)
- **Just**: `cargo install just`

### Build with Just
Use the root `justfile` for automated compilation:

```bash
# Build mysud release binary for aarch64-linux-android:
just build_mysud

# Lint with clippy and check formatting:
just clippy
```

Alternatively, build directly using `cargo` or `cross`:

```bash
cross build --target aarch64-linux-android --release --package mysud
cross build --target aarch64-linux-android --release --package mysuinit
```

The resulting binaries will be placed in `target/aarch64-linux-android/release/`.

---

## 3. Building the Android Manager App

### Prerequisites
- **Java Development Kit**: OpenJDK 17 or 21
- **Android SDK**: Build-Tools 35.0.0+, Platform SDK 35
- **CMake**: 3.18.1+ (for JNI native library compilation)

### Build Debug APK with Just
The `just build_manager` target builds `mysud`, copies the binary as `libmysud.so` to the JNI libs directory, and triggers the Gradle build:

```bash
just build_manager
```

### Build via Gradle Directly
```bash
cd manager
./gradlew assembleDebug
# Output: manager/app/build/outputs/apk/debug/app-debug.apk
```

---

## 4. Packaging and Repacking (`repack_apk.py`)

MySU includes a comprehensive Python script to automate cross-compiling `mysud`, stripping symbols with NDK `llvm-strip`, injecting libraries, and signing the final APK with custom keystores:

```bash
# Example invocation using config file:
python3 repack_apk.py --config repack-config.example.json

# Custom command line arguments:
python3 repack_apk.py \
    --app-build-type release \
    --mysud-build-type release \
    --arch arm64-v8a,x86_64 \
    --strip \
    --keystore-path /path/to/keystore.jks \
    --key-alias mykey
```

### Synchronizing Kernel Signature Hash
The kernel verifies the Manager APK via embedded APK v2 signature checks. If you use a custom release keystore:
1. Run `python3 repack_apk.py` or `mysud debug get-sign <signed_manager.apk>` to compute the size and SHA-256 hash.
2. Ensure `EXPECTED_SIZE` and `EXPECTED_HASH` in `kernel/manager/apk_sign.h` match your signature.
