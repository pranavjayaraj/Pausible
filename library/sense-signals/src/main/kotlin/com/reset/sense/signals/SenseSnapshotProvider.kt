package com.reset.sense.signals

import android.content.Context
import com.reset.sense.ml.DeviceSnapshot
import com.reset.sense.ml.TimeContext
import com.reset.sense.ml.UsageSnapshot
import com.reset.sense.signals.activity.ActivityStateStore
import com.reset.sense.signals.android.AppCategoryResolver
import com.reset.sense.signals.android.DeviceStateSource
import com.reset.sense.signals.android.UsageEventsSource
import com.reset.sense.signals.usage.UsageWindowAggregator
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Everything the decision engine needs that comes from device signals. */
data class SenseSnapshots(
    val usage: UsageSnapshot,
    val device: DeviceSnapshot,
    val time: TimeContext,
    val accuracyTier: SenseAccuracyTier,
    /** Most recent screen-off today; drives the wind-down success signal. */
    val lastScreenOffMs: Long? = null,
)

/**
 * Assembles [SenseSnapshots] at evaluation time (the WorkManager tick or an
 * event-driven re-evaluation). Stateless between calls: every field is
 * re-derived from `queryEvents` + stores, so a deferred or killed process
 * changes nothing — there is no "I remember the last tick" state to corrupt.
 *
 * ResponseHistory and GateContext come from the decision log (`:sense-store`),
 * not from here — device signals and learned feedback are different layers.
 */
class SenseSnapshotProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageEventsSource: UsageEventsSource,
    private val categoryResolver: AppCategoryResolver,
    private val deviceStateSource: DeviceStateSource,
) {

    private val aggregator = UsageWindowAggregator()

    suspend fun capture(nowMs: Long = System.currentTimeMillis()): SenseSnapshots =
        withContext(Dispatchers.IO) {
            val events = usageEventsSource.query(localMidnightMs(nowMs), nowMs)
            val aggregate = aggregator.aggregate(
                events = events,
                nowMs = nowMs,
                categoryOf = categoryResolver::categoryOf,
                // Notification-listener seam lands with :sense-delivery;
                // until then every unlock counts as a cold open (conservative).
                notificationDrivenOpensLastHour = 0,
            )

            val activity = ActivityStateStore(context).current(nowMs)
            val battery = deviceStateSource.battery()

            SenseSnapshots(
                usage = aggregate.usage,
                device = DeviceSnapshot(
                    activityState = activity.state,
                    minutesInCurrentActivity = ((nowMs - activity.sinceMs) / 60_000L).toInt(),
                    charging = battery.charging,
                    batteryPercent = battery.batteryPercent,
                    minutesSinceFirstUnlockToday = aggregate.minutesSinceFirstUnlockToday,
                ),
                time = timeContext(nowMs),
                accuracyTier = SensePermissions.tier(context),
                lastScreenOffMs = aggregate.lastScreenOffMs,
            )
        }

    private fun timeContext(nowMs: Long): TimeContext {
        val calendar = Calendar.getInstance().apply { timeInMillis = nowMs }
        return TimeContext(
            hourOfDay = calendar.get(Calendar.HOUR_OF_DAY),
            minuteOfHour = calendar.get(Calendar.MINUTE),
            // Calendar: SUNDAY=1..SATURDAY=7 → trainer convention Monday=0..Sunday=6.
            dayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7,
        )
    }

    private fun localMidnightMs(nowMs: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
