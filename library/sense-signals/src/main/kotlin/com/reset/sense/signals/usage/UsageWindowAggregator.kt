package com.reset.sense.signals.usage

import com.reset.sense.ml.AppCategory
import com.reset.sense.ml.UsageSnapshot
import kotlin.math.max
import kotlin.math.min

/** [UsageSnapshot] plus usage-derived fields that live outside the feature vector. */
data class UsageAggregate(
    val usage: UsageSnapshot,
    val minutesSinceFirstUnlockToday: Int,
    /** Wall-clock of the most recent screen-off event today, if any. Feeds the
     *  wind-down success signal ("screens off soon after the nudge"). */
    val lastScreenOffMs: Long? = null,
)

/**
 * Turns a day's worth of [RawUsageEvent]s into the normalized usage signals
 * of the current context window. Pure Kotlin — the only Android-specific part
 * is producing the event list (see AndroidUsageEventsSource).
 *
 * Expected event range: from local midnight (for first-unlock) through `now`.
 * Screen/unlock events require API 28+; on older devices those fields degrade
 * to zero and the model runs on the remaining features.
 */
class UsageWindowAggregator(
    private val windowMs: Long = 5 * 60_000L,
    private val lookbackMs: Long = 30 * 60_000L,
) {

    fun aggregate(
        events: List<RawUsageEvent>,
        nowMs: Long,
        categoryOf: (String) -> AppCategory,
        /** From the optional notification-listener seam; 0 when not available. */
        notificationDrivenOpensLastHour: Int = 0,
    ): UsageAggregate {
        val windowStart = nowMs - windowMs
        val lookbackStart = nowMs - lookbackMs
        val sessions = AppSessionNormalizer().normalize(events, nowMs)

        // ---- app-usage block
        val foregroundDurationSec = (sessions.sumOf { overlap(it, windowStart, nowMs) } / 1000L).toInt()

        val windowSessions = sessions.filter { it.endMs > windowStart }
        val uniqueAppCount = windowSessions.map { it.packageName }.toSet().size
        val appSwitchCount = windowSessions.zipWithNext()
            .count { (a, b) -> a.packageName != b.packageName }

        val lookbackSessions = sessions.filter { it.endMs > lookbackStart }
        val longestSessionSec = (
            lookbackSessions.maxOfOrNull { overlap(it, lookbackStart, nowMs) } ?: 0L
            ).toInt() / 1000

        // Re-entries into the same distracting app within the lookback:
        // n visits to one package count as n-1 "returns".
        val distractingVisits = HashMap<String, Int>()
        for (session in lookbackSessions) {
            val category = categoryOf(session.packageName)
            if (category == AppCategory.SOCIAL || category == AppCategory.GAME_DATING || category == AppCategory.VIDEO) {
                distractingVisits.merge(session.packageName, 1, Int::plus)
            }
        }
        val distractingReturnCount = distractingVisits.values.sumOf { max(0, it - 1) }

        // ---- screen block
        val screenIntervals = buildScreenIntervals(events, nowMs)
        val screenOnTimeSec = (
            screenIntervals.sumOf { (start, end) -> max(0L, min(end, nowMs) - max(start, windowStart)) } / 1000L
            ).toInt()
        val lastInterval = screenIntervals.lastOrNull()
        val continuousScreenOnMin =
            if (lastInterval != null && lastInterval.second == nowMs) {
                ((nowMs - lastInterval.first) / 60_000L).toInt()
            } else 0

        // ---- unlock block
        val unlocks = events.filter { it.type == RawEventType.UNLOCK }
        val unlockCountLastHour = unlocks.count { it.timestampMs >= nowMs - 3_600_000L }
        val coldOpenCount = max(0, unlockCountLastHour - notificationDrivenOpensLastHour)
        val minutesSinceFirstUnlockToday = unlocks.minOfOrNull { it.timestampMs }
            ?.let { ((nowMs - it) / 60_000L).toInt() } ?: 0

        // ---- current foreground category (most recent session still open at `now`)
        val currentPackage = sessions.lastOrNull { it.endMs >= nowMs }?.packageName
            ?: sessions.lastOrNull()?.packageName
        val foregroundCategory = currentPackage?.let(categoryOf) ?: AppCategory.OTHER

        return UsageAggregate(
            usage = UsageSnapshot(
                foregroundDurationSec = foregroundDurationSec,
                appSwitchCount = appSwitchCount,
                uniqueAppCount = uniqueAppCount,
                longestSessionSec = longestSessionSec,
                screenOnTimeSec = screenOnTimeSec,
                continuousScreenOnMin = continuousScreenOnMin,
                distractingReturnCount = distractingReturnCount,
                coldOpenCount = coldOpenCount,
                unlockCountLastHour = unlockCountLastHour,
                foregroundCategory = foregroundCategory,
            ),
            minutesSinceFirstUnlockToday = minutesSinceFirstUnlockToday,
            lastScreenOffMs = events
                .filter { it.type == RawEventType.SCREEN_OFF }
                .maxOfOrNull { it.timestampMs },
        )
    }

    private fun overlap(session: AppSession, from: Long, to: Long): Long =
        max(0L, min(session.endMs, to) - max(session.startMs, from))

    /**
     * SCREEN_ON/SCREEN_OFF events → list of on-intervals, oldest first. An
     * interval still open at the end is closed at [nowMs] (that closure is
     * what marks "screen is on right now" for the continuous-streak signal).
     * A leading SCREEN_OFF implies the screen was on since the stream began.
     */
    private fun buildScreenIntervals(events: List<RawUsageEvent>, nowMs: Long): List<Pair<Long, Long>> {
        val screenEvents = events
            .filter { it.type == RawEventType.SCREEN_ON || it.type == RawEventType.SCREEN_OFF }
            .sortedBy { it.timestampMs }
        if (screenEvents.isEmpty()) return emptyList()

        val streamStartMs = events.minOf { it.timestampMs }
        val intervals = ArrayList<Pair<Long, Long>>()
        var onSince: Long? = null
        for ((index, event) in screenEvents.withIndex()) {
            when (event.type) {
                RawEventType.SCREEN_ON -> if (onSince == null) onSince = event.timestampMs
                RawEventType.SCREEN_OFF -> {
                    val start = onSince ?: if (index == 0) streamStartMs else null
                    if (start != null && event.timestampMs > start) intervals += start to event.timestampMs
                    onSince = null
                }
                else -> Unit
            }
        }
        onSince?.let { if (nowMs > it) intervals += it to nowMs }
        return intervals
    }
}
