# ADR 0003: Pin the FFmpeg tag, NDK and configure flags

**Status:** Accepted
**Date:** 2026-10-11

## Context

The binaries' behavior depends on the FFmpeg version, the NDK that compiled them and the `configure` flags. If any of them drift, codecs, output and binary size change silently for every user.

## Decision

- `native/scripts/build-android.sh` clones FFmpeg at a release tag (`FFMPEG_TAG`, e.g. `n7.1`), never a branch.
- The NDK version is pinned in `gradle/libs.versions.toml` (`ndk`). Use the same NDK release when running the script.
- The `configure` flags live in the script and are reviewed like code.
- Changing any of the three is a deliberate change: note it in the changelog and bump the `mediakit-ffmpeg` version.

## Consequences

- Rebuilding the binaries gives the same result.
- Security fixes in FFmpeg require an explicit tag bump; watch FFmpeg release announcements.
