package io.mediakit.error

public sealed class MediaKitException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {

    /** ffmpeg ran but exited with a non-zero code. [logTail] holds its last stderr lines. */
    public class ExecutionFailed(public val exitCode: Int, public val logTail: String) :
        MediaKitException("ffmpeg exited with code $exitCode\n$logTail")

    /** The ffmpeg binary is missing or can't be started on this device. */
    public class EngineUnavailable(message: String, cause: Throwable? = null) : MediaKitException(message, cause)

    /** The input can't be opened or isn't supported. */
    public class InvalidSource(message: String, cause: Throwable? = null) : MediaKitException(message, cause)
}
