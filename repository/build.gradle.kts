plugins {
    alias(libs.plugins.reset.androidLibrary)
    alias(libs.plugins.reset.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.reset.data"
}

dependencies {
    implementation(project(":model"))
    implementation(libs.coroutines.core)
    // Builder presets are persisted as a JSON blob inside DataStore preferences.
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
