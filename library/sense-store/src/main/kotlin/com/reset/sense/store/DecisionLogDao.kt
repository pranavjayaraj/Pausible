package com.reset.sense.store

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * Deliberately narrow: fetch-by-window + point updates. History math lives in
 * [SenseDecisionLog] in plain Kotlin — row volume is tiny (≲100/day), and it
 * keeps the aggregation logic unit-testable against an in-memory fake.
 */
@Dao
interface DecisionLogDao {

    @Insert
    suspend fun insert(entity: DecisionEntity): Long

    @Query("SELECT * FROM decision_log WHERE id = :id")
    suspend fun byId(id: Long): DecisionEntity?

    /** Prompts actually shown to the user (any response) newer than [sinceMs],
     *  oldest first. NOT_SHOWN rows are withheld-at-the-last-moment races —
     *  the user saw nothing, so they count for no cap and no rate. */
    @Query(
        "SELECT * FROM decision_log " +
            "WHERE action != 'SUPPRESS' AND outcome != 'NOT_SHOWN' AND timestampMs >= :sinceMs " +
            "ORDER BY timestampMs ASC",
    )
    suspend fun shownSince(sinceMs: Long): List<DecisionEntity>

    @Query(
        "UPDATE decision_log SET outcome = :outcome, outcomeAtMs = :atMs, " +
            "responseDelaySec = :delaySec WHERE id = :id",
    )
    suspend fun updateOutcome(id: Long, outcome: String, atMs: Long, delaySec: Int?)

    /** Timestamp of the most recent completed break, if any. */
    @Query(
        "SELECT MAX(outcomeAtMs) FROM decision_log WHERE outcome = 'COMPLETED'",
    )
    suspend fun lastCompletedBreakAtMs(): Long?

    /** All-time count of terminal outcomes — drives the rules→model α ramp. */
    @Query(
        "SELECT COUNT(*) FROM decision_log " +
            "WHERE action != 'SUPPRESS' AND outcome NOT IN ('PENDING', 'NOT_SHOWN')",
    )
    suspend fun labeledOutcomeCount(): Int

    /** Shown prompts still PENDING and older than [cutoffMs] (ignore sweep). */
    @Query(
        "SELECT id FROM decision_log " +
            "WHERE action != 'SUPPRESS' AND outcome = 'PENDING' AND timestampMs < :cutoffMs",
    )
    suspend fun pendingOlderThan(cutoffMs: Long): List<Long>

    /** Labeled rows with features — the training export. */
    @Query(
        "SELECT * FROM decision_log " +
            "WHERE featuresCsv IS NOT NULL AND outcome NOT IN ('PENDING', 'NOT_SHOWN') " +
            "ORDER BY timestampMs ASC",
    )
    suspend fun labeledTrainingRows(): List<DecisionEntity>

    /** Retention: drop rows older than [cutoffMs]. */
    @Query("DELETE FROM decision_log WHERE timestampMs < :cutoffMs")
    suspend fun purgeOlderThan(cutoffMs: Long): Int
}
