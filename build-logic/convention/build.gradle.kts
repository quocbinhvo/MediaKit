plugins {
    `kotlin-dsl`
}

dependencies {
    // `implementation` (not compileOnly) so the precompiled script plugins can apply these by id.
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
}
