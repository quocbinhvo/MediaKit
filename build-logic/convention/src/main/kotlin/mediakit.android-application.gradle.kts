// Convention for sample apps.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = libs.versionInt("compileSdk")

    defaultConfig {
        minSdk = libs.versionInt("minSdk")
        targetSdk = libs.versionInt("compileSdk")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        // The ffmpeg executable ships as lib/<abi>/libffmpeg.so and must be extracted
        // to nativeLibraryDir so it can be exec'd (docs/adr/0001).
        jniLibs.useLegacyPackaging = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}
