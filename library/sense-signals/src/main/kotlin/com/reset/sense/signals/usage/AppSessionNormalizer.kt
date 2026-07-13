package com.reset.sense.signals.usage

/**
 * Collapses Activity-level usage events into app-level [AppSession]s.
 *
 * Why this exists: from API 29, `MOVE_TO_FOREGROUND`/`MOVE_TO_BACKGROUND` are
 * deprecated in favour of `ACTIVITY_RESUMED`/`ACTIVITY_PAUSED`/`ACTIVITY_STOPPED`,
 * and the semantics change from per-app to **per-Activity**:
 *
 *  - one app can fire multiple RESUMED events as the user moves between its
 *    activities (naïve counting inflates `app_switch_count`);
 *  - under split-screen, two packages hold RESUMED activities simultaneously;
 *  - a backgrounded activity fires PAUSED *and then* STOPPED (double-decrement
 *    hazard for count-based approaches).
 *
 * Approach: track the set of live activity keys per package. A session opens
 * when a package's set becomes non-empty and closes when it empties. Set
 * semantics make repeated RESUMED and PAUSED→STOPPED for the same activity
 * idempotent. Legacy streams (null className) degrade to one key per package,
 * which reproduces the old app-level behaviour exactly.
 *
 * Sessions of the same package separated by less than [debounceMs] are merged
 * to absorb intra-app activity transitions that momentarily empty the set.
 */
class AppSessionNormalizer(private val debounceMs: Long = 2_000) {

    /**
     * @param events any order; only FOREGROUND/BACKGROUND are consumed.
     * @param endOfStreamMs close time for sessions still open (usually `now`).
     */
    fun normalize(events: List<RawUsageEvent>, endOfStreamMs: Long): List<AppSession> {
        val liveActivities = HashMap<String, MutableSet<String>>()
        val openStart = HashMap<String, Long>()
        val completed = ArrayList<AppSession>()

        for (event in events.sortedBy { it.timestampMs }) {
            val pkg = event.packageName
            val activityKey = event.className ?: pkg
            when (event.type) {
                RawEventType.FOREGROUND -> {
                    val set = liveActivities.getOrPut(pkg) { mutableSetOf() }
                    val wasEmpty = set.isEmpty()
                    set += activityKey
                    if (wasEmpty && pkg !in openStart) {
                        val previous = completed.lastOrNull()
                        if (previous != null &&
                            previous.packageName == pkg &&
                            event.timestampMs - previous.endMs <= debounceMs
                        ) {
                            // Reopen: the gap was an intra-app transition, not a real exit.
                            completed.removeAt(completed.lastIndex)
                            openStart[pkg] = previous.startMs
                        } else {
                            openStart[pkg] = event.timestampMs
                        }
                    }
                }

                RawEventType.BACKGROUND -> {
                    val set = liveActivities[pkg] ?: continue
                    set -= activityKey // STOPPED after PAUSED: second remove is a no-op
                    if (set.isEmpty()) {
                        openStart.remove(pkg)?.let { start ->
                            if (event.timestampMs > start) {
                                completed += AppSession(pkg, start, event.timestampMs)
                            }
                        }
                    }
                }

                else -> Unit // screen/unlock events are handled by the aggregator
            }
        }

        // Sessions still open at end of stream (the app currently in foreground).
        for ((pkg, start) in openStart) {
            if (endOfStreamMs > start) completed += AppSession(pkg, start, endOfStreamMs)
        }
        return completed.sortedBy { it.startMs }
    }
}
