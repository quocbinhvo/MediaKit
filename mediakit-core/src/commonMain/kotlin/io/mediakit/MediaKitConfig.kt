package io.mediakit

public data class MediaKitConfig(
    /** Passed to ffmpeg as `-loglevel`; controls how many [io.mediakit.session.ExecutionEvent.Log] events you get. */
    val logLevel: LogLevel = LogLevel.Info,
)

public enum class LogLevel(internal val ffmpegName: String) {
    Quiet("quiet"),
    Error("error"),
    Warning("warning"),
    Info("info"),
    Verbose("verbose"),
    Debug("debug"),
}
