package io.mediakit

import io.mediakit.command.FFmpegCommand
import io.mediakit.engine.EngineOutput
import io.mediakit.engine.FFmpegEngine
import io.mediakit.error.MediaKitException
import io.mediakit.session.ExecutionEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DefaultMediaKitTest {

    private class FakeEngine(private vararg val outputs: EngineOutput) : FFmpegEngine {
        var lastArguments: List<String> = emptyList()

        override fun run(arguments: List<String>): Flow<EngineOutput> {
            lastArguments = arguments
            return flowOf(*outputs)
        }
    }

    private val command = FFmpegCommand.raw("-i", "in.mp4", "out.mp4")

    @Test
    fun mapsEngineOutputToEvents() = runTest {
        val engine = FakeEngine(
            EngineOutput.Stderr("Input #0, mov,mp4"),
            EngineOutput.Stdout("frame=10"),
            EngineOutput.Stdout("progress=end"),
            EngineOutput.Exit(0),
        )

        val events = DefaultMediaKit(engine, MediaKitConfig()).execute(command).toList()

        assertIs<ExecutionEvent.Started>(events[0])
        assertEquals(ExecutionEvent.Log("Input #0, mov,mp4"), events[1])
        assertEquals(10L, assertIs<ExecutionEvent.Progressed>(events[2]).progress.frame)
        assertIs<ExecutionEvent.Completed>(events[3])
        assertEquals(4, events.size)
    }

    @Test
    fun prependsBaseArgumentsBeforeUserArguments() = runTest {
        val engine = FakeEngine(EngineOutput.Exit(0))

        DefaultMediaKit(engine, MediaKitConfig(logLevel = LogLevel.Error)).executeAwait(command)

        assertEquals(listOf("-i", "in.mp4", "out.mp4"), engine.lastArguments.takeLast(3))
        assertTrue(engine.lastArguments.containsAll(listOf("-progress", "pipe:1", "-loglevel", "error")))
    }

    @Test
    fun nonZeroExitFailsWithLogTail() = runTest {
        val engine = FakeEngine(EngineOutput.Stderr("in.mp4: No such file or directory"), EngineOutput.Exit(1))

        val error = assertFailsWith<MediaKitException.ExecutionFailed> {
            DefaultMediaKit(engine, MediaKitConfig()).executeAwait(command)
        }

        assertEquals(1, error.exitCode)
        assertEquals("in.mp4: No such file or directory", error.logTail)
    }
}
