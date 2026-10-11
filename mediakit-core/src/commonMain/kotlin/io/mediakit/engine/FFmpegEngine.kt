package io.mediakit.engine

import kotlinx.coroutines.flow.Flow

/**
 * Runs ffmpeg on one platform. Implementations only move bytes; parsing and
 * event mapping live in common code ([io.mediakit.DefaultMediaKit]).
 */
internal interface FFmpegEngine {
    /**
     * Starts ffmpeg with [arguments] (no leading `ffmpeg`). Emits output line by line and
     * finishes with exactly one [EngineOutput.Exit]. Cancelling collection must stop ffmpeg.
     *
     * @throws io.mediakit.error.MediaKitException.EngineUnavailable if ffmpeg can't be started.
     */
    fun run(arguments: List<String>): Flow<EngineOutput>
}

internal sealed interface EngineOutput {
    data class Stdout(val line: String) : EngineOutput
    data class Stderr(val line: String) : EngineOutput
    data class Exit(val code: Int) : EngineOutput
}
