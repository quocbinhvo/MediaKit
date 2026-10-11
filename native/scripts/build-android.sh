#!/usr/bin/env bash
# Builds the ffmpeg + ffprobe executables for one Android ABI and copies them
# into :mediakit-ffmpeg as lib<name>.so (see docs/adr/0001-execution-model.md).
#
# Requirements: Linux or WSL (FFmpeg's configure doesn't run on plain Windows),
#               build-essential, git, pkg-config, and the *Linux* Android NDK.
#
# Usage:
#   export ANDROID_NDK_HOME=~/android-ndk-r28c
#   native/scripts/build-android.sh x86_64      # emulator
#   native/scripts/build-android.sh arm64-v8a   # real devices
set -euo pipefail

# --- Pinned versions (docs/adr/0003). Change deliberately. ---------------------
FFMPEG_TAG=n7.1
API=24 # must match minSdk in gradle/libs.versions.toml

# --- Inputs --------------------------------------------------------------------
ABI=${1:-arm64-v8a}
NDK=${ANDROID_NDK_HOME:?Set ANDROID_NDK_HOME to the Linux NDK (r28+)}
TOOLCHAIN=$NDK/toolchains/llvm/prebuilt/linux-x86_64

case $ABI in
  arm64-v8a) TARGET=aarch64-linux-android; ARCH=aarch64; EXTRA_FLAGS="" ;;
  # x86 assembly needs nasm and has PIC issues on Android; the emulator is fast enough without it.
  x86_64)    TARGET=x86_64-linux-android;  ARCH=x86_64;  EXTRA_FLAGS="--disable-asm" ;;
  *) echo "Unsupported ABI: $ABI (use arm64-v8a or x86_64)" >&2; exit 1 ;;
esac

NATIVE_DIR=$(cd "$(dirname "$0")/.." && pwd)
SRC=$NATIVE_DIR/ffmpeg
OUT=$NATIVE_DIR/out/android/$ABI
JNILIBS=$NATIVE_DIR/../mediakit-ffmpeg/src/main/jniLibs/$ABI

# --- 1. Get the source (once) --------------------------------------------------
if [ ! -d "$SRC" ]; then
  git clone --depth 1 --branch "$FFMPEG_TAG" https://git.ffmpeg.org/ffmpeg.git "$SRC"
fi

# --- 2. Configure: cross-compile for Android with the NDK's clang --------------
cd "$SRC"
make distclean >/dev/null 2>&1 || true

# No --enable-gpl            → the build stays LGPL.
# No --enable-shared         → one self-contained executable (libav* linked in statically).
# max-page-size=16384        → 16 KB page alignment, required by Google Play for Android 15+.
./configure \
  --prefix="$OUT" \
  --enable-cross-compile \
  --target-os=android \
  --arch="$ARCH" \
  --sysroot="$TOOLCHAIN/sysroot" \
  --cc="$TOOLCHAIN/bin/${TARGET}${API}-clang" \
  --cxx="$TOOLCHAIN/bin/${TARGET}${API}-clang++" \
  --nm="$TOOLCHAIN/bin/llvm-nm" \
  --ar="$TOOLCHAIN/bin/llvm-ar" \
  --ranlib="$TOOLCHAIN/bin/llvm-ranlib" \
  --strip="$TOOLCHAIN/bin/llvm-strip" \
  --extra-ldflags="-Wl,-z,max-page-size=16384" \
  --enable-pic \
  --disable-doc \
  --disable-ffplay \
  --disable-debug \
  --disable-vulkan \
  $EXTRA_FLAGS

# --- 3. Compile ----------------------------------------------------------------
make -j"$(nproc)"
make install

# --- 4. Package: Android only installs lib*.so files into nativeLibraryDir -----
mkdir -p "$JNILIBS"
cp "$OUT/bin/ffmpeg"  "$JNILIBS/libffmpeg.so"
cp "$OUT/bin/ffprobe" "$JNILIBS/libffprobe.so"

echo "Done: $JNILIBS"
ls -lh "$JNILIBS"
