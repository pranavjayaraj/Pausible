package com.reset.sense.signals.android

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import com.reset.sense.signals.usage.RawEventType
import com.reset.sense.signals.usage.RawUsageEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Seam over `UsageStatsManager.queryEvents` so the aggregator stays testable. */
interface UsageEventsSource {
    /** Events in [beginMs, endMs], chronological. Empty without usage-access permission. */
    fun query(beginMs: Long, endMs: Long): List<RawUsageEvent>
}

class AndroidUsageEventsSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : UsageEventsSource {

    override fun query(beginMs: Long, endMs: Long): List<RawUsageEvent> {
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyList()
        val usageEvents = manager.queryEvents(beginMs, endMs) ?: return emptyList()

        val out = ArrayList<RawUsageEvent>(256)
        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val type = mapType(event.eventType) ?: continue
            val pkg = event.packageName ?: continue
            out += RawUsageEvent(
                type = type,
                packageName = pkg,
                timestampMs = event.timeStamp,
                className = event.className,
            )
        }
        return out
    }

    /**
     * ACTIVITY_RESUMED/PAUSED (API 29+) share constant values with the legacy
     * MOVE_TO_FOREGROUND/BACKGROUND (1/2), so one mapping covers both event
     * models; ACTIVITY_STOPPED (23) additionally maps to BACKGROUND and is
     * deduplicated by the normalizer's per-activity set semantics.
     * Screen/unlock events exist from API 28; below that they simply never
     * appear and those features degrade to zero.
     */
    private fun mapType(eventType: Int): RawEventType? = when {
        eventType == LEGACY_MOVE_TO_FOREGROUND -> RawEventType.FOREGROUND
        eventType == LEGACY_MOVE_TO_BACKGROUND -> RawEventType.BACKGROUND
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            eventType == UsageEvents.Event.ACTIVITY_STOPPED -> RawEventType.BACKGROUND
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> when (eventType) {
            UsageEvents.Event.SCREEN_INTERACTIVE -> RawEventType.SCREEN_ON
            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> RawEventType.SCREEN_OFF
            UsageEvents.Event.KEYGUARD_HIDDEN -> RawEventType.UNLOCK
            else -> null
        }
        else -> null
    }

    private companion object {
        // == UsageEvents.Event.ACTIVITY_RESUMED / ACTIVITY_PAUSED on API 29+.
        @Suppress("DEPRECATION")
        const val LEGACY_MOVE_TO_FOREGROUND = UsageEvents.Event.MOVE_TO_FOREGROUND

        @Suppress("DEPRECATION")
        const val LEGACY_MOVE_TO_BACKGROUND = UsageEvents.Event.MOVE_TO_BACKGROUND
    }
}
