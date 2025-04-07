# android/app/src/main/cpp/CMakeLists.txt
cmake_minimum_required(VERSION 3.10)
project(OzSynth)

# Android NDK için platform ve mimari bilgileri alalım
if(NOT DEFINED ANDROID_ABI)
    message(FATAL_ERROR "ANDROID_ABI is not defined")
endif()

# Cargo çıkış dizinini belirle (geçici derleme dosyaları burada toplanacak)
set(CARGO_TARGET_DIR ${CMAKE_BINARY_DIR}/cargo)

# Android ABI'yi Rust hedef triple'ına çevir
# arm64-v8a -> aarch64-linux-android, armeabi-v7a -> armv7-linux-androideabi, x86 -> i686-linux-android, x86_64 -> x86_64-linux-android
if(${ANDROID_ABI} STREQUAL "arm64-v8a")
    set(RUST_TARGET "aarch64-linux-android")
elseif(${ANDROID_ABI} STREQUAL "armeabi-v7a")
    set(RUST_TARGET "armv7-linux-androideabi")
elseif(${ANDROID_ABI} STREQUAL "x86")
    set(RUST_TARGET "i686-linux-android")
elseif(${ANDROID_ABI} STREQUAL "x86_64")
    set(RUST_TARGET "x86_64-linux-android")
else()
    message(FATAL_ERROR "Unsupported ANDROID_ABI: ${ANDROID_ABI}")
endif()

# Rust kütüphanesini Cargo ile derle
add_custom_command(
    OUTPUT ${CARGO_TARGET_DIR}/${RUST_TARGET}/release/libozsynth.so
    COMMAND cargo build --target ${RUST_TARGET} --release
    WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}/../../../../rust/audio_engine
    COMMENT "Building Rust audio_engine library (${RUST_TARGET})"
)

# Derlenen .so kütüphanesini projeye dahil et
add_library(ozsynth SHARED IMPORTED)
set_target_properties(ozsynth PROPERTIES IMPORTED_LOCATION
    ${CARGO_TARGET_DIR}/${RUST_TARGET}/release/libozsynth.so)

# Üstteki custom command'i tetiklemek için bir custom target oluştur
add_custom_target(build_ozsynth ALL DEPENDS ${CARGO_TARGET_DIR}/${RUST_TARGET}/release/libozsynth.so)
add_dependencies(ozsynth build_ozsynth)

# Kotlin/Java tarafının kullanabileceği şekilde kütüphaneyi link et (genelde gerekli olmayabilir)
# target_link_libraries(ozsynth PUBLIC ...)
