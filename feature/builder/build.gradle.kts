plugins {
    alias(libs.plugins.reset.androidFeatureCompose)
}

android {
    namespace = "com.reset.feature.builder"
}

dependencies {
    implementation(project(":model"))
    implementation(project(":navigation"))
    implementation(project(":feature:builder:api"))
    // Cross-feature navigation targets only the destination contract, never the feature impl.
    implementation(project(":feature:sessions:api"))
    implementation(libs.coroutines.core)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.compose.activity)

    testImplementation(libs.junit)
    testImplementation(libs.orbit.test)
    testImplementation(libs.coroutines.test)
}
