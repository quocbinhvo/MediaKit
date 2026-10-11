// Convention for Kotlin Multiplatform library modules (e.g. :mediakit-core).
// Phase 1 targets Android only; add jvm(), js(), iosArm64()… here as phases land
// so every KMP module picks them up together.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    explicitApi()

    compilerOptions {
        // expect/actual classes (e.g. PlatformContext) are still flagged as Beta.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidLibrary {
        compileSdk = libs.versionInt("compileSdk")
        minSdk = libs.versionInt("minSdk")

        // Runs commonTest + androidHostTest on the JVM: `./gradlew testAndroidHostTest`
        withHostTestBuilder {}
    }

    sourceSets {
        commonTest.dependencies {
            implementation(libs.findLibrary("kotlin-test").get())
        }
    }
}
