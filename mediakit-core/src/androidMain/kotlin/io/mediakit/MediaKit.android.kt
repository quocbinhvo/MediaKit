package io.mediakit

import io.mediakit.engine.ProcessFFmpegEngine

public actual typealias PlatformContext = android.content.Context

public actual fun MediaKit(context: PlatformContext, config: MediaKitConfig): MediaKit =
    DefaultMediaKit(ProcessFFmpegEngine(context.applicationContext), config)
