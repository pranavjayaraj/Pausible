package com.reset.feature.checkin

import androidx.compose.runtime.Stable
import com.reset.model.domain.checkin.InputMethod
import com.reset.model.domain.checkin.NeedState

/**
 * Immutable UI state for the Check In feature: a conversational input screen (Sprout + speech
 * bubble, Text/Voice toggle, quick-chip pills + free text) → a brief "finding" beat → offer,
 * with the binary follow-up in between when a need has one. [step] is the current sub-screen,
 * an intra-feature transition; leaving the experience goes through the injected Navigator.
 * Build new state only with [getDefault] + `copy`.
 */
@Stable
data class CheckInState(
    val step: CheckInStep = CheckInStep.Input,
    val inputMode: InputMode = InputMode.Text,
    /** Current free-text draft (Text mode). Never logged — only the derived need is. */
    val chatText: String = "",
    /** Which quick chips to render — night/stillness swaps already applied. */
    val grid: List<ChipId> = ChipId.BASE_GRID,
    /** Sense-suggested chip to glow — never auto-selected, independent of [grid]'s swaps. */
    val suggestedChip: ChipId? = null,
    val isNight: Boolean = false,
    /** How the in-flight need was produced (chip tap vs typed text) — threaded to the
     *  propensity log so text vs chip selection can be compared. Survives the follow-up. */
    val originInputMethod: InputMethod = InputMethod.CHIP,
    /** True while the Tier-2 embedding model's Play Asset Delivery pack is downloading in
     *  the background — a subtle, non-blocking cue ("getting smarter for next time"), never
     *  a spinner on Send. This Send's input already fell back to Tier-1 + chips. */
    val embedModelFetching: Boolean = false,
) {
    val sendEnabled: Boolean get() = chatText.isNotBlank()

    companion object {
        fun getDefault() = CheckInState()
    }
}

/** Text or Voice input. Voice is deferred — its panel shows a "coming soon" state. */
enum class InputMode { Text, Voice }

/** Which Check In sub-screen is showing. */
sealed interface CheckInStep {
    /** The conversational input screen (chips + text). */
    data object Input : CheckInStep

    /** The single binary clarifier — only [NeedState.WOUND_UP] and
     *  [NeedState.BODY_TENSION] have one; see [ChipId.hasFollowUp]. */
    data class FollowUp(val originNeedState: NeedState) : CheckInStep

    /** The brief "Finding your minute…" loading beat before [Offer]. */
    data object Finding : CheckInStep

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
