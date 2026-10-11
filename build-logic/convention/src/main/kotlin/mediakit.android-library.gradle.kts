// Convention for Android-only library modules (e.g. :mediakit-ffmpeg, which ships prebuilt binaries).
// These can't be KMP modules because the KMP Android plugin doesn't support NDK/CMake builds.
plugins {
    id("com.android.library")
}

android {
    compileSdk = libs.versionInt("compileSdk")
    ndkVersion = libs.version("ndk")

    defaultConfig {
        minSdk = libs.versionInt("minSdk")
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
