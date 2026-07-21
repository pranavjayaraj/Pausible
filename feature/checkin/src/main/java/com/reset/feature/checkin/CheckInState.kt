package com.reset.feature.checkin

import androidx.compose.runtime.Stable
import com.reset.model.domain.checkin.NeedState

/**
 * Immutable UI state for the Check In feature: chip grid → optional follow-up → offer.
 * [step] is the current sub-screen — an intra-feature transition; leaving the experience
 * (back from the grid, "just browsing") goes through the injected Navigator. Build new
 * state only with [getDefault] + `copy`.
 */
@Stable
data class CheckInState(
    val step: CheckInStep = CheckInStep.ChipGrid,
    /** Which 10 chips to render right now — night/stillness swaps already applied. */
    val grid: List<ChipId> = ChipId.BASE_GRID,
    /** Sense-suggested chip to glow — never auto-selected, independent of [grid]'s swaps. */
    val suggestedChip: ChipId? = null,
    val isNight: Boolean = false,
) {
    companion object {
        fun getDefault() = CheckInState()
    }
}

/** Which Check In sub-screen is showing. */
sealed interface CheckInStep {
    data object ChipGrid : CheckInStep

    /** The single binary clarifier — only [NeedState.WOUND_UP] and
     *  [NeedState.BODY_TENSION] have one; see [ChipId.hasFollowUp]. */
    data class FollowUp(val originNeedState: NeedState) : CheckInStep

    data class Offer(
        val needState: NeedState,
        val primary: OfferedSession,
        val alternate: OfferedSession,
        val showingAlternate: Boolean = false,
        /** The propensity log row for this offer — carried so completion/alternate-taken
         *  updates land on the right row without a second lookup. */
        val logRowId: Long,
    ) : CheckInStep {
        val current: OfferedSession get() = if (showingAlternate) alternate else primary
    }

    /** The quiet, always-available "Need support now?" resources screen. */
    data object SupportResources : CheckInStep
}

/** Just enough of a catalog session to render the offer screen — see
 *  [com.reset.model.domain.checkin.SessionPreview], which this mirrors 1:1. */
@Stable
data class OfferedSession(
    val scriptId: String,
    val displayName: String,
    val totalSec: Int,
    val whyThisOne: String,
    val evidenceTag: String,
)
