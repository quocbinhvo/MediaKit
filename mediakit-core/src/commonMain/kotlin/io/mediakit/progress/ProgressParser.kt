package io.mediakit.progress

import kotlin.time.Duration.Companion.microseconds

/**
 * Parses the `key=value` lines ffmpeg writes with `-progress pipe:1`.
 * Each block ends with `progress=continue` or `progress=end`; [feed] returns a [Progress] then.
 * Not thread-safe: use one parser per job.
 */
internal class ProgressParser {
    private val block = mutableMapOf<String, String>()

    fun feed(line: String): Progress? {
        val separator = line.indexOf('=')
        if (separator <= 0) return null
        val key = line.substring(0, separator).trim()
        val value = line.substring(separator + 1).trim()
        if (key != "progress") {
            block[key] = value
            return null
        }
        val progress = Progress(
            frame = field("frame")?.toLongOrNull(),
            fps = field("fps")?.toDoubleOrNull(),
            // out_time_ms is also in microseconds (long-standing ffmpeg quirk); prefer out_time_us.
            outTime = (field("out_time_us") ?: field("out_time_ms"))?.toLongOrNull()?.microseconds,
            bitrateKbps = field("bitrate")?.removeSuffix("kbits/s")?.toDoubleOrNull(),
            totalSizeBytes = field("total_size")?.toLongOrNull(),
            speed = field("speed")?.removeSuffix("x")?.toDoubleOrNull(),
            isFinished = value == "end",
        )
        block.clear()
        return progress
    }

    private fun field(key: String): String? = block[key]?.takeUnless { it == "N/A" }
}
