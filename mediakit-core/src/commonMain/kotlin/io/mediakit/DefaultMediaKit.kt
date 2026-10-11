package io.mediakit

import io.mediakit.command.FFmpegCommand
import io.mediakit.engine.EngineOutput
import io.mediakit.engine.FFmpegEngine
import io.mediakit.error.MediaKitException
import io.mediakit.operations.DefaultMediaOperations
import io.mediakit.operations.MediaOperations
import io.mediakit.probe.MediaInfo
import io.mediakit.progress.ProgressParser
import io.mediakit.session.ExecutionEvent
import io.mediakit.session.ExecutionResult
import io.mediakit.source.MediaSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlin.time.TimeSource

/** Platform-independent orchestration on top of an [FFmpegEngine]. */
internal class DefaultMediaKit(
    private val engine: FFmpegEngine,
    private val config: MediaKitConfig,
) : MediaKit {

    override val operations: MediaOperations = DefaultMediaOperations(this)

    override fun execute(command: FFmpegCommand): Flow<ExecutionEvent> = flow {
        emit(ExecutionEvent.Started(command))
        val parser = ProgressParser()
        val logTail = ArrayDeque<String>(LOG_TAIL_LINES)
        val startedAt = TimeSource.Monotonic.markNow()

        engine.run(baseArguments() + command.arguments).collect { output ->
            when (output) {
                is EngineOutput.Stdout -> parser.feed(output.line)?.let { emit(ExecutionEvent.Progressed(it)) }
                is EngineOutput.Stderr -> {
                    if (logTail.size == LOG_TAIL_LINES) logTail.removeFirst()
                    logTail.addLast(output.line)
                    emit(ExecutionEvent.Log(output.line))
                }
                is EngineOutput.Exit -> emit(
                    if (output.code == 0) {
                        ExecutionEvent.Completed(ExecutionResult(elapsed = startedAt.elapsedNow()))
                    } else {
                        ExecutionEvent.Failed(MediaKitException.ExecutionFailed(output.code, logTail.joinToString("\n")))
                    },
                )
            }
        }
    }.catch { error ->
        if (error is MediaKitException) emit(ExecutionEvent.Failed(error)) else throw error
    }

    override suspend fun executeAwait(command: FFmpegCommand): ExecutionResult =
        when (val end = execute(command).first { it is ExecutionEvent.Completed || it is ExecutionEvent.Failed }) {
            is ExecutionEvent.Completed -> end.result
            is ExecutionEvent.Failed -> throw end.error
            else -> error("unreachable")
        }

    override suspend fun probe(source: MediaSource): MediaInfo =
        TODO("Phase 1: run ffprobe -print_format json and parse into MediaInfo")

    /** Options every job gets: machine-readable progress on stdout, logs on stderr, never read stdin. */
    private fun baseArguments(): List<String> = listOf(
        "-hide_banner",
        "-nostdin",
        "-nostats",
        "-loglevel", config.logLevel.ffmpegName,
        "-progress", "pipe:1",
    )

    private companion object {
        const val LOG_TAIL_LINES = 20
    }
}
