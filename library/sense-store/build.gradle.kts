plugins {
    alias(libs.plugins.reset.androidLibrary)
    alias(libs.plugins.reset.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.reset.sense.store"
}

// Version-controlled schema history — required for real Migrations later.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Decision/outcome types (Decision, PromptAction, ResponseHistory, …).
    api(project(":sense-ml"))

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
