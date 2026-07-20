package com.reset.app

import com.reset.feature.sessions.api.SessionDestination
import com.reset.model.domain.CelebrationEvent
import com.reset.model.domain.CelebrationStore
import com.reset.sense.ml.BreakType
import com.reset.sense.store.SenseDecisionLog
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * App-side glue between an accepted Sense prompt and the Session experience.
 *
 * Two responsibilities:
 *  1. Map the prompt's [BreakType] onto the session graph's typed route
 *     (and remember the decision id while the break runs).
 *  2. Observe [CelebrationStore] for BreakFinished — the session feature
 *     already dispatches it on every completed break — and upgrade the
 *     pending decision to COMPLETED. COMPLETED (not the tap) is the positive
 *     training label; abandoning mid-break leaves the honest ACCEPTED.
 *
 * Observation is non-consuming: the celebration UI still owns consume().
 */
@Singleton
class SenseBreakCoordinator @Inject constructor(
    private val decisionLog: SenseDecisionLog,
    private val celebrationStore: CelebrationStore,
) {

    private val pendingDecisionId = AtomicLong(NONE)

    /** Call once from Application.onCreate with an app-lifetime scope. */
    fun startObserving(scope: CoroutineScope) {
        scope.launch {
            celebrationStore.pending.collect { event ->
                if (event is CelebrationEvent.BreakFinished) {
                    val id = pendingDecisionId.getAndSet(NONE)
                    if (id != NONE) decisionLog.recordCompleted(id, System.currentTimeMillis())
                }
            }
        }
    }

    /**
     * Host reached the foreground. Under the CLICK_IS_SUCCESS launch policy a
     * recent pending prompt is credited as OPENED_APP retention — the
     * notification worked even without a tap. Call from MainActivity.onResume.
     */
    suspend fun onHostOpened() {
        decisionLog.resolveAppOpenOutcomes(System.currentTimeMillis())
    }

    /** Maps the prompt's break type onto a typed session route; arms completion tracking. */
    fun destinationFor(breakTypeName: String?, decisionId: Long): SessionDestination {
        pendingDecisionId.set(decisionId)
        val breakType = breakTypeName?.let { name ->
            runCatching { BreakType.valueOf(name) }.getOrNull()
        } ?: BreakType.BREATHING_RESET
        return when (breakType) {
            BreakType.STRETCH,
            BreakType.RECOVERY_BREAK -> SessionDestination(
                mode = SessionDestination.MODE_BREAK,
                breakKind = SessionDestination.KIND_STRETCH,
                durationMin = if (breakType == BreakType.RECOVERY_BREAK) 3 else 1,
            )
            BreakType.WIND_DOWN -> SessionDestination(
                mode = SessionDestination.MODE_BREAK,
                breakKind = SessionDestination.KIND_BREATHING,
                durationMin = 3,
                paceSec = 6, // slower cadence for the wind-down
            )
            BreakType.EYE_BREAK -> SessionDestination(
                mode = SessionDestination.MODE_BREAK,
                breakKind = SessionDestination.KIND_MEDITATE,
                durationMin = 1,
            )
            BreakType.BREATHING_RESET,
            BreakType.PASSIVE_REMINDER,
            BreakType.NONE -> SessionDestination(
                mode = SessionDestination.MODE_BREAK,
                breakKind = SessionDestination.KIND_BREATHING,
                durationMin = 1,
            )
        }
    }

    private companion object {
        const val NONE = -1L
    }
}
