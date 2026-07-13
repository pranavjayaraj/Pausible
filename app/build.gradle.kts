plugins {
    alias(libs.plugins.reset.androidApplicationCompose)
    alias(libs.plugins.reset.hilt)
    // The app-owned OnboardingDestination is a @Serializable typed route.
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.reset.app"
    defaultConfig {
        applicationId = "com.reset.app"
        versionCode = 1
        versionName = "1.0.0"
        multiDexEnabled = true
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":navigation"))
    implementation(project(":feature:builder"))
    implementation(project(":feature:builder:api"))
    implementation(project(":feature:home"))
    implementation(project(":feature:home:api"))
    implementation(project(":feature:sessions"))
    implementation(project(":feature:sessions:api"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:profile:api"))
    implementation(project(":feature:mood"))
    implementation(project(":feature:mood:api"))
    implementation(project(":model"))
    implementation(project(":repository"))
    // Sense: context-aware microbreak engine (tick scheduling, prompt deep-link
    // handling, completion reporting back into the decision log).
    implementation(project(":sense-delivery"))
    implementation(project(":sense-signals"))
    implementation(project(":sense-store"))
    // The app host owns the real navigation graph (NavHost) and drives ObserveNavigation,
    // so it depends on navigation-compose directly rather than inheriting it from :navigation.
    implementation(libs.androidx.navigation.compose)
    // Branded launch: system splash themed via Theme.ResetApp.Starting, held until first frame.
    implementation(libs.androidx.splashscreen)
    implementation(libs.timber)
    // App-scope coroutine launch of the startup ReminderScheduler sync.
    implementation(libs.coroutines.android)
}
