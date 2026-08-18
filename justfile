alias bk := build_mysud
alias bmysud := build_mysud
alias bm := build_manager

build_mysud:
    cross build --target aarch64-linux-android --release

build_manager: build_mysud
    mkdir -p manager/app/src/main/jniLibs/arm64-v8a
    cp target/aarch64-linux-android/release/mysud manager/app/src/main/jniLibs/arm64-v8a/libmysud.so
    cd manager && ./gradlew aDebug

port_module input output="":
    ./scripts/mysu-module-port.sh {{input}} {{output}}

clippy:
    cargo fmt
    cross clippy --target aarch64-linux-android --release
