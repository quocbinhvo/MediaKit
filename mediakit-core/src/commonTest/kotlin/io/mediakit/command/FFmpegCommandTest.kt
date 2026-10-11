package io.mediakit.command

import io.mediakit.source.MediaSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class FFmpegCommandTest {

    @Test
    fun placesGlobalInputAndOutputOptionsInOrder() {
        val command = ffmpeg {
            overwrite()
            input(MediaSource.File("in.mp4")) { seek(5.seconds) }
            output("out.mp4") {
                videoCodec("libx264")
                crf(23)
            }
        }

        assertEquals(
            listOf("-y", "-ss", "5.000", "-i", "in.mp4", "-c:v", "libx264", "-crf", "23", "out.mp4"),
            command.arguments,
        )
    }

    @Test
    fun formatsDurationsWithMillisecondPrecision() {
        assertEquals("0.050", 50.milliseconds.toFFmpegTime())
        assertEquals("90.250", 90_250.milliseconds.toFFmpegTime())
    }

    @Test
    fun quotesArgumentsWithSpacesInToString() {
        assertEquals("-i \"my clip.mp4\" out.mp4", FFmpegCommand.raw("-i", "my clip.mp4", "out.mp4").toString())
    }

    @Test
    fun requiresAnOutput() {
        assertFailsWith<IllegalArgumentException> {
            ffmpeg { input(MediaSource.File("in.mp4")) }
        }
    }
}
