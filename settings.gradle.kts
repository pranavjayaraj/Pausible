rootProject.name = "reset-app"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

include(":app")
include(":core")
include(":navigation")
include(":feature:builder")
include(":feature:builder:api")
include(":feature:home")
include(":feature:home:api")
include(":feature:sessions")
include(":feature:sessions:api")
include(":feature:checkin")
include(":feature:checkin:api")
include(":feature:profile")
include(":feature:profile:api")
include(":feature:mood")
include(":feature:mood:api")
include(":feature:settings")
include(":feature:settings:api")
include(":model")
include(":repository")
// The Sense ML system lives under library/ (SDK-extractable; see docs/SENSE_ML.md).
// Module names stay flat (:sense-*) so dependency declarations don't churn.
include(":sense-ml")
project(":sense-ml").projectDir = file("library/sense-ml")
include(":sense-signals")
project(":sense-signals").projectDir = file("library/sense-signals")
include(":sense-store")
project(":sense-store").projectDir = file("library/sense-store")
include(":sense-delivery")
project(":sense-delivery").projectDir = file("library/sense-delivery")
// Tier-2 on-device text embedding — its own library module (not :feature:checkin, and not
// a dynamic feature module: it ships in the base APK; only the model *data* is deferred,
// via the :model_pack Play Asset Delivery pack below).
include(":text-embed")
project(":text-embed").projectDir = file("library/text-embed")
// Play Asset Delivery, on-demand: the ~25MB sentence-encoder model, no code. See
// model_pack/build.gradle.kts.
include(":model_pack")
