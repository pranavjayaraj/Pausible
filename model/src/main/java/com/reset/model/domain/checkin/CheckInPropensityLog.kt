package com.reset.model.domain.checkin

/**
 * The future session-bandit's training data — captured now because it can't be back-filled
 * later, mirroring how [SenseDecisionLog][com.reset.sense.store.SenseDecisionLog] logs the
 * timing engine's own decisions. Every check-in logs the full shortlist trace, not just the
 * winner, so a later model can learn from what was filtered and why.
 */
interface CheckInPropensityLog {

    /** Logs one check-in's full selection trace. Returns the row id for later outcome updates. */
    suspend fun logSelection(
        needState: NeedState,
        inputMethod: InputMethod,
        selection: SessionSelection,
        nowMs: Long,
    ): Long

    /** The offered alternate was taken instead of the primary ("Not it?"). */
    suspend fun recordAlternateTaken(rowId: Long)

    /** The offered session (primary, or the alternate if swapped) was completed. */
    suspend fun recordCompleted(rowId: Long, scriptId: String, nowMs: Long)

    /** Completed sessions since [sinceMs] — the selector's freshness-tiebreak input. */
    suspend fun recentCompletions(sinceMs: Long): List<RecentCompletion>
}
