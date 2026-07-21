package com.reset.feature.sessions.content

import com.reset.model.domain.checkin.GateReason
import com.reset.model.domain.checkin.SelectionContext

/**
 * Filters a [SessionScript] against real-world availability — requirements and the night
 * veto never rank a shortlist, they only exclude from it (see [SessionSelector] for ranking
 * and the separate, softer freshness tiebreak).
 */
object SessionRequirementResolver {

    /** Null when [script] can be offered right now; otherwise why not, for the propensity
     *  trail. Checked in a fixed order so a script failing multiple gates reports the first. */
    fun gate(script: SessionScript, context: SelectionContext): GateReason? = when {
        !requirementsMet(script, context) -> GateReason.REQUIREMENT_UNMET
        isNightVetoed(script, context) -> GateReason.NIGHT_VETOED
        isFrequencyCapped(script, context) -> GateReason.FREQUENCY_CAPPED
        else -> null
    }

    fun isServable(script: SessionScript, context: SelectionContext): Boolean =
        gate(script, context) == null

    private fun requirementsMet(script: SessionScript, context: SelectionContext): Boolean =
        script.requirements.all { requirement ->
            when (requirement) {
                Requirement.AUDIO -> context.audioAvailable
                Requirement.STAIRS -> context.stairsAvailable
                Requirement.MOVE_SPACE -> context.moveSpaceAvailable
                Requirement.WATER_ACCESS -> context.waterAccessAvailable
                Requirement.HAS_CLOSE_PERSON -> context.hasClosePerson
            }
        }

    /** Movement/activation drops out after quiet hours start — every [Modality.MOVE] session
     *  implicitly, plus any session explicitly flagged [SessionScript.nightVetoed] (e.g. The
     *  Plunge's cold water, which is jarring despite its calm-modality tag). */
    private fun isNightVetoed(script: SessionScript, context: SelectionContext): Boolean =
        context.isNight && (script.modality == Modality.MOVE || script.nightVetoed)

    /** [SessionScript.minHoursBetween] is a hard precondition (a self-imposed frequency cap),
     *  not a soft tiebreak — unlike the general one-hour freshness demotion in [SessionSelector]. */
    private fun isFrequencyCapped(script: SessionScript, context: SelectionContext): Boolean {
        val capHours = script.minHoursBetween ?: return false
        val lastCompletedAt = context.recentCompletions
            .filter { it.scriptId == script.id }
            .maxOfOrNull { it.completedAtMs } ?: return false
        val elapsedHours = (context.nowMillis - lastCompletedAt) / MS_PER_HOUR
        return elapsedHours < capHours
    }

    private const val MS_PER_HOUR = 3_600_000L
}
