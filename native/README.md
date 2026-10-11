# native/

Builds FFmpeg for Android. See [docs/architecture.md](../docs/architecture.md) §5.

| Path | Contents |
|---|---|
| `scripts/build-android.sh` | Clones FFmpeg at the pinned tag, cross-compiles it with the NDK, copies the result into `:mediakit-ffmpeg` |
| `ffmpeg/` | FFmpeg source, created by the script (gitignored) |
| `out/` | Build output: `out/android/<abi>/{bin,lib,include}` (gitignored) |

## Usage

FFmpeg's `configure` needs Linux, macOS or **WSL on Windows**. Inside WSL, install the **Linux** NDK; the Windows NDK won't work there.

```bash
sudo apt install build-essential git pkg-config
export ANDROID_NDK_HOME=~/android-ndk-r28c

native/scripts/build-android.sh x86_64      # emulator: build this first
native/scripts/build-android.sh arm64-v8a   # real devices
```

Result:

```
mediakit-ffmpeg/src/main/jniLibs/
├── x86_64/     libffmpeg.so  libffprobe.so
└── arm64-v8a/  libffmpeg.so  libffprobe.so
```

The files are executables renamed to `lib*.so`, because Android only installs `lib*.so` files into `nativeLibraryDir`, the one app folder where files may be executed.
