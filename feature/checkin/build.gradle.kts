plugins {
    alias(libs.plugins.reset.androidFeatureCompose)
}

android {
    namespace = "com.reset.feature.checkin"
}

dependencies {
    implementation(project(":model"))
    implementation(project(":navigation"))
    implementation(project(":feature:checkin:api"))
    // Cross-feature navigation targets only the destination + catalog-id contracts, never
    // the sessions feature's impl (the selector's own result is already plain scriptIds).
    implementation(project(":feature:sessions:api"))
    implementation(libs.coroutines.core)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.compose.activity)

    testImplementation(libs.junit)
    testImplementation(libs.orbit.test)
    testImplementation(libs.coroutines.test)
}
