package com.reset.sense.ml

import com.reset.sense.ml.FeatureSchema.FEATURE_COUNT
import com.reset.sense.ml.FeatureSchema.IDX_ACCEPT_RATE_7D
import com.reset.sense.ml.FeatureSchema.IDX_ACCEPT_RATE_THIS_HOUR
import com.reset.sense.ml.FeatureSchema.IDX_ACTIVITY_FIRST
import com.reset.sense.ml.FeatureSchema.IDX_APP_SWITCH_COUNT
import com.reset.sense.ml.FeatureSchema.IDX_AVG_RESPONSE_DELAY
import com.reset.sense.ml.FeatureSchema.IDX_BATTERY
import com.reset.sense.ml.FeatureSchema.IDX_BREAK_COMPLETION_RATE
import com.reset.sense.ml.FeatureSchema.IDX_CAT_FIRST
import com.reset.sense.ml.FeatureSchema.IDX_CHARGING
import com.reset.sense.ml.FeatureSchema.IDX_COLD_OPENS
import com.reset.sense.ml.FeatureSchema.IDX_CONTINUOUS_SCREEN_ON
import com.reset.sense.ml.FeatureSchema.IDX_DISMISS_24H
import com.reset.sense.ml.FeatureSchema.IDX_DISTRACTING_RETURNS
import com.reset.sense.ml.FeatureSchema.IDX_DOW_COS
import com.reset.sense.ml.FeatureSchema.IDX_DOW_SIN
import com.reset.sense.ml.FeatureSchema.IDX_FOREGROUND_DURATION
import com.reset.sense.ml.FeatureSchema.IDX_HOUR_COS
import com.reset.sense.ml.FeatureSchema.IDX_HOUR_SIN
import com.reset.sense.ml.FeatureSchema.IDX_LATE_NIGHT
import com.reset.sense.ml.FeatureSchema.IDX_LONGEST_SESSION
import com.reset.sense.ml.FeatureSchema.IDX_MINUTES_IN_ACTIVITY
import com.reset.sense.ml.FeatureSchema.IDX_MIN_SINCE_FIRST_UNLOCK
import com.reset.sense.ml.FeatureSchema.IDX_MIN_SINCE_LAST_BREAK
import com.reset.sense.ml.FeatureSchema.IDX_SCREEN_ON_TIME
import com.reset.sense.ml.FeatureSchema.IDX_SNOOZE_24H
import com.reset.sense.ml.FeatureSchema.IDX_UNIQUE_APP_COUNT
import com.reset.sense.ml.FeatureSchema.IDX_UNLOCKS_LAST_HOUR
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Builds the FloatArray(34) model input from pure-data snapshots.
 * Mirrors `bootstrap_data.featurize()` in the Python trainer exactly —
 * cross-language agreement is enforced by [FeatureSchema.specHash] at model
 * load and by the golden-vector parity test.
 */
object FeatureBuilder {

    fun build(input: DecisionInput): FloatArray = build(
        usage = input.usage,
        device = input.device,
        time = input.time,
        history = input.history,
    )

