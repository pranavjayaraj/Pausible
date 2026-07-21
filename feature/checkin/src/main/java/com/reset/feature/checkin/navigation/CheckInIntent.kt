package com.reset.feature.checkin.navigation

import com.reset.feature.checkin.ChipId

/** All user/UI intents for the Check In feature. */
sealed interface CheckInIntent {
    data class SelectChip(val chipId: ChipId) : CheckInIntent

    /** Follow-up answers: [WoundUp]/[WornOut] answer WOUND_UP's clarifier (worn-out
     *  reroutes to DRAINED); [NeckShoulders]/[WristsHands] answer BODY_TENSION's (wrists
     *  routes to HAND_STRAIN). */
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
