package com.reset.sense.ml

import com.reset.sense.ml.FeatureSchema.IDX_APP_SWITCH_COUNT
import com.reset.sense.ml.FeatureSchema.IDX_CONTINUOUS_SCREEN_ON
import kotlin.math.min

/** What the engine decided to do this tick. */
enum class PromptAction { SUPPRESS, SOFT_NUDGE, FULL_PROMPT }

/** Which break to offer, mapped from context (Stage-2 bandit replaces this). */
enum class BreakType { EYE_BREAK, STRETCH, BREATHING_RESET, RECOVERY_BREAK, WIND_DOWN, PASSIVE_REMINDER, NONE }

/** How assertive the host app wants the engine to be. */
enum class SenseMode(internal val fullThreshold: Float, internal val softThreshold: Float) {
    GENTLE(0.70f, 0.55f),
    BALANCED(0.60f, 0.40f),
    ASSERTIVE(0.50f, 0.32f),
}

/** Full decision record — also the row that gets logged for training. */
data class Decision(
    val action: PromptAction,
    val breakType: BreakType,
    val blendedScore: Float,
    val ruleScore: Float,
    val modelScore: Float?,
    val modelAlpha: Float,
    val gateReason: GateReason?,
    val reason: String,
)

/**
 * Orchestrates one evaluation tick: hard gates → rules score → model score →
 * blended decision → break-type mapping.
 *
 * Blend: `final = α·model + (1−α)·rules`, with α ramping from 0 to
 * [maxModelAlpha] as labeled outcomes accumulate — a fresh user runs almost
 * pure rules; a mature user runs mostly model. A `null` or failed model
 * degrades to pure rules; the engine itself never throws into the caller.
 */
class BreakDecisionEngine(
    private val model: AcceptanceModel?,
    private val rules: RulesEngine = RulesEngine(),
    private val mode: SenseMode = SenseMode.BALANCED,
    private val maxModelAlpha: Float = 0.7f,
    /** Labeled outcomes at which α reaches its max. */
    private val alphaRampOutcomes: Int = 200,
    /** Wind-down needs more certainty than a daytime nudge. The rules score
     *  already carries a −0.15 late-night penalty, so 0.55 here is an
     *  effective daytime-equivalent bar of ~0.70 — above BALANCED's full
     *  threshold without double-penalizing the night context. */
    private val windDownThreshold: Float = 0.55f,
) {

    fun decide(input: DecisionInput): Decision {
        val gate = rules.gate(input)
        if (gate is GateResult.Suppressed) {
            return Decision(
                action = PromptAction.SUPPRESS,
                breakType = BreakType.NONE,
                blendedScore = 0f,
                ruleScore = 0f,
                modelScore = null,
                modelAlpha = 0f,
                gateReason = gate.reason,
                reason = "hard gate: ${gate.reason}",
            )
        }

        val features = FeatureBuilder.build(input)
        val ruleScore = rules.score(features)

        // Model failure is never fatal — degrade to rules and keep serving.
        val modelScore = model?.let {
            runCatching { it.predict(features) }.getOrNull()
        }

        val alpha = if (modelScore != null) {
            maxModelAlpha * min(1f, input.history.labeledOutcomeCount / alphaRampOutcomes.toFloat())
        } else 0f
        val blended = alpha * (modelScore ?: 0f) + (1f - alpha) * ruleScore

        // Quiet hours + demonstrably awake: the only permitted outcome is one
        // silent, sleep-framed nudge — never a full/heads-up prompt, never a
        // stretch/eye-break, and only above the stricter threshold.
        if (gate is GateResult.WindDownOnly) {
            val prompt = blended >= windDownThreshold
            return Decision(
                action = if (prompt) PromptAction.SOFT_NUDGE else PromptAction.SUPPRESS,
                breakType = if (prompt) BreakType.WIND_DOWN else BreakType.NONE,
                blendedScore = blended,
                ruleScore = ruleScore,
                modelScore = modelScore,
                modelAlpha = alpha,
                gateReason = null,
                reason = "wind_down blended=%.2f threshold=%.2f".format(blended, windDownThreshold),
            )
        }

        val action = when {
            blended >= mode.fullThreshold -> PromptAction.FULL_PROMPT
            blended >= mode.softThreshold -> PromptAction.SOFT_NUDGE
            else -> PromptAction.SUPPRESS
        }
        val breakType = if (action == PromptAction.SUPPRESS) BreakType.NONE else mapBreakType(input, features)

        return Decision(
            action = action,
            breakType = breakType,
            blendedScore = blended,
            ruleScore = ruleScore,
            modelScore = modelScore,
            modelAlpha = alpha,
            gateReason = null,
            reason = buildString {
                append("blended=%.2f (rules=%.2f".format(blended, ruleScore))
                if (modelScore != null) append(", model=%.2f, α=%.2f".format(modelScore, alpha))
                append(") mode=$mode")
            },
        )
    }

    /** Context → break type. Priority order matters: safety, then circadian, then load. */
    private fun mapBreakType(input: DecisionInput, f: FloatArray): BreakType = when {
        input.device.activityState == ActivityState.ON_FOOT -> BreakType.PASSIVE_REMINDER
        input.time.isLateNight -> BreakType.WIND_DOWN
        f[IDX_APP_SWITCH_COUNT] >= 0.5f -> BreakType.BREATHING_RESET // fragmented attention
        f[IDX_CONTINUOUS_SCREEN_ON] >= 0.75f &&
            input.device.activityState == ActivityState.STILL &&
            input.device.minutesInCurrentActivity >= 90 -> BreakType.RECOVERY_BREAK // overused + long-stationary
        f[IDX_CONTINUOUS_SCREEN_ON] >= 0.5f -> BreakType.STRETCH // long focus
        else -> BreakType.EYE_BREAK
    }
}
