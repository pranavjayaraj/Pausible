plugins {
    alias(libs.plugins.reset.androidLibrary)
    alias(libs.plugins.reset.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.reset.data"
}

// Version-controlled schema history — required for real Migrations later.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":model"))
    implementation(project(":sense-store"))
    implementation(libs.coroutines.core)
    // Builder presets are persisted as a JSON blob inside DataStore preferences.
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime)
    // The check-in propensity trail's own Room database — separate from sense_store.db,
    // keeping the sense-* modules untouched.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
