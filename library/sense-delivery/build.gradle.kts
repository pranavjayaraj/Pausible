plugins {
    alias(libs.plugins.reset.androidLibrary)
    alias(libs.plugins.reset.hilt)
}

android {
    namespace = "com.reset.sense.delivery"
}

dependencies {
    api(project(":sense-ml"))
    implementation(project(":sense-signals"))
    implementation(project(":sense-store"))

    implementation(libs.androidx.work.runtime)
    implementation(libs.coroutines.android)
    // Host-foreground gate: never notify someone who is already in the app.
    implementation(libs.androidx.lifecycle.process)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
