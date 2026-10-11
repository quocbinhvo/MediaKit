package io.mediakit.probe

import kotlin.time.Duration

public data class MediaInfo(
    val formatName: String,
    val duration: Duration?,
    val bitrate: Long?,
    val streams: List<StreamInfo>,
)

public data class StreamInfo(
    val index: Int,
    val type: StreamType,
    val codec: String,
    val width: Int? = null,
    val height: Int? = null,
    val frameRate: Double? = null,
    val sampleRate: Int? = null,
    val channels: Int? = null,
)

public enum class StreamType { Video, Audio, Subtitle, Data, Unknown }
