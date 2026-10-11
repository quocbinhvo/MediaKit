package io.mediakit.engine

import android.content.Context
import io.mediakit.error.MediaKitException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

/**
 * Runs the ffmpeg executable shipped by :mediakit-ffmpeg as `lib/<abi>/libffmpeg.so`
 * in a child process (docs/adr/0001).
 */
internal class ProcessFFmpegEngine(private val context: Context) : FFmpegEngine {

    private val executable: File
        get() = File(context.applicationInfo.nativeLibraryDir, EXECUTABLE_NAME)

    override fun run(arguments: List<String>): Flow<EngineOutput> = flow {
        val binary = executable
        if (!binary.canExecute()) {
            throw MediaKitException.EngineUnavailable(
                "ffmpeg not found at ${binary.path}. Build it with native/scripts/build-android.sh, " +
                    "depend on :mediakit-ffmpeg and set packaging.jniLibs.useLegacyPackaging = true.",
            )
        }
        // Phase 1: ProcessBuilder(listOf(binary.path) + arguments), read stdout/stderr on Dispatchers.IO,
        // emit lines + Exit(waitFor()), destroy the process when the collector is cancelled.
        TODO("Phase 1: ProcessBuilder-based execution")
    }

    private companion object {
        const val EXECUTABLE_NAME = "libffmpeg.so"
    }
}
