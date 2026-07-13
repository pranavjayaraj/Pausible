plugins {
    alias(libs.plugins.reset.androidLibrary)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.reset.sense.ml"
}

dependencies {
    // Model artifacts (model.json) are parsed with kotlinx-serialization; the
    // inference math itself is dependency-free Kotlin (no TFLite — see
    // AcceptanceModel). Kept `implementation` so nothing leaks to consumers.
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
