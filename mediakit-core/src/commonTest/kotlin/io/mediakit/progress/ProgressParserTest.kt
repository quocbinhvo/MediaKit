package io.mediakit.progress

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ProgressParserTest {

    // Shape of real `ffmpeg -progress pipe:1` output.
    private val block = """
        frame=120
        fps=59.94
        stream_0_0_q=28.0
        bitrate=1534.2kbits/s
        total_size=1048576
        out_time_us=2000000
        out_time_ms=2000000
        out_time=00:00:02.000000
        dup_frames=0
        drop_frames=0
        speed=1.98x
    """.trimIndent().lines()

    @Test
    fun emitsProgressOnlyAtEndOfBlock() {
        val parser = ProgressParser()
        block.forEach { assertNull(parser.feed(it)) }

        val progress = parser.feed("progress=continue")!!

        assertEquals(120L, progress.frame)
        assertEquals(59.94, progress.fps)
        assertEquals(2.seconds, progress.outTime)
        assertEquals(1534.2, progress.bitrateKbps)
        assertEquals(1_048_576L, progress.totalSizeBytes)
        assertEquals(1.98, progress.speed)
        assertEquals(false, progress.isFinished)
        assertEquals(0.5f, progress.fractionOf(4.seconds))
    }

    @Test
    fun treatsNotAvailableAsNullAndMarksEnd() {
        val parser = ProgressParser()
        parser.feed("bitrate=N/A")
        parser.feed("speed=N/A")

        val progress = parser.feed("progress=end")!!

        assertNull(progress.bitrateKbps)
        assertNull(progress.speed)
        assertTrue(progress.isFinished)
    }

    @Test
    fun ignoresLinesThatAreNotKeyValue() {
        assertNull(ProgressParser().feed("garbage"))
    }
}
