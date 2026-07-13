plugins {
    alias(libs.plugins.reset.androidLibrary)
    alias(libs.plugins.reset.hilt)
}

android {
    namespace = "com.reset.sense.signals"
}

dependencies {
    // Pure-data contracts (UsageSnapshot, DeviceSnapshot, AppCategory, …).
    api(project(":sense-ml"))

    // Activity Recognition Transition API (physical context: still/on-foot/vehicle).
    implementation(libs.play.services.location)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
}
