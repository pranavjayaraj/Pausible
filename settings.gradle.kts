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
