# ADR 0002: Two artifacts: API and binaries

**Status:** Accepted (revised 2026-10-12: simplified from a three-module plan)
**Date:** 2026-10-11

## Context

The Kotlin API changes often. The FFmpeg binaries change rarely, take a long time to build, are large, and carry a license (LGPL here). Mixing them in one module couples two very different release rhythms.

## Options

### A. One module with API and binaries
Simple, but every API change ships a new copy of the binaries, and the binaries can't be swapped independently.

### B. Two modules
- `:mediakit-core`: KMP, the public API and engines. No binaries, no C/C++.
- `:mediakit-ffmpeg`: Android library containing only the prebuilt `.so` files.

## Decision

**B.** Both modules apply the convention plugins in `build-logic/` (`mediakit.kmp-library`, `mediakit.android-library`) rather than configuring AGP/Kotlin themselves.

## Consequences

- Users add two dependencies: `mediakit-core` and `mediakit-ffmpeg`.
- `mediakit-core` must report a clear error (`MediaKitException.EngineUnavailable`) when the binaries are missing.
- Later additions (a GPL binaries artifact, a JNI module, a BoM) are new modules next to these two. Neither existing module changes shape.
