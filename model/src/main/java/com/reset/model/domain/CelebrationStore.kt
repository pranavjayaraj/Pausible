package com.reset.model.domain

import kotlinx.coroutines.flow.StateFlow

/** Cross-feature one-shot moments worth celebrating. Pure data — no platform types. */
sealed interface CelebrationEvent {

    /** A mindful break just finished (in any feature); Home shows the banner. */
    data object BreakFinished : CelebrationEvent
}

/**
 * Seam that lets the Sessions feature hand Home its "Nice breather!" moment without a
 * navigation payload. Same pull semantics as [ReminderActionStore]: the event stays
 * pending until the consumer calls [consume] after acting, so it survives collector churn
 * and can never replay once handled.
 */
interface CelebrationStore {

    /** The celebration awaiting handling, or null when there is none. */
    val pending: StateFlow<CelebrationEvent?>

    fun dispatch(event: CelebrationEvent)

    /** Acknowledge [event] as handled; a no-op if a different event is now pending. */
    fun consume(event: CelebrationEvent)
}
