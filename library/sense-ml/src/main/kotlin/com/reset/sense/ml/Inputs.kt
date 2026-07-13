package com.reset.sense.ml

/**
 * Pure-data inputs to the decision engine. `:sense-signals` is responsible for
 * producing these from Android APIs; nothing in this module touches Android.
 */

/** Coarse app category. The model never sees package names — only this. */
enum class AppCategory { WORK, SOCIAL, VIDEO, GAME_DATING, CHAT, OTHER }

/** Physical context from the Activity Recognition Transition API. */
enum class ActivityState { STILL, ON_FOOT, IN_VEHICLE, UNKNOWN }

/** Aggregated usage signals for the current 5-minute context window. */
data class UsageSnapshot(
    val foregroundDurationSec: Int,
    val appSwitchCount: Int,
    val uniqueAppCount: Int,
    val longestSessionSec: Int,
    val screenOnTimeSec: Int,
    val continuousScreenOnMin: Int,
    val distractingReturnCount: Int,
    val coldOpenCount: Int,
    val unlockCountLastHour: Int,
    val foregroundCategory: AppCategory,
)

/** Device + physical context at evaluation time. */
data class DeviceSnapshot(
    val activityState: ActivityState,
    val minutesInCurrentActivity: Int,
    val charging: Boolean,
    val batteryPercent: Int,
    val minutesSinceFirstUnlockToday: Int,
)

/** Wall-clock context. [dayOfWeek] is 0..6 (Monday = 0, matching the trainer). */
data class TimeContext(
    val hourOfDay: Int,
    val minuteOfHour: Int,
    val dayOfWeek: Int,
) {
    val fractionalHour: Float get() = hourOfDay + minuteOfHour / 60f
    val isLateNight: Boolean get() = hourOfDay >= 23 || hourOfDay < 5
}

/**
 * Per-user response history — the personalization block. Defaults are the
 * cold-start priors (0.5 = "no evidence either way"), matching the trainer.
 */
data class ResponseHistory(
    val minutesSinceLastCompletedBreak: Int = Int.MAX_VALUE,
    val dismissCount24h: Int = 0,
    val snoozeCount24h: Int = 0,
    val acceptRate7d: Float = 0.5f,
    val acceptRateThisHour: Float = 0.5f,
    val avgResponseDelaySec: Float = 60f,
    val breakCompletionRate: Float = 0.5f,
    /** Total labeled prompt outcomes ever logged — drives the rules→model blend ramp. */
    val labeledOutcomeCount: Int = 0,
    /** Prompts historically shown in this hour-of-day bucket (for dead-hour detection). */
    val promptsShownThisHourHistoric: Int = 0,
)

/** Deterministic gate context — evaluated BEFORE any model inference. */
data class GateContext(
    /** Sustained phone interaction while IN_VEHICLE ⇒ passenger, gate lifts. */
    val activePhoneUse: Boolean,
    val promptsShownToday: Int,
    val quietHoursStart: Int = 22,
    val quietHoursEnd: Int = 7,
    /** WIND_DOWN prompts already shown since tonight's quiet-hours start. */
    val windDownShownTonight: Int = 0,
)

/** Everything the engine needs for one evaluation tick. */
data class DecisionInput(
    val usage: UsageSnapshot,
    val device: DeviceSnapshot,
    val time: TimeContext,
    val history: ResponseHistory,
    val gate: GateContext,
)
