# MediaKit Architecture

**Status:** Accepted · **Last updated:** 2026-10-12

MediaKit makes FFmpeg usable from Kotlin. It starts with Android and is kept simple on purpose: two SDK artifacts, one build script, one sample app. Other platforms come later without changing the public API.

## 1. The whole picture

```
                 BUILD TIME (you, once)                        RUN TIME (user's app)

  native/ffmpeg (source, tag n7.1)                     App code
          │                                               │  MediaKit(context).execute(cmd)
          ▼                                               ▼
  native/scripts/build-android.sh  ──────┐      ┌─ mediakit-core ─────────────────────┐
   (NDK cross-compile, LGPL)             │      │  commonMain: API + DefaultMediaKit  │
          │                              │      │  androidMain: ProcessFFmpegEngine ──┼──┐
          ▼                              │      └─────────────────────────────────────┘  │ ProcessBuilder
  mediakit-ffmpeg/src/main/jniLibs/      │      ┌─ mediakit-ffmpeg (AAR) ─────────────┐  │
   <abi>/libffmpeg.so, libffprobe.so ────┘      │  lib/<abi>/libffmpeg.so  ◄──────────┼──┘
                                                └─────────────────────────────────────┘
```

- **Build time:** you cross-compile FFmpeg with the NDK and package the result into an AAR. Users of the SDK never need the NDK.
- **Run time:** `mediakit-core` starts `libffmpeg.so` as a child process and turns its output into Kotlin events ([ADR 0001](adr/0001-execution-model.md)).

## 2. Repository layout

```
MediaKit/
├── build-logic/                 # shared Gradle setup (Android/KMP config)
├── mediakit-core/               # SDK artifact 1: the Kotlin API
│   └── src/
│       ├── commonMain/          # public API + logic (no Android imports)
│       ├── commonTest/          # fast unit tests with a fake engine
│       └── androidMain/         # ProcessFFmpegEngine
├── mediakit-ffmpeg/             # SDK artifact 2: prebuilt binaries only (no code)
│   └── src/main/jniLibs/<abi>/  # filled by the build script, gitignored
├── native/
│   ├── ffmpeg/                  # FFmpeg source (gitignored, cloned at a tag by the script)
│   └── scripts/build-android.sh
├── samples/android/             # demo app = manual integration test
└── docs/
    ├── architecture.md          # this file
    └── adr/                     # why each decision was made
```

## 3. Two artifacts, one job each

| Artifact | Contains | Changes when |
|---|---|---|
| `io.mediakit:mediakit-core` | Kotlin API and engine | You change the API |
| `io.mediakit:mediakit-ffmpeg` | `.so` files only | You upgrade FFmpeg or change build flags |

See [ADR 0002](adr/0002-module-split.md).

## 4. Inside `mediakit-core`

```
commonMain/io/mediakit/
├── MediaKit.kt          # public interface + MediaKit(context) factory
├── MediaKitConfig.kt    # options (log level)
├── command/             # FFmpegCommand + ffmpeg { } DSL
├── progress/            # Progress + parser for `-progress pipe:1`
├── session/             # ExecutionEvent, ExecutionResult
├── source/              # MediaSource
├── probe/               # MediaInfo
├── operations/          # trim, thumbnail
├── error/               # MediaKitException
├── engine/              # internal FFmpegEngine interface
└── DefaultMediaKit.kt   # internal: turns engine output into events
androidMain/io/mediakit/
├── MediaKit.android.kt             # actual factory
└── engine/ProcessFFmpegEngine.kt   # internal: runs libffmpeg.so
```

The **only** platform-specific class is the engine. A new platform means one new engine; everything else is reused.

### Public API

```kotlin
interface MediaKit {
    fun execute(command: FFmpegCommand): Flow<ExecutionEvent>   // Started / Progressed / Log / Completed / Failed
    suspend fun executeAwait(command: FFmpegCommand): ExecutionResult
    suspend fun probe(source: MediaSource): MediaInfo
    val operations: MediaOperations
}
```

