package com.reset.sense.store

import com.reset.sense.ml.Decision
import com.reset.sense.ml.FeatureSchema
import com.reset.sense.ml.ResponseHistory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The learning-loop ledger: log decision → observe outcome → serve
 * [ResponseHistory] back to the engine. Every number the personalization
 * block sees is derived from this table.
 */
@Singleton
class SenseDecisionLog @Inject constructor(
    private val dao: DecisionLogDao,
) {

    // ------------------------------------------------------------ writes

    /**
     * Logs one evaluation tick. [features] must be non-null exactly when the
     * prompt was shown — that row later becomes a labeled training example.
     */
    suspend fun logDecision(
        decision: Decision,
        features: FloatArray?,
        nowMs: Long,
        hourOfDay: Int,
    ): Long = dao.insert(
        DecisionEntity(
            timestampMs = nowMs,
            hourOfDay = hourOfDay,
            action = decision.action.name,
            breakType = decision.breakType.name,
            blendedScore = decision.blendedScore,
            ruleScore = decision.ruleScore,
            modelScore = decision.modelScore,
            modelAlpha = decision.modelAlpha,
            gateReason = decision.gateReason?.name,
            featuresCsv = features?.joinToString(","),
            schemaVersion = FeatureSchema.SCHEMA_VERSION,
            appliedThreshold = decision.appliedThreshold,
            explorationEpsilon = decision.explorationEpsilon,
            explored = decision.explored,
        ),
    )

    suspend fun recordAccepted(decisionId: Long, nowMs: Long) {
        val row = dao.byId(decisionId) ?: return
        // First response wins — with one exception: the tap itself opens the
        // app, so host onResume can write the inferred OPENED_APP a moment
        // before the receiver's ACCEPTED lands. The explicit tap is the more
        // specific fact and refines the inference.
        if (row.outcomeEnum.isTerminal && row.outcomeEnum != PromptOutcome.OPENED_APP) return
        val delaySec = ((nowMs - row.timestampMs) / 1000L).toInt().coerceAtLeast(0)
        dao.updateOutcome(decisionId, PromptOutcome.ACCEPTED.name, nowMs, delaySec)
    }

    /** Dismissal splits into fast (hard negative) vs slow by [FAST_DISMISS_SEC]. */
    suspend fun recordDismissed(decisionId: Long, nowMs: Long) =
        recordTerminal(decisionId, nowMs) { delaySec ->
            if (delaySec <= FAST_DISMISS_SEC) PromptOutcome.DISMISSED_FAST
            else PromptOutcome.DISMISSED_SLOW
        }

    suspend fun recordSnoozed(decisionId: Long, nowMs: Long) =
        recordTerminal(decisionId, nowMs) { _ -> PromptOutcome.SNOOZED }

    /**
     * Host-reported completion (the user finished the break). Upgrades an
     * ACCEPTED row; the original response delay is preserved.
     */
    suspend fun recordCompleted(decisionId: Long, nowMs: Long) {
        val row = dao.byId(decisionId) ?: return
        dao.updateOutcome(
            id = decisionId,
            outcome = PromptOutcome.COMPLETED.name,
            atMs = nowMs,
            delaySec = row.responseDelaySec
                ?: ((nowMs - row.timestampMs) / 1000L).toInt(),
        )
    }

    /** Race guard: the notification was withheld after the decision was
     *  logged (host app reached the foreground first). The row drops out of
     *  every cap, rate, and training query — the user never saw anything. */
    suspend fun recordNotShown(decisionId: Long, nowMs: Long) {
        val row = dao.byId(decisionId) ?: return
        if (row.outcomeEnum.isTerminal) return
        dao.updateOutcome(decisionId, PromptOutcome.NOT_SHOWN.name, nowMs, null)
    }

    /** Marks stale PENDING prompts as IGNORED. Call from the periodic tick. */
    suspend fun sweepIgnored(nowMs: Long, timeoutMs: Long = IGNORE_TIMEOUT_MS) {
        for (id in dao.pendingOlderThan(nowMs - timeoutMs)) {
            dao.updateOutcome(id, PromptOutcome.IGNORED.name, nowMs, null)
        }
    }

    /** WIND_DOWN prompts shown since [sinceMs] — the nightly-cap gate input. */
    suspend fun windDownShownSince(sinceMs: Long): Int =
        dao.shownSince(sinceMs).count { it.breakType == WIND_DOWN_TYPE }

    /**
     * Wind-down success signal. A wind-down's true positive is not a tap —
     * it's the screen going dark soon after the nudge. If the most recent
     * screen-off landed within [WIND_DOWN_SUCCESS_WINDOW_MS] after a pending
     * WIND_DOWN prompt, upgrade it to COMPLETED. Run BEFORE sweepIgnored so
     * successes aren't first swept into IGNORED.
     */
    suspend fun resolveWindDownOutcomes(nowMs: Long, lastScreenOffMs: Long?) {
        if (lastScreenOffMs == null) return
        val pending = dao.shownSince(nowMs - ONE_DAY_MS).filter {
            it.breakType == WIND_DOWN_TYPE && !it.outcomeEnum.isTerminal
        }
        for (row in pending) {
            val delayMs = lastScreenOffMs - row.timestampMs
            if (delayMs in 0..WIND_DOWN_SUCCESS_WINDOW_MS) {
                dao.updateOutcome(
                    id = row.id,
                    outcome = PromptOutcome.COMPLETED.name,
                    atMs = lastScreenOffMs,
                    delaySec = (delayMs / 1000L).toInt(),
                )
            }
        }
    }

    /**
     * App-open attribution (CLICK_IS_SUCCESS launch policy): the host reached
     * the foreground while a shown prompt was still pending and recent —
     * credit the prompt with [PromptOutcome.OPENED_APP]. The user came; the
     * tap is just one of the doors. Call from the host's onResume.
     *
     * Explicit responses (tap / dismiss / snooze) always win over this
     * inference, and WIND_DOWN prompts are excluded: their success is the
     * screen going DARK — an app open at 1 AM is the opposite of that.
     */
    suspend fun resolveAppOpenOutcomes(nowMs: Long) {
        if (!PromptOutcome.CLICK_IS_SUCCESS) return
        for (row in dao.shownSince(nowMs - APP_OPEN_ATTRIBUTION_MS)) {
            if (row.outcomeEnum.isTerminal || row.breakType == WIND_DOWN_TYPE) continue
            dao.updateOutcome(
                id = row.id,
                outcome = PromptOutcome.OPENED_APP.name,
                atMs = nowMs,
                delaySec = ((nowMs - row.timestampMs) / 1000L).toInt(),
            )
        }
    }

    /** Retention sweep — the log is training data, not a diary. */
    suspend fun purgeOldRows(nowMs: Long, retentionMs: Long = RETENTION_MS) {
        dao.purgeOlderThan(nowMs - retentionMs)
    }

    private suspend fun recordTerminal(
        decisionId: Long,
        nowMs: Long,
        classify: (delaySec: Int) -> PromptOutcome,
    ) {
        val row = dao.byId(decisionId) ?: return
        if (row.outcomeEnum.isTerminal) return // first response wins
        val delaySec = ((nowMs - row.timestampMs) / 1000L).toInt().coerceAtLeast(0)
        dao.updateOutcome(decisionId, classify(delaySec).name, nowMs, delaySec)
    }

    // ------------------------------------------------------------ reads

    /** Prompts shown since local midnight — the daily-cap gate input. */
    suspend fun promptsShownToday(localMidnightMs: Long): Int =
        dao.shownSince(localMidnightMs).size

    /**
     * Assembles the personalization block for the current tick. Cold-start
     * rules: rates default to 0.5 ("no evidence"), delay to 60s, and
     * minutes-since-break saturates when no break was ever completed —
     * matching the trainer's priors exactly.
     */
    suspend fun responseHistory(nowMs: Long, hourOfDay: Int): ResponseHistory {
        val monthRows = dao.shownSince(nowMs - THIRTY_DAYS_MS)
        val weekRows = monthRows.filter { it.timestampMs >= nowMs - SEVEN_DAYS_MS }
        val dayOutcomes = monthRows.filter { (it.outcomeAtMs ?: 0) >= nowMs - ONE_DAY_MS }

        val weekLabeled = weekRows.filter { it.outcomeEnum.isTerminal }
        val hourRows = monthRows.filter { it.hourOfDay == hourOfDay }
        val hourLabeled = hourRows.filter { it.outcomeEnum.isTerminal }
        val monthLabeled = monthRows.filter { it.outcomeEnum.isTerminal }
        val delays = monthLabeled.mapNotNull { it.responseDelaySec }

        // Empirical-Bayes shrinkage (SENSE_ML.md §8.6 stage 2): sparse buckets
        // read near their prior instead of swinging on a handful of outcomes.
        // The per-hour rate shrinks toward the user's overall rate, which
        // itself shrinks toward the population prior — "2 of 3 accepted at
        // 14:00" reads ~0.55, not 0.67.
        val overallAcceptRate = shrunkRate(
            successes = monthLabeled.count { it.outcomeEnum.isPositive },
            n = monthLabeled.size,
            prior = COLD_START_RATE,
        )

        return ResponseHistory(
            // "Completed" here means SUCCESS under the current policy — a tap
            // while PromptOutcome.CLICK_IS_SUCCESS is on (cooldown starts on
            // tap; earned trust counts taps).
            minutesSinceLastCompletedBreak = dao.lastSuccessAtMs(PromptOutcome.successNames())
                ?.let { ((nowMs - it) / 60_000L).toInt() } ?: Int.MAX_VALUE,
            dismissCount24h = dayOutcomes.count {
                it.outcomeEnum == PromptOutcome.DISMISSED_FAST ||
                    it.outcomeEnum == PromptOutcome.DISMISSED_SLOW
            },
            snoozeCount24h = dayOutcomes.count { it.outcomeEnum == PromptOutcome.SNOOZED },
            acceptRate7d = shrunkRate(
                successes = weekLabeled.count { it.outcomeEnum.isPositive },
                n = weekLabeled.size,
                prior = overallAcceptRate,
            ),
            acceptRateThisHour = shrunkRate(
                successes = hourLabeled.count { it.outcomeEnum.isPositive },
                n = hourLabeled.size,
                prior = overallAcceptRate,
            ),
            acceptRateThisHourRaw = rate(hourLabeled) { it.outcomeEnum.isPositive },
            avgResponseDelaySec = if (delays.isEmpty()) 60f else delays.average().toFloat(),
            breakCompletionRate = rate(monthLabeled) { it.outcomeEnum.isSuccess },
            labeledOutcomeCount = dao.labeledOutcomeCount(),
            promptsShownThisHourHistoric = hourRows.size,
        )
    }

    /**
     * Labeled (features, signed sample weight) pairs for on-device/offline
     * retraining. The weight comes from [PromptOutcome.trainingWeight] —
     * completion-graded, NOT tap-graded — so the trainer consumes the
     * product's reward philosophy directly; it cannot accidentally
     * re-derive a clicks objective from a boolean.
     */
    suspend fun trainingRows(): List<Pair<FloatArray, Float>> =
        dao.labeledTrainingRows().mapNotNull { row ->
            // Rows logged under an older feature schema are orphans: their
            // floats no longer align with the current pipeline's indices.
            // They still serve every outcome-stat read; they just never train.
            if (row.schemaVersion != FeatureSchema.SCHEMA_VERSION) return@mapNotNull null
            val weight = row.outcomeEnum.trainingWeight
            if (weight == 0f) return@mapNotNull null
            val features = row.featuresCsv
                ?.split(',')
                ?.mapNotNull { it.toFloatOrNull() }
                ?.toFloatArray()
                ?: return@mapNotNull null
            features to weight
        }

    private fun rate(rows: List<DecisionEntity>, predicate: (DecisionEntity) -> Boolean): Float =
        if (rows.isEmpty()) COLD_START_RATE else rows.count(predicate).toFloat() / rows.size

    /** Beta-style shrinkage: [PRIOR_STRENGTH] pseudo-observations at [prior]. */
    private fun shrunkRate(successes: Int, n: Int, prior: Float): Float =
        (successes + PRIOR_STRENGTH * prior) / (n + PRIOR_STRENGTH)

    private companion object {
        const val COLD_START_RATE = 0.5f
        const val PRIOR_STRENGTH = 5f
        const val FAST_DISMISS_SEC = 5
        const val IGNORE_TIMEOUT_MS = 30 * 60 * 1000L
        const val WIND_DOWN_TYPE = "WIND_DOWN" // BreakType.WIND_DOWN.name
        const val WIND_DOWN_SUCCESS_WINDOW_MS = 10 * 60 * 1000L
        const val APP_OPEN_ATTRIBUTION_MS = 10 * 60 * 1000L
        const val ONE_DAY_MS = 24 * 60 * 60 * 1000L
        const val SEVEN_DAYS_MS = 7 * ONE_DAY_MS
        const val THIRTY_DAYS_MS = 30 * ONE_DAY_MS
        const val RETENTION_MS = 90 * ONE_DAY_MS
    }
}
