plugins {
    id("mediakit.kmp-library")
}

kotlin {
    androidLibrary {
        namespace = "io.mediakit.core"
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
