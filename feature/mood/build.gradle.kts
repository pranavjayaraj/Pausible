plugins {
    alias(libs.plugins.reset.androidFeatureCompose)
}

android {
    namespace = "com.reset.feature.mood"
}

dependencies {
    implementation(project(":model"))
    implementation(project(":navigation"))
    implementation(project(":feature:mood:api"))
    implementation(libs.coroutines.core)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.compose.activity)

    testImplementation(libs.junit)
    testImplementation(libs.orbit.test)
    testImplementation(libs.coroutines.test)
}
