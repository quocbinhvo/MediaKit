package io.mediakit.progress

import kotlin.time.Duration

/** One snapshot of ffmpeg's `-progress` output. Fields are null when ffmpeg reports `N/A`. */
public data class Progress(
    val frame: Long?,
    val fps: Double?,
    /** Position written to the output so far. */
    val outTime: Duration?,
    val bitrateKbps: Double?,
    val totalSizeBytes: Long?,
    /** Processing speed relative to real time (2.0 = twice as fast). */
    val speed: Double?,
    /** True on the last snapshot of a job. */
    val isFinished: Boolean,
) {
    /** Completion in 0..1, given the expected output length; null if unknown. */
    public fun fractionOf(total: Duration): Float? {
        val out = outTime ?: return null
        if (!total.isPositive()) return null
        return (out / total).toFloat().coerceIn(0f, 1f)
    }
}
