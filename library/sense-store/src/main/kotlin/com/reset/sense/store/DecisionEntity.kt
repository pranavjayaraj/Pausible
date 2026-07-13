package com.reset.sense.store

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.reset.sense.ml.FeatureSchema

/**
 * One evaluation tick's decision + (for shown prompts) its outcome. This
 * table is simultaneously:
 *  - the source of [com.reset.sense.ml.ResponseHistory] (personalization block),
 *  - the gate inputs (prompts shown today, dead hours),
 *  - the training dataset (featuresCsv + outcome = one labeled row).
 *
 * Suppressed ticks are logged too (for diagnostics and prompts/day metrics)
 * but carry no features and never become training rows — only shown-prompt
 * windows are labeled, per the no-heuristic-labels rule.
 */
@Entity(
    tableName = "decision_log",
    indices = [Index("timestampMs"), Index("outcome")],
)
data class DecisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    /** Local hour 0..23 at decision time — drives per-hour acceptance stats. */
    val hourOfDay: Int,
    /** PromptAction name: SUPPRESS / SOFT_NUDGE / FULL_PROMPT. */
    val action: String,
    /** BreakType name. */
    val breakType: String,
    val blendedScore: Float,
    val ruleScore: Float,
    val modelScore: Float?,
    val modelAlpha: Float,
    /** GateReason name when suppressed by a hard gate, else null. */
    val gateReason: String?,
    /** The 34-float model input, comma-joined. Only for shown prompts. */
    val featuresCsv: String?,
    /**
     * FeatureSchema.SCHEMA_VERSION that produced [featuresCsv]. Training
     * export filters on it — after a schema bump, old rows would otherwise
     * mix 34-float and 35-float vectors into one ragged, misaligned dataset.
     * (Outcome-stat reads ignore this; they never touch the features.)
     */
    val schemaVersion: Int = FeatureSchema.SCHEMA_VERSION,
    /**
     * Propensity trail for off-policy evaluation: the threshold this decision
     * was judged against, the exploration ε in effect (0 while dormant), and
     * whether this prompt exists only because the ε draw fired. Together with
     * [blendedScore] these reconstruct P(show | context) for every historical
     * decision — data that cannot be back-filled later.
     */
    val appliedThreshold: Float? = null,
    val explorationEpsilon: Float = 0f,
    val explored: Boolean = false,
    /** PromptOutcome name. PENDING until a response or the ignore sweep. */
    val outcome: String = PromptOutcome.PENDING.name,
    val outcomeAtMs: Long? = null,
    val responseDelaySec: Int? = null,
) {
    val wasShown: Boolean get() = action != "SUPPRESS"
    val outcomeEnum: PromptOutcome get() = PromptOutcome.valueOf(outcome)
}
