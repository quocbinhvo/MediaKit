package io.mediakit.source

/**
 * Where media is read from. Platform-specific inputs (Android `content://` Uris,
 * browser Blobs) get their own factory functions in the platform source sets.
 */
public sealed interface MediaSource {
    /** The string ffmpeg receives after `-i`. */
    public val ffmpegInput: String

    public data class File(val path: String) : MediaSource {
        override val ffmpegInput: String get() = path
    }

    /** Anything ffmpeg's protocols understand: `https://…`, `rtmp://…`, … */
    public data class Url(val url: String) : MediaSource {
        override val ffmpegInput: String get() = url
    }
}
