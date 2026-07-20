package com.reset.sense.ml

/**
 * Pure-data inputs to the decision engine. `:sense-signals` is responsible for
 * producing these from Android APIs; nothing in this module touches Android.
 */

/**
 * Coarse app category, named for the BEHAVIOR pattern, not the content
 * vertical. The model never sees package names — only this.
 *
 * ORDER IS LOAD-BEARING: the ordinal is the one-hot slot (features 9–14) and
 * must keep matching the trainer's CATEGORIES list and the spec labels
 * (`cat_work`, `cat_social`, `cat_video`, `cat_game_dating`, `cat_chat`,
 * `cat_other`). The spec labels keep their legacy names until the next real
 * SCHEMA_VERSION bump — renaming them alone would orphan the bundled
 * artifact for zero benefit. Kotlin-side names are free to be honest:
 * SOCIAL_FEED/STREAMING/REWARD_LOOP form the "distracting" set
 * (UsageWindowAggregator) — feeds, streams, and variable-reward loops.
 */
enum class AppCategory {
    WORK,
    /** Infinite-scroll feeds: social networks, short-video, news doomscroll. */
    SOCIAL_FEED,
    /** Long-form passive watching: YouTube, Netflix, Hotstar. */
    STREAMING,
    /** Variable-reward compulsion loops: games, dating swipes, fantasy/betting. */
    REWARD_LOOP,
    /** 1:1 / group communication — deliberately NOT in the distracting set. */
    MESSAGING,
    OTHER,
}

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
    /** Empirical-Bayes-shrunk per-hour accept rate — feeds the feature vector
     *  and the rules receptivity term. Sparse buckets read near the prior. */
    val acceptRateThisHour: Float = 0.5f,
    /** Raw (unshrunk) per-hour accept rate — the dead-hour gate needs actual
     *  observed acceptance; shrinkage would pull a genuinely dead hour above
     *  its 5% bar and keep it alive forever. */
    val acceptRateThisHourRaw: Float = 0.5f,
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
