package io.mediakit.operations

import io.mediakit.MediaKit
import io.mediakit.command.ffmpeg
import io.mediakit.session.ExecutionResult
import io.mediakit.source.MediaSource
import kotlin.time.Duration

/**
 * High-level tasks. Each is a thin recipe over [MediaKit.execute],
 * so it behaves identically on every platform.
 */
public interface MediaOperations {
    /** Cuts [duration] starting at [start] without re-encoding (cuts snap to keyframes). */
    public suspend fun trim(input: MediaSource, outputPath: String, start: Duration, duration: Duration): ExecutionResult

    /** Writes a single frame at [position] as an image (format from [outputPath]'s extension). */
    public suspend fun thumbnail(input: MediaSource, outputPath: String, position: Duration): ExecutionResult
}

internal class DefaultMediaOperations(private val kit: MediaKit) : MediaOperations {

    override suspend fun trim(input: MediaSource, outputPath: String, start: Duration, duration: Duration): ExecutionResult =
        kit.executeAwait(
            ffmpeg {
                overwrite()
                input(input) { seek(start) }
                output(outputPath) {
                    duration(duration)
                    copyStreams()
                }
            },
        )

    override suspend fun thumbnail(input: MediaSource, outputPath: String, position: Duration): ExecutionResult =
        kit.executeAwait(
            ffmpeg {
                overwrite()
                input(input) { seek(position) }
                output(outputPath) { frames(1) }
            },
        )
}
