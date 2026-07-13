package com.reset.sense.store

/** In-memory DAO mirroring the SQL semantics for repository unit tests. */
class FakeDecisionLogDao : DecisionLogDao {

    val rows = mutableListOf<DecisionEntity>()
    private var nextId = 1L

    override suspend fun insert(entity: DecisionEntity): Long {
        val id = nextId++
        rows += entity.copy(id = id)
        return id
    }

    override suspend fun byId(id: Long): DecisionEntity? = rows.find { it.id == id }

    override suspend fun shownSince(sinceMs: Long): List<DecisionEntity> =
        rows.filter {
            it.wasShown && it.outcome != PromptOutcome.NOT_SHOWN.name && it.timestampMs >= sinceMs
        }.sortedBy { it.timestampMs }

    override suspend fun updateOutcome(id: Long, outcome: String, atMs: Long, delaySec: Int?) {
        val index = rows.indexOfFirst { it.id == id }
        if (index >= 0) {
            rows[index] = rows[index].copy(
                outcome = outcome, outcomeAtMs = atMs, responseDelaySec = delaySec,
            )
        }
    }

    override suspend fun lastCompletedBreakAtMs(): Long? =
        rows.filter { it.outcome == PromptOutcome.COMPLETED.name }
            .mapNotNull { it.outcomeAtMs }
            .maxOrNull()

    override suspend fun labeledOutcomeCount(): Int =
        rows.count {
            it.wasShown && it.outcome != PromptOutcome.PENDING.name &&
                it.outcome != PromptOutcome.NOT_SHOWN.name
        }

    override suspend fun pendingOlderThan(cutoffMs: Long): List<Long> =
        rows.filter { it.wasShown && it.outcome == PromptOutcome.PENDING.name && it.timestampMs < cutoffMs }
            .map { it.id }

    override suspend fun labeledTrainingRows(): List<DecisionEntity> =
        rows.filter {
            it.featuresCsv != null && it.outcome != PromptOutcome.PENDING.name &&
                it.outcome != PromptOutcome.NOT_SHOWN.name
        }.sortedBy { it.timestampMs }

    override suspend fun purgeOlderThan(cutoffMs: Long): Int {
        val before = rows.size
        rows.removeAll { it.timestampMs < cutoffMs }
        return before - rows.size
    }
}
