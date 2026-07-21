package com.reset.model.domain.closeperson

import kotlinx.coroutines.flow.Flow

/**
 * A close person the user has designated for "Reach Out"-style check-ins — send warmth, not
 * get validation. [contactUri] is a `tel:`/`smsto:` URI the app host can hand to
 * ACTION_DIAL/ACTION_SENDTO; building and launching that Intent from a fired
 * [com.reset.model.domain.model.ChimeKind]-style side effect is a later phase (see
 * SessionRoute's `LaunchActionRequested` attach point) — this repository only persists
 * whether one is set, which is all the HAS_CLOSE_PERSON-gated session selector needs today.
 */
data class ClosePerson(val name: String, val contactUri: String)

/** Persistence boundary for the designated close person. */
interface ClosePersonRepository {

    val closePerson: Flow<ClosePerson?>

    suspend fun setClosePerson(closePerson: ClosePerson)

    suspend fun clearClosePerson()
}
