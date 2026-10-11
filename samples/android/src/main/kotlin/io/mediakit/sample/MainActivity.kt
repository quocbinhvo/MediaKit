package io.mediakit.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.mediakit.MediaKit
import io.mediakit.command.FFmpegCommand
import io.mediakit.sample.databinding.ActivityMainBinding
import io.mediakit.session.ExecutionEvent
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mediaKit = MediaKit(this)
        lifecycleScope.launch {
            mediaKit.execute(FFmpegCommand.raw("-version")).collect { event ->
                binding.sampleText.text = when (event) {
                    is ExecutionEvent.Started -> "Running: ffmpeg ${event.command}"
                    is ExecutionEvent.Log -> event.line
                    is ExecutionEvent.Progressed -> "Progress: ${event.progress.outTime}"
                    is ExecutionEvent.Completed -> "Done in ${event.result.elapsed}"
                    is ExecutionEvent.Failed -> "Failed: ${event.error.message}"
                }
            }
        }
    }
}