```kotlin
val kit = MediaKit(context)
kit.execute(
    ffmpeg {
        overwrite()
        input(MediaSource.File(inPath)) { seek(5.seconds) }
        output(outPath) { videoCodec("libx264"); crf(23) }
    },
).collect { event -> /* render progress */ }
```

### Engine contract

```kotlin
internal interface FFmpegEngine {
    fun run(arguments: List<String>): Flow<EngineOutput>   // Stdout(line) | Stderr(line) | Exit(code)
}
```

Engines only move bytes. `DefaultMediaKit` adds `-hide_banner -nostdin -nostats -loglevel <level> -progress pipe:1`, parses progress from stdout, and collects the last stderr lines for error messages. This logic is common code, unit-tested once with a fake engine.

## 5. Building FFmpeg

```bash
# In WSL or Linux, with the Linux NDK
export ANDROID_NDK_HOME=~/android-ndk-r28c
native/scripts/build-android.sh x86_64      # emulator
native/scripts/build-android.sh arm64-v8a   # real devices
```

The script clones FFmpeg at the pinned tag, cross-compiles it with the NDK's clang, and copies `ffmpeg`/`ffprobe` into `mediakit-ffmpeg/src/main/jniLibs/<abi>/` as `libffmpeg.so`/`libffprobe.so`.

| Key choice | Why |
|---|---|
| Executable renamed to `lib*.so` | Android only installs `lib*.so` into `nativeLibraryDir`, the one app folder where files may be executed |
| No `--enable-shared` | One self-contained executable, the simplest output |
| No `--enable-gpl` | The build stays LGPL |
| `-Wl,-z,max-page-size=16384` | 16 KB page alignment, required by Google Play for Android 15+ |
| `extractNativeLibs="true"` in `mediakit-ffmpeg`'s manifest | The executable must exist as a real file on disk to be started |

## 6. SDK best practices

| # | Practice | In this project |
|---|---|---|
| 1 | Users never need the NDK | Prebuilt `.so` files in `mediakit-ffmpeg` |
| 2 | Small public API, everything else `internal` | `explicitApi()` in `mediakit.kmp-library` |
| 3 | Track API changes | Kotlin ABI validation: a committed `.api` file checked in CI *(to add)* |
| 4 | No hidden initialization | Users call `MediaKit(context)` explicitly |
| 5 | Coroutines for async | `Flow<ExecutionEvent>` + `suspend fun`; cancelling the collector cancels the job |
| 6 | Typed errors | `sealed class MediaKitException` |
| 7 | Ship R8/ProGuard rules | `consumer-rules.pro` in each Android module |
| 8 | Pin native versions | `FFMPEG_TAG` in the script, `ndk` in `libs.versions.toml` ([ADR 0003](adr/0003-ffmpeg-version-pinning.md)) |
| 9 | LGPL by default, documented | No `--enable-gpl` |
| 10 | Semantic versioning + CHANGELOG | `0.x` while learning, `1.0` once the API is stable |
| 11 | Tests at two levels | `commonTest` with a fake engine; the sample app on a device |
| 12 | KDoc on every public item | Dokka-generated reference *(later)* |

## 7. Roadmap

| Step | Goal | You learn |
|---|---|---|
| 1 | Build FFmpeg for **x86_64** in WSL | NDK cross-compiling |
| 2 | Sample app prints `ffmpeg -version` | Packaging `.so` files, `nativeLibraryDir`, `ProcessBuilder` |
| 3 | A real job with progress and cancellation | Coroutines, `callbackFlow`, process lifecycle |
| 4 | `probe()` via ffprobe JSON + `content://` inputs | Android storage, parsing |
| 5 | Add **arm64**, then `publishToMavenLocal` | Multi-ABI builds, Maven publishing |
| 6 | Maven Central + CI | Signing, release automation |
| Later | Desktop → web → iOS → React Native | Only once Android feels solid |

Things deliberately left out until a step needs them: JNI/CMake code, GPL or "full" builds, a BoM, and bindings for JavaScript/React.
