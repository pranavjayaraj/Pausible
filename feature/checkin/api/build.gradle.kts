plugins {
    // Bare id, not the catalog alias: this module is nested under :feature:checkin, whose own
    // convention plugins are already on the parent classpath with version "unspecified" —
    // a versioned request from a child cannot pass Gradle's compatibility check.
    id("reset.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.reset.feature.checkin.api"
}

dependencies {
    // `api` on purpose: the destination implements Screen and is @Serializable, so both
    // :navigation and the serialization runtime (transitively via :navigation) are part of
    // this module's public surface.
    api(project(":navigation"))
}
