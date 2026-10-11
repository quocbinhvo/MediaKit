# ADR 0001: Run FFmpeg as a child process on Android

**Status:** Accepted
**Date:** 2026-10-11

## Context

MediaKit must run arbitrary FFmpeg commands on Android, report progress and support cancellation. The same public API must later work on iOS, JVM desktop and the browser.

## Options

### A. Executable in a child process
Build the `ffmpeg` CLI, ship it as `lib/<abi>/libffmpeg.so` (so the package manager installs it into `nativeLibraryDir`, which may be executed), and run it with `ProcessBuilder`.

- ✅ Crash isolation: an FFmpeg segfault ends the child, not the app.
- ✅ Cancellation is `Process.destroy()`; no cooperative cancellation hooks in C.
- ✅ Unmodified FFmpeg: no patches to `fftools`, which use global state and are not re-entrant.
- ✅ Concurrent jobs are naturally isolated.
- ❌ Process start costs a few ms per job.
- ❌ Not possible on iOS (no `fork`/`exec`) or in the browser.

### B. In-process `fftools` via JNI (the ffmpeg-kit approach)
- ✅ Works the same way on iOS.
- ❌ Requires maintaining patches to `fftools` for re-entrancy, `exit()` handling and cancellation, re-applied on every FFmpeg upgrade.
- ❌ A crash in FFmpeg kills the host app.

### C. Direct `libav*` API via JNI
- ✅ Fastest for narrow tasks (probe, thumbnail, frame extraction).
- ❌ Every feature is hand-written C; doesn't cover "run any ffmpeg command".

## Decision

Use **A** on Android (and later JVM desktop). Hide it behind the internal `FFmpegEngine` interface, so iOS can use **B** and the browser an Emscripten build without changing the public API. Add **C** later, in a separate JNI module, only for hot paths.

## Consequences

- Apps must keep native libraries extracted: `:mediakit-ffmpeg` declares `android:extractNativeLibs="true"`; apps can also set `packaging.jniLibs.useLegacyPackaging = true`.
- Progress is read from `-progress pipe:1` on stdout; logs from stderr.
- The engine interface stays byte-level (`Stdout` / `Stderr` / `Exit`); all parsing is common code.
