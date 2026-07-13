package com.reset.sense.delivery

import com.reset.sense.ml.Decision
import com.reset.sense.signals.SenseSnapshotProvider
import com.reset.sense.signals.SenseSnapshots
import javax.inject.Inject

/**
 * Seams the evaluator depends on. Interfaces (not the concrete Android
 * classes) so the orchestration logic stays JVM-unit-testable and the future
 * SDK extraction can swap implementations without touching the evaluator.
 */

interface SnapshotSource {
    suspend fun capture(nowMs: Long): SenseSnapshots
}

class AndroidSnapshotSource @Inject constructor(
    private val provider: SenseSnapshotProvider,
) : SnapshotSource {
    override suspend fun capture(nowMs: Long): SenseSnapshots = provider.capture(nowMs)
}

interface BreakPresenter {
    /** False when notifications are blocked (runtime permission / user toggle). */
    fun canPresent(): Boolean

    /**
     * Shows the prompt for [decision]; [decisionId] rides the PendingIntents.
     * @return false when the notification was withheld at the last moment
     * (host reached the foreground, or posting failed) — the caller must mark
     * the decision NOT_SHOWN so it never pollutes caps, rates, or training.
     */
    fun present(decision: Decision, decisionId: Long): Boolean

    fun dismissCurrent()
}

/** Is the host app itself currently on screen? Prompting someone who is
 *  already IN the app is the one interruption with zero possible upside —
 *  a good break window while the host is open belongs to an in-app surface
 *  (future receptivity banner), never to a notification. */
fun interface HostStateSource {
    fun isHostForeground(): Boolean
}

class AndroidHostStateSource @Inject constructor() : HostStateSource {
    override fun isHostForeground(): Boolean = runCatching {
        // currentState is read-only here; observer registration (which IS
        // main-thread-only) never happens from this path. Any surprise from
        // a worker thread degrades to "not foreground", i.e. normal delivery.
        androidx.lifecycle.ProcessLifecycleOwner.get().lifecycle.currentState
            .isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)
    }.getOrDefault(false)
}
