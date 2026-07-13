package com.reset.sense.ml

import com.reset.sense.ml.FeatureSchema.IDX_ACTIVITY_FIRST
import com.reset.sense.ml.FeatureSchema.IDX_APP_SWITCH_COUNT
import com.reset.sense.ml.FeatureSchema.IDX_CHARGING
import com.reset.sense.ml.FeatureSchema.IDX_COLD_OPENS
import com.reset.sense.ml.FeatureSchema.IDX_CONTINUOUS_SCREEN_ON
import com.reset.sense.ml.FeatureSchema.IDX_DISMISS_24H
import com.reset.sense.ml.FeatureSchema.IDX_DISTRACTING_RETURNS
import com.reset.sense.ml.FeatureSchema.IDX_LATE_NIGHT
import com.reset.sense.ml.FeatureSchema.IDX_MINUTES_IN_ACTIVITY
import com.reset.sense.ml.FeatureSchema.IDX_MIN_SINCE_LAST_BREAK
import kotlin.math.max

/** Why a prompt was suppressed before any scoring happened. */
enum class GateReason { VEHICLE, QUIET_HOURS, NIGHT_CAP, COOLDOWN, DAILY_CAP, DEAD_HOUR }

sealed class GateResult {
    data object Pass : GateResult()
    data class Suppressed(val reason: GateReason) : GateResult()

    /**
     * Quiet hours, but the user is demonstrably awake and mid-doomscroll
     * (long continuous screen-on). The ONLY allowed intervention is a single,
     * silent, sleep-framed WIND_DOWN nudge — see BreakDecisionEngine.
     * Waking the user remains impossible: this state is reachable only when
     * the screen has already been on for a sustained stretch.
     */
    data object WindDownOnly : GateResult()
}

/**
 * Stage-0 deterministic layer: hard gates + interpretable receptivity score.
 *
 * The gates are policy, not features — the model never gets the chance to
 * learn its way around them (e.g. "driving users accept prompts"). The score
 * is the cold-start fallback and the baseline every trained model must beat
 * (enforced by the ship gate in ml/train.py, which replicates these weights).
 */
class RulesEngine(
    private val cooldownMinutes: Int = 20,
    /** Prompts/day every user starts with. The scarcity promise. */
    private val baseDailyPromptCap: Int = 3,
    /** Prompts/day for users who demonstrably honor them — see [dailyPromptCap]. */
    private val earnedDailyPromptCap: Int = 5,
    /** Evidence floor before trust can raise the cap; below this the
     *  completion rate is noise, not a track record. */
    private val earnedTrustMinOutcomes: Int = 20,
    private val earnedTrustMinCompletionRate: Float = 0.6f,
    /** An hour is "dead" once we have enough history and near-zero acceptance in it. */
    private val deadHourMinPrompts: Int = 5,
    private val deadHourMaxAcceptRate: Float = 0.05f,
    /** Continuous screen-on required before quiet hours may become WindDownOnly —
     *  a quick alarm-check is not doomscrolling. */
    private val windDownMinActiveMin: Int = 20,
    /** Hard nightly cap for wind-down nudges. One is care; two is nagging. */
    private val nightlyWindDownCap: Int = 1,
) {

    fun gate(input: DecisionInput): GateResult {
        val g = input.gate
        val inVehicle = input.device.activityState == ActivityState.IN_VEHICLE
        if (inVehicle && !g.activePhoneUse) return GateResult.Suppressed(GateReason.VEHICLE)

        if (isInQuietHours(input.time.hourOfDay, g.quietHoursStart, g.quietHoursEnd)) {
            return quietHoursGate(input)
        }
        if (input.history.minutesSinceLastCompletedBreak < cooldownMinutes) {
            return GateResult.Suppressed(GateReason.COOLDOWN)
        }
        if (g.promptsShownToday >= dailyPromptCap(input.history)) {
            return GateResult.Suppressed(GateReason.DAILY_CAP)
        }
        if (isDeadHour(input)) return GateResult.Suppressed(GateReason.DEAD_HOUR)
        return GateResult.Pass
    }

    /**
     * Earned-trust cap: everyone starts at [baseDailyPromptCap]; a user whose
     * trailing completion rate proves prompts are welcome earns
     * [earnedDailyPromptCap]. Self-decaying — the completion rate is a
     * trailing-30d window, so a stretch of dismissals drops it back below the
     * bar and the cap reverts on its own. More notifications must be EARNED
     * by completed breaks, never granted by engagement pressure.
     */
    fun dailyPromptCap(history: ResponseHistory): Int =
        if (history.labeledOutcomeCount >= earnedTrustMinOutcomes &&
            history.breakCompletionRate >= earnedTrustMinCompletionRate
        ) earnedDailyPromptCap else baseDailyPromptCap

    /**
     * Inside quiet hours the cardinal sin is WAKING the user. Nudging someone
     * whose screen has already been on for [windDownMinActiveMin] wakes nobody
     * — that is the highest-value intervention window this product has. All
     * other suppressions still apply; the dead-hour gate makes the exception
     * self-extinguishing for users who dismiss it.
     */
    private fun quietHoursGate(input: DecisionInput): GateResult {
        if (input.usage.continuousScreenOnMin < windDownMinActiveMin) {
            return GateResult.Suppressed(GateReason.QUIET_HOURS)
        }
        if (input.gate.windDownShownTonight >= nightlyWindDownCap) {
            return GateResult.Suppressed(GateReason.NIGHT_CAP)
        }
        if (input.history.minutesSinceLastCompletedBreak < cooldownMinutes) {
            return GateResult.Suppressed(GateReason.COOLDOWN)
        }
        if (isDeadHour(input)) return GateResult.Suppressed(GateReason.DEAD_HOUR)
        return GateResult.WindDownOnly
    }

    private fun isDeadHour(input: DecisionInput): Boolean =
        input.history.promptsShownThisHourHistoric >= deadHourMinPrompts &&
            input.history.acceptRateThisHour < deadHourMaxAcceptRate

    /**
     * Interpretable 0..1 receptivity score over the normalized feature vector.
     * MUST stay in sync with `rules_baseline_score()` in ml/train.py.
     */
    fun score(f: FloatArray): Float {
        val still = f[IDX_ACTIVITY_FIRST] // one-hot slot 0 = STILL
        val raw = (
            0.40f * f[IDX_CONTINUOUS_SCREEN_ON] +
                0.20f * f[IDX_APP_SWITCH_COUNT] +
                0.15f * f[IDX_MINUTES_IN_ACTIVITY] * still +
                0.10f * f[IDX_DISTRACTING_RETURNS] +
                0.10f * f[IDX_COLD_OPENS] +
                0.05f * f[IDX_CHARGING] -
                0.30f * max(0f, 1f - f[IDX_MIN_SINCE_LAST_BREAK] * 4f) -
                0.20f * f[IDX_DISMISS_24H] -
                0.15f * f[IDX_LATE_NIGHT]
            )
        return raw.coerceIn(0f, 1f)
    }

    private fun isInQuietHours(hour: Int, start: Int, end: Int): Boolean =
        if (start <= end) hour in start until end // e.g. 13..15
        else hour >= start || hour < end          // wraps midnight, e.g. 22..7
}
