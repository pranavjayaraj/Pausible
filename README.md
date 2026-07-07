# Pausible

An App for Mental Wellness

## Structure

```
reset-app/
├─ settings.gradle.kts          # includes :app and :core
├─ build.gradle.kts             # root plugins (apply false)
├─ gradle.properties            # SDK levels + Gradle flags
├─ gradle/libs.versions.toml    # version catalog (single source of truth)
├─ build-logic/                 # composite build with convention plugins
│   └─ convention/
│       ├─ AndroidApplicationComposePlugin   → reset.android.application.compose
│       ├─ AndroidLibraryPlugin              → reset.android.library
│       ├─ AndroidLibraryComposePlugin       → reset.android.library.compose
│       ├─ HiltPlugin                        → reset.hilt
│       └─ reset/plugins/  (KotlinAndroid, AndroidCompose, AndroidApp, Ext)
├─ app/                         # @HiltAndroidApp + Compose MainActivity (the "Hello World")
└─ core/                        # android library; provides GreetingProvider via Hilt
```

`MainActivity` field-injects `GreetingProvider` from `:core`, proving cross-module Hilt +
Compose wiring end to end.

## Build & run

Requires JDK 17+ and the Android SDK (compileSdk 34). `local.properties` points at the SDK.

```bash
./gradlew :app:assembleDebug                 # build
./gradlew :app:installDebug                  # install on a connected device/emulator
adb shell am start -n com.reset.app/.MainActivity
```

## What was intentionally left out (vs. the blueprint)

The following are deferred until the project actually needs
them: macrobenchmark + baseline profiles, R8 full mode, the AAR↔source toggle, Showkase,
CI workflows, and the full `feature:* + domain:* + data:* + model:*` vertical. 
