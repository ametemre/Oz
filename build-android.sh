#!/bin/bash

# Build for all targets
TARGETS=(
    "aarch64-linux-android"    # arm64-v8a
    "armv7-linux-androideabi"  # armeabi-v7a
    "x86_64-linux-android"     # x86_64
)

# Root project directory
PROJECT_DIR="/home/kurmes/StudioProjects/Oz"
RUST_DIR="$PROJECT_DIR/rust/audio_engine"
ANDROID_DIR="$PROJECT_DIR/android/app"

# Clean previous builds
rm -rf "$ANDROID_DIR/src/main/jniLibs"

# Build for each target
for target in "${TARGETS[@]}"; do
    echo "Building for $target..."
    cd "$RUST_DIR"
    cargo build --target "$target" --release
    
    # Determine ABI directory
    case $target in
        aarch64-linux-android)    abi="arm64-v8a";;
        armv7-linux-androideabi)  abi="armeabi-v7a";;
        x86_64-linux-android)     abi="x86_64";;
    esac
    
    # Create directory and copy .so
    mkdir -p "$ANDROID_DIR/src/main/jniLibs/$abi"
    cp "$RUST_DIR/target/$target/release/libozsynth.so" \
       "$ANDROID_DIR/src/main/jniLibs/$abi/"
done

echo "Build complete! jniLibs populated."
