plugins {
    // Bare id, not the catalog alias: this module is nested under :feature:mood, whose own
    // convention plugins are already on the parent classpath with version "unspecified" —
    // a versioned request from a child cannot pass Gradle's compatibility check.
    id("reset.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.reset.feature.mood.api"
}

dependencies {
    // `api` on purpose: the destinations implement Screen and are @Serializable, so both
    // :navigation and the serialization runtime (transitively via :navigation) are part of
    // this module's public surface.
    api(project(":navigation"))
}
