plugins {
    alias(libs.plugins.reset.androidLibrary)
    alias(libs.plugins.reset.hilt)
}

android {
    namespace = "com.reset.textembed"
}

dependencies {
    // TextEmbedder / EmbedModelManager — the shared contract this implements, so this
    // library never depends on any :feature module (see TextEmbedder's own doc).
    implementation(project(":model"))

    // The native .so runtime ships in the base APK unconditionally (a few MB) — the ~5.8MB
    // model itself is deferred via Play Asset Delivery (:model_pack), read from the
    // downloaded file path at runtime, never from APK assets.
    implementation(libs.mediapipe.tasksText)
    implementation(libs.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)

    // Instrumented smoke test only: verifies the real checked-in model file produces sane
    // embeddings on-device. The model is copied into src/androidTest/assets for this test
    // only — production code never reads a model from APK assets (see class doc above).
    androidTestImplementation(libs.androidx.test.junitExt)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.coroutines.test)
}
