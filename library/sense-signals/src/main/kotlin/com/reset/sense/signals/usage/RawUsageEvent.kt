package com.reset.sense.signals.usage

/**
 * Platform-independent projection of a `UsageEvents.Event`. The Android
 * adapter ([com.reset.sense.signals.android.AndroidUsageEventsSource]) maps
 * system events into this type; everything downstream (normalizer,
 * aggregator) is pure Kotlin and unit-testable without Robolectric.
 */
enum class RawEventType {
    /** ACTIVITY_RESUMED (API 29+) or legacy MOVE_TO_FOREGROUND. */
    FOREGROUND,

    /** ACTIVITY_PAUSED / ACTIVITY_STOPPED (API 29+) or legacy MOVE_TO_BACKGROUND. */
    BACKGROUND,

    /** SCREEN_INTERACTIVE (API 28+). */
    SCREEN_ON,

    /** SCREEN_NON_INTERACTIVE (API 28+). */
    SCREEN_OFF,

    /** KEYGUARD_HIDDEN (API 28+) — an unlock. */
    UNLOCK,
}

data class RawUsageEvent(
    val type: RawEventType,
    val packageName: String,
    val timestampMs: Long,
    /**
     * Activity class for API 29+ events; null on legacy streams. Used to
     * balance per-Activity RESUMED/PAUSED pairs — see [AppSessionNormalizer].
     */
    val className: String? = null,
)

/** One continuous foreground run of a package, app-level (activities collapsed). */
data class AppSession(
    val packageName: String,
    val startMs: Long,
    val endMs: Long,
) {
    val durationMs: Long get() = endMs - startMs
}
