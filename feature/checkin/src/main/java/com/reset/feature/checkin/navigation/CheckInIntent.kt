package com.reset.feature.checkin.navigation

import com.reset.feature.checkin.ChipId
import com.reset.feature.checkin.InputMode

/** All user/UI intents for the Check In feature. */
sealed interface CheckInIntent {
    data class SelectChip(val chipId: ChipId) : CheckInIntent

    // ── Text / voice input ────────────────────────────────────
    data class SetInputMode(val mode: InputMode) : CheckInIntent
    data class ChatTextChanged(val text: String) : CheckInIntent

    /** Classify the current [com.reset.feature.checkin.CheckInState.chatText] → need → offer,
     *  or crisis/no-match escapes. */
    data object SendChat : CheckInIntent

    /** Voice mic tap — deferred; surfaces a "coming soon" nudge for now. */
    data object MicTap : CheckInIntent

    /** Follow-up answers: [AnswerWoundUp]/[AnswerWornOut] answer WOUND_UP's clarifier
     *  (worn-out reroutes to DRAINED); [AnswerNeckShoulders]/[AnswerWristsHands] answer
     *  BODY_TENSION's (wrists routes to HAND_STRAIN). */
    data object AnswerWoundUp : CheckInIntent
    data object AnswerWornOut : CheckInIntent
    data object AnswerNeckShoulders : CheckInIntent
    data object AnswerWristsHands : CheckInIntent

    data object StartOffered : CheckInIntent

    /** "Not it?" — swaps the offer to the alternate; logs the swap. */
    data object TryAlternate : CheckInIntent

    data object OpenSupportResources : CheckInIntent

    /** "Just browsing" — leaves Check In for the existing Sessions tab. */
    data object JustBrowsing : CheckInIntent

    data object HandleBackPress : CheckInIntent
}
