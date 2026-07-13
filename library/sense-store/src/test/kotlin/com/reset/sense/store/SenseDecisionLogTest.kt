package com.reset.sense.store

import com.reset.sense.ml.BreakType
import com.reset.sense.ml.Decision
import com.reset.sense.ml.GateReason
import com.reset.sense.ml.PromptAction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SenseDecisionLogTest {

    private val dao = FakeDecisionLogDao()
    private val log = SenseDecisionLog(dao)

    private val t0 = 1_000_000_000_000L // arbitrary epoch base
    private val minute = 60_000L
    private val hour = 60 * minute
    private val day = 24 * hour

    private fun shownDecision() = Decision(
        action = PromptAction.FULL_PROMPT,
        breakType = BreakType.STRETCH,
        blendedScore = 0.7f,
        ruleScore = 0.6f,
        modelScore = 0.8f,
        modelAlpha = 0.5f,
        gateReason = null,
        reason = "test",
    )

    private fun suppressedDecision() = shownDecision().copy(
        action = PromptAction.SUPPRESS, breakType = BreakType.NONE, gateReason = GateReason.DAILY_CAP,
    )

    private fun windDownDecision() = shownDecision().copy(
        action = PromptAction.SOFT_NUDGE, breakType = BreakType.WIND_DOWN,
    )

    private val features = FloatArray(34) { 0.5f }

    // ------------------------------------------------------------ outcomes

    @Test
    fun `fast dismissal is classified as hard negative`() = runTest {
        val id = log.logDecision(shownDecision(), features, t0, hourOfDay = 15)
        log.recordDismissed(id, t0 + 3_000) // 3s
        assertEquals(PromptOutcome.DISMISSED_FAST.name, dao.byId(id)!!.outcome)
        assertEquals(3, dao.byId(id)!!.responseDelaySec)
    }

    @Test
    fun `slow dismissal is the weak negative`() = runTest {
        val id = log.logDecision(shownDecision(), features, t0, 15)
        log.recordDismissed(id, t0 + 40_000) // 40s
        assertEquals(PromptOutcome.DISMISSED_SLOW.name, dao.byId(id)!!.outcome)
    }

    @Test
    fun `completion upgrades an accepted prompt and keeps original delay`() = runTest {
        val id = log.logDecision(shownDecision(), features, t0, 15)
        log.recordAccepted(id, t0 + 10_000)
        log.recordCompleted(id, t0 + 70_000)
        val row = dao.byId(id)!!
        assertEquals(PromptOutcome.COMPLETED.name, row.outcome)
        assertEquals("tap delay preserved, not completion delay", 10, row.responseDelaySec)
    }

    @Test
    fun `first terminal response wins`() = runTest {
        val id = log.logDecision(shownDecision(), features, t0, 15)
        log.recordDismissed(id, t0 + 2_000)
        log.recordAccepted(id, t0 + 9_000) // late tap after dismissal — ignored
        assertEquals(PromptOutcome.DISMISSED_FAST.name, dao.byId(id)!!.outcome)
    }

    @Test
    fun `sweep marks stale pending prompts as ignored`() = runTest {
        val stale = log.logDecision(shownDecision(), features, t0, 15)
        val fresh = log.logDecision(shownDecision(), features, t0 + 50 * minute, 15)
        log.sweepIgnored(nowMs = t0 + 60 * minute) // timeout = 30 min
        assertEquals(PromptOutcome.IGNORED.name, dao.byId(stale)!!.outcome)
        assertEquals(PromptOutcome.PENDING.name, dao.byId(fresh)!!.outcome)
    }

    // ------------------------------------------------------------ history assembly

    @Test
    fun `cold start returns trainer priors`() = runTest {
        val history = log.responseHistory(t0, hourOfDay = 15)
        assertEquals(Int.MAX_VALUE, history.minutesSinceLastCompletedBreak)
        assertEquals(0.5f, history.acceptRate7d, 0f)
        assertEquals(0.5f, history.acceptRateThisHour, 0f)
        assertEquals(0.5f, history.breakCompletionRate, 0f)
        assertEquals(60f, history.avgResponseDelaySec, 0f)
        assertEquals(0, history.labeledOutcomeCount)
    }

    @Test
    fun `accept rates split by week and hour buckets`() = runTest {
        // 2 prompts at hour 15: 1 accepted, 1 fast-dismissed.
        val a = log.logDecision(shownDecision(), features, t0, 15)
        log.recordAccepted(a, t0 + 10_000)
        val b = log.logDecision(shownDecision(), features, t0 + hour, 15)
        log.recordDismissed(b, t0 + hour + 2_000)
        // 1 prompt at hour 9: accepted.
        val c = log.logDecision(shownDecision(), features, t0 + 2 * hour, 9)
        log.recordAccepted(c, t0 + 2 * hour + 5_000)

        val now = t0 + 3 * hour
        val h15 = log.responseHistory(now, hourOfDay = 15)
        assertEquals(0.5f, h15.acceptRateThisHour, 1e-6f)
        assertEquals("2 of 3 accepted overall this week", 2f / 3f, h15.acceptRate7d, 1e-6f)
        assertEquals(2, h15.promptsShownThisHourHistoric)
        assertEquals(3, h15.labeledOutcomeCount)

        val h9 = log.responseHistory(now, hourOfDay = 9)
        assertEquals(1.0f, h9.acceptRateThisHour, 1e-6f)
    }

    @Test
    fun `dismiss and snooze counts are last-24h only`() = runTest {
        val old = log.logDecision(shownDecision(), features, t0 - 2 * day, 10)
        log.recordDismissed(old, t0 - 2 * day + 2_000)
        val recent = log.logDecision(shownDecision(), features, t0 - hour, 10)
        log.recordDismissed(recent, t0 - hour + 2_000)
        val snoozed = log.logDecision(shownDecision(), features, t0 - 2 * hour, 10)
        log.recordSnoozed(snoozed, t0 - 2 * hour + 8_000)

        val history = log.responseHistory(t0, 10)
        assertEquals(1, history.dismissCount24h)
        assertEquals(1, history.snoozeCount24h)
    }

    @Test
    fun `minutes since last completed break`() = runTest {
        val id = log.logDecision(shownDecision(), features, t0, 11)
        log.recordAccepted(id, t0 + 10_000)
        log.recordCompleted(id, t0 + 90_000)
        val history = log.responseHistory(t0 + 90_000 + 45 * minute, 12)
        assertEquals(45, history.minutesSinceLastCompletedBreak)
    }

    // ------------------------------------------------------------ gates & training

    @Test
    fun `prompts shown today ignores suppressed ticks`() = runTest {
        log.logDecision(shownDecision(), features, t0 + 1 * hour, 8)
        log.logDecision(suppressedDecision(), null, t0 + 2 * hour, 9)
        log.logDecision(shownDecision(), features, t0 + 3 * hour, 10)
        assertEquals(2, log.promptsShownToday(localMidnightMs = t0))
    }

    @Test
    fun `training rows are labeled shown prompts only`() = runTest {
        val labeled = log.logDecision(shownDecision(), features, t0, 15)
        log.recordAccepted(labeled, t0 + 5_000)
        log.logDecision(shownDecision(), features, t0 + hour, 15) // still PENDING
        log.logDecision(suppressedDecision(), null, t0 + 2 * hour, 15)

        val rows = log.trainingRows()
        assertEquals("only the labeled shown prompt exports", 1, rows.size)
        val (exportedFeatures, weight) = rows.single()
        assertEquals(34, exportedFeatures.size)
        assertEquals("a bare tap is faint praise, not a full positive", 0.2f, weight, 0f)
    }

    @Test
    fun `training weights encode completion over clicks`() = runTest {
        val completed = log.logDecision(shownDecision(), features, t0, 15)
        log.recordAccepted(completed, t0 + 5_000)
        log.recordCompleted(completed, t0 + 60_000)
        val fastDismissed = log.logDecision(shownDecision(), features, t0 + hour, 15)
        log.recordDismissed(fastDismissed, t0 + hour + 2_000)
        val snoozed = log.logDecision(shownDecision(), features, t0 + 2 * hour, 15)
        log.recordSnoozed(snoozed, t0 + 2 * hour + 8_000)

        val weights = log.trainingRows().map { (_, weight) -> weight }
        assertEquals(listOf(1.0f, -1.0f, 0.1f), weights)

        // The reward philosophy, as assertions: completing beats tapping,
        // and a fast swat is the strongest negative we log.
        assertTrue(PromptOutcome.COMPLETED.trainingWeight > PromptOutcome.ACCEPTED.trainingWeight)
        assertTrue(PromptOutcome.DISMISSED_FAST.trainingWeight < PromptOutcome.DISMISSED_SLOW.trainingWeight)
        assertTrue(PromptOutcome.SNOOZED.trainingWeight > 0f)
        assertEquals("PENDING must never train", 0f, PromptOutcome.PENDING.trainingWeight, 0f)
    }

    // ------------------------------------------------------------ wind-down

    @Test
    fun `screen off within ten minutes upgrades wind-down to completed`() = runTest {
        val id = log.logDecision(windDownDecision(), features, t0, 1)
        log.resolveWindDownOutcomes(nowMs = t0 + 15 * minute, lastScreenOffMs = t0 + 5 * minute)
        val row = dao.byId(id)!!
        assertEquals(PromptOutcome.COMPLETED.name, row.outcome)
        assertEquals("delay = prompt → screen-off", 300, row.responseDelaySec)
    }

    @Test
    fun `screen off outside the window leaves the prompt pending`() = runTest {
        val id = log.logDecision(windDownDecision(), features, t0, 1)
        // Screen-off 25 min later — user kept scrolling; not a success.
        log.resolveWindDownOutcomes(nowMs = t0 + 26 * minute, lastScreenOffMs = t0 + 25 * minute)
        assertEquals(PromptOutcome.PENDING.name, dao.byId(id)!!.outcome)
        // Screen-off BEFORE the prompt never counts either.
        log.resolveWindDownOutcomes(nowMs = t0 + 26 * minute, lastScreenOffMs = t0 - minute)
        assertEquals(PromptOutcome.PENDING.name, dao.byId(id)!!.outcome)
    }

    @Test
    fun `resolution never overwrites a terminal outcome and skips non-wind-down rows`() = runTest {
        val dismissed = log.logDecision(windDownDecision(), features, t0, 1)
        log.recordDismissed(dismissed, t0 + 2_000)
        val stretch = log.logDecision(shownDecision(), features, t0, 1)
        log.resolveWindDownOutcomes(nowMs = t0 + 6 * minute, lastScreenOffMs = t0 + 5 * minute)
        assertEquals(PromptOutcome.DISMISSED_FAST.name, dao.byId(dismissed)!!.outcome)
        assertEquals("STRETCH row untouched", PromptOutcome.PENDING.name, dao.byId(stretch)!!.outcome)
    }

    @Test
    fun `windDownShownSince counts only wind-down rows in range`() = runTest {
        log.logDecision(windDownDecision(), features, t0 - 2 * hour, 23) // before range
        log.logDecision(windDownDecision(), features, t0 + minute, 23)
        log.logDecision(shownDecision(), features, t0 + 2 * minute, 23) // stretch — not counted
        assertEquals(1, log.windDownShownSince(sinceMs = t0))
    }

    @Test
    fun `rows from an older feature schema never train but still feed stats`() = runTest {
        // A labeled row logged before a schema bump (v2 features, 2 floats).
        dao.rows += DecisionEntity(
            id = 999,
            timestampMs = t0,
            hourOfDay = 15,
            action = PromptAction.FULL_PROMPT.name,
            breakType = BreakType.STRETCH.name,
            blendedScore = 0.7f, ruleScore = 0.7f, modelScore = null,
            modelAlpha = 0f, gateReason = null,
            featuresCsv = "0.5,0.5",
            schemaVersion = 2,
            outcome = PromptOutcome.COMPLETED.name,
            outcomeAtMs = t0 + minute, responseDelaySec = 10,
        )
        val current = log.logDecision(shownDecision(), features, t0 + hour, 15)
        log.recordCompleted(current, t0 + hour + minute)

        val rows = log.trainingRows()
        assertEquals("orphaned schema row excluded from training", 1, rows.size)
        assertEquals(34, rows.single().first.size)

        // …but the old row still counts for outcome statistics.
        val history = log.responseHistory(t0 + 2 * hour, 15)
        assertEquals(2, history.labeledOutcomeCount)
    }

    @Test
    fun `purge respects retention`() = runTest {
        log.logDecision(shownDecision(), features, t0 - 100 * day, 15)
        log.logDecision(shownDecision(), features, t0 - 10 * day, 15)
        log.purgeOldRows(t0) // retention 90d
        assertEquals(1, dao.rows.size)
    }
}
