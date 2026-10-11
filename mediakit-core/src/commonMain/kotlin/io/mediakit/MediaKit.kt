package io.mediakit

import io.mediakit.command.FFmpegCommand
import io.mediakit.operations.MediaOperations
import io.mediakit.probe.MediaInfo
import io.mediakit.session.ExecutionEvent
import io.mediakit.session.ExecutionResult
import io.mediakit.source.MediaSource
import kotlinx.coroutines.flow.Flow

/**
 * Entry point of MediaKit.
 *
 * The API is platform-independent; each platform supplies its own engine
 * (Android: the ffmpeg executable in a child process — see docs/adr/0001).
 */
public interface MediaKit {

    /**
     * Runs [command] and streams its lifecycle. The flow is cold: collecting starts the job,
     * cancelling the collector cancels the job.
     */
    public fun execute(command: FFmpegCommand): Flow<ExecutionEvent>

    /**
     * Runs [command] to completion.
     *
     * @throws io.mediakit.error.MediaKitException if the job fails.
     */
    public suspend fun executeAwait(command: FFmpegCommand): ExecutionResult

    /** Reads container and stream metadata of [source]. */
    public suspend fun probe(source: MediaSource): MediaInfo

    /** Common tasks (trim, thumbnail, …) built on top of [execute]. */
    public val operations: MediaOperations
}

/** Platform handle needed to locate binaries and resolve sources. `android.content.Context` on Android. */
public expect abstract class PlatformContext

/** Creates a [MediaKit] backed by the current platform's engine. */
public expect fun MediaKit(context: PlatformContext, config: MediaKitConfig = MediaKitConfig()): MediaKit
