plugins {
    alias(libs.plugins.reset.androidLibrary)
    alias(libs.plugins.reset.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.reset.model"
}

dependencies {
    implementation(libs.coroutines.core)
    // SessionPreset is @Serializable so the data layer can persist builder presets as JSON.
    api(libs.kotlinx.serialization.core)

    testImplementation(libs.junit)
}
