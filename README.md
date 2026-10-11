# MediaKit

FFmpeg for Kotlin Multiplatform. Android first; JVM desktop, web (wasm), iOS and React / React Native bindings planned.

> **Status:** early. The structure and API are in place; the Android engine is the next step (see the roadmap in docs/architecture.md).

```kotlin
val kit = MediaKit(context)
kit.execute(
    ffmpeg {
        overwrite()
        input(MediaSource.File(inPath)) { seek(5.seconds) }
        output(outPath) { videoCodec("libx264"); crf(23) }
    },
).collect { event -> println(event) }
```

## Modules

| Module | Purpose |
|---|---|
| `mediakit-core` | Public KMP API: commands, progress, probe, operations |
| `mediakit-ffmpeg` | Prebuilt FFmpeg binaries for Android (built by `native/scripts/build-android.sh`) |
| `samples/android` | Demo app |
| `native/` | FFmpeg build script (see native/README.md) |
| `build-logic/` | Gradle convention plugins |

## Build

```bash
./gradlew :mediakit-core:testAndroidHostTest :samples:android:assembleDebug
```

See [docs/architecture.md](docs/architecture.md) for the design and roadmap.
