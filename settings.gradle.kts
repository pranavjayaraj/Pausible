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
include(":feature:profile")
include(":feature:profile:api")
include(":feature:mood")
include(":feature:mood:api")
include(":model")
include(":repository")
