package io.mediakit.session

import io.mediakit.command.FFmpegCommand
import io.mediakit.error.MediaKitException
import io.mediakit.progress.Progress
import kotlin.time.Duration

/** Lifecycle of one ffmpeg job. Every flow ends with exactly one [Completed] or [Failed]. */
public sealed interface ExecutionEvent {
    public data class Started(val command: FFmpegCommand) : ExecutionEvent
    public data class Progressed(val progress: Progress) : ExecutionEvent
    public data class Log(val line: String) : ExecutionEvent
    public data class Completed(val result: ExecutionResult) : ExecutionEvent
    public data class Failed(val error: MediaKitException) : ExecutionEvent
}

public data class ExecutionResult(
    /** Wall-clock time the job took. */
    val elapsed: Duration,
)