    fun build(
        usage: UsageSnapshot,
        device: DeviceSnapshot,
        time: TimeContext,
        history: ResponseHistory,
    ): FloatArray {
        val f = FloatArray(FEATURE_COUNT)

        // Block 1 — usage
        f[IDX_FOREGROUND_DURATION] = norm(usage.foregroundDurationSec, FeatureSchema.NORM_FOREGROUND_SEC)
        f[IDX_APP_SWITCH_COUNT] = norm(usage.appSwitchCount, FeatureSchema.NORM_SWITCH_COUNT)
        f[IDX_UNIQUE_APP_COUNT] = norm(usage.uniqueAppCount, FeatureSchema.NORM_UNIQUE_APPS)
        f[IDX_LONGEST_SESSION] = norm(usage.longestSessionSec, FeatureSchema.NORM_LONGEST_SESSION_SEC)
        f[IDX_SCREEN_ON_TIME] = norm(usage.screenOnTimeSec, FeatureSchema.NORM_SCREEN_ON_SEC)
        f[IDX_CONTINUOUS_SCREEN_ON] = norm(usage.continuousScreenOnMin, FeatureSchema.NORM_CONTINUOUS_SCREEN_MIN)
        f[IDX_DISTRACTING_RETURNS] = norm(usage.distractingReturnCount, FeatureSchema.NORM_DISTRACTING_RETURNS)
        f[IDX_COLD_OPENS] = norm(usage.coldOpenCount, FeatureSchema.NORM_COLD_OPENS)
        f[IDX_UNLOCKS_LAST_HOUR] = norm(usage.unlockCountLastHour, FeatureSchema.NORM_UNLOCKS_HOUR)

        // Block 2 — app category one-hot (ordinal order matches trainer CATEGORIES)
        f[IDX_CAT_FIRST + usage.foregroundCategory.ordinal] = 1f

        // Block 3 — cyclical time
        val hour = time.fractionalHour
        f[IDX_HOUR_SIN] = sin(2.0 * PI * hour / 24.0).toFloat()
        f[IDX_HOUR_COS] = cos(2.0 * PI * hour / 24.0).toFloat()
        f[IDX_DOW_SIN] = sin(2.0 * PI * time.dayOfWeek / 7.0).toFloat()
        f[IDX_DOW_COS] = cos(2.0 * PI * time.dayOfWeek / 7.0).toFloat()
        f[IDX_LATE_NIGHT] = if (time.isLateNight) 1f else 0f

        // Block 4 — physical & device context. IN_VEHICLE never reaches the
        // model (hard-gated upstream); it maps to the UNKNOWN slot defensively.
        val activitySlot = when (device.activityState) {
            ActivityState.STILL -> 0
            ActivityState.ON_FOOT -> 1
            ActivityState.IN_VEHICLE, ActivityState.UNKNOWN -> 2
        }
        f[IDX_ACTIVITY_FIRST + activitySlot] = 1f
        f[IDX_MINUTES_IN_ACTIVITY] = norm(device.minutesInCurrentActivity, FeatureSchema.NORM_MINUTES_IN_ACTIVITY)
        f[IDX_CHARGING] = if (device.charging) 1f else 0f
        f[IDX_BATTERY] = norm(device.batteryPercent, FeatureSchema.NORM_BATTERY_PCT)
        f[IDX_MIN_SINCE_FIRST_UNLOCK] = norm(device.minutesSinceFirstUnlockToday, FeatureSchema.NORM_MIN_FIRST_UNLOCK)

        // Block 5 — personalization
        f[IDX_MIN_SINCE_LAST_BREAK] = norm(
            history.minutesSinceLastCompletedBreak.coerceAtMost(1_000_000),
            FeatureSchema.NORM_MIN_SINCE_BREAK,
        )
        f[IDX_DISMISS_24H] = norm(history.dismissCount24h, FeatureSchema.NORM_DISMISS)
        f[IDX_SNOOZE_24H] = norm(history.snoozeCount24h, FeatureSchema.NORM_SNOOZE)
        f[IDX_ACCEPT_RATE_7D] = history.acceptRate7d.coerceIn(0f, 1f)
        f[IDX_ACCEPT_RATE_THIS_HOUR] = history.acceptRateThisHour.coerceIn(0f, 1f)
        f[IDX_AVG_RESPONSE_DELAY] = (history.avgResponseDelaySec / FeatureSchema.NORM_RESPONSE_DELAY_SEC).coerceIn(0f, 1f)
        f[IDX_BREAK_COMPLETION_RATE] = history.breakCompletionRate.coerceIn(0f, 1f)

        return f
    }

    private fun norm(value: Int, divisor: Float): Float = (value / divisor).coerceIn(0f, 1f)
}
