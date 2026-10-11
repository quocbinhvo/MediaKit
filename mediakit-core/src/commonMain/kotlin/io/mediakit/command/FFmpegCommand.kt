package io.mediakit.command

import io.mediakit.source.MediaSource
import kotlin.time.Duration

/** An immutable ffmpeg argument list (without the leading `ffmpeg`). */
public class FFmpegCommand internal constructor(public val arguments: List<String>) {

    /** Shell-like rendering, for logs only — arguments are never passed through a shell. */
    override fun toString(): String = arguments.joinToString(" ") { arg ->
        val needsQuotes = arg.isEmpty() || arg.any { it.isWhitespace() || it == '"' }
        if (needsQuotes) "\"" + arg.replace("\"", "\\\"") + "\"" else arg
    }

    override fun equals(other: Any?): Boolean = other is FFmpegCommand && other.arguments == arguments
    override fun hashCode(): Int = arguments.hashCode()

    public companion object {
        /** Escape hatch: pass arguments exactly as you would on the command line. */
        public fun raw(vararg arguments: String): FFmpegCommand = FFmpegCommand(arguments.toList())

        public fun raw(arguments: List<String>): FFmpegCommand = FFmpegCommand(arguments.toList())
    }
}

@DslMarker
internal annotation class FFmpegDsl

/**
 * Builds an [FFmpegCommand]:
 * ```
 * ffmpeg {
 *     overwrite()
 *     input(MediaSource.File(inPath)) { seek(5.seconds) }
 *     output(outPath) { videoCodec("libx264"); crf(23) }
 * }
 * ```
 */
public fun ffmpeg(block: FFmpegCommandBuilder.() -> Unit): FFmpegCommand =
    FFmpegCommandBuilder().apply(block).build()

@FFmpegDsl
public class FFmpegCommandBuilder internal constructor() {
    private val global = mutableListOf<String>()
    private val inputs = mutableListOf<String>()
    private val outputs = mutableListOf<String>()

    /** `-y`: overwrite output files without asking. */
    public fun overwrite() {
        global += "-y"
    }

    /** Adds global options, placed before all inputs. */
    public fun globalArgs(vararg args: String) {
        global += args
    }

    public fun input(source: MediaSource, block: InputBuilder.() -> Unit = {}) {
        inputs += InputBuilder().apply(block).args
        inputs += listOf("-i", source.ffmpegInput)
    }

    public fun output(path: String, block: OutputBuilder.() -> Unit = {}) {
        outputs += OutputBuilder().apply(block).args
        outputs += path
    }

    internal fun build(): FFmpegCommand {
        require(outputs.isNotEmpty()) { "An ffmpeg command needs at least one output" }
        return FFmpegCommand(global + inputs + outputs)
    }
}

/** Options that apply to the next input (they precede its `-i`). */
@FFmpegDsl
public class InputBuilder internal constructor() {
    internal val args = mutableListOf<String>()

    public fun seek(position: Duration) { args += listOf("-ss", position.toFFmpegTime()) }
    public fun duration(duration: Duration) { args += listOf("-t", duration.toFFmpegTime()) }
    public fun format(format: String) { args += listOf("-f", format) }
    public fun args(vararg args: String) { this.args += args }
}

/** Options that apply to the next output (they precede its path). */
@FFmpegDsl
public class OutputBuilder internal constructor() {
    internal val args = mutableListOf<String>()

    public fun videoCodec(codec: String) { args += listOf("-c:v", codec) }
    public fun audioCodec(codec: String) { args += listOf("-c:a", codec) }
    /** `-c copy`: remux without re-encoding. */
    public fun copyStreams() { args += listOf("-c", "copy") }
    public fun crf(value: Int) { args += listOf("-crf", value.toString()) }
    public fun duration(duration: Duration) { args += listOf("-t", duration.toFFmpegTime()) }
    public fun frames(count: Int) { args += listOf("-frames:v", count.toString()) }
    public fun noVideo() { args += "-vn" }
    public fun noAudio() { args += "-an" }
    public fun format(format: String) { args += listOf("-f", format) }
    public fun args(vararg args: String) { this.args += args }
}

/** Seconds with millisecond precision, e.g. `5.250` — accepted by every ffmpeg time option. */
internal fun Duration.toFFmpegTime(): String {
    val ms = inWholeMilliseconds
    return "${ms / 1000}.${(ms % 1000).toString().padStart(3, '0')}"
}
