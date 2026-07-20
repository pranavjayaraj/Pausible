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
        assertEquals(0.5f, history.acceptRateThisHourRaw, 0f)
        assertEquals(0.5f, history.breakCompletionRate, 0f)
        assertEquals(60f, history.avgResponseDelaySec, 0f)
        assertEquals(0, history.labeledOutcomeCount)
    }

    @Test
    fun `accept rates split by week and hour buckets with EB shrinkage`() = runTest {
        // 2 prompts at hour 15: 1 accepted, 1 fast-dismissed.
        val a = log.logDecision(shownDecision(), features, t0, 15)
        log.recordAccepted(a, t0 + 10_000)
        val b = log.logDecision(shownDecision(), features, t0 + hour, 15)
        log.recordDismissed(b, t0 + hour + 2_000)
        // 1 prompt at hour 9: accepted.
        val c = log.logDecision(shownDecision(), features, t0 + 2 * hour, 9)
        log.recordAccepted(c, t0 + 2 * hour + 5_000)

        // Shrinkage (k = 5 pseudo-observations): overall = (2 + 2.5)/8 = 0.5625;
        // each bucket shrinks toward overall, overall toward the 0.5 prior.
        val now = t0 + 3 * hour
        val h15 = log.responseHistory(now, hourOfDay = 15)
        assertEquals("1 of 2: (1 + 5·0.5625)/7", 0.544643f, h15.acceptRateThisHour, 1e-4f)
        assertEquals("raw rate stays unshrunk for the dead-hour gate", 0.5f, h15.acceptRateThisHourRaw, 1e-6f)
        assertEquals("2 of 3: (2 + 5·0.5625)/8", 0.601563f, h15.acceptRate7d, 1e-4f)
        assertEquals(2, h15.promptsShownThisHourHistoric)
        assertEquals(3, h15.labeledOutcomeCount)

        // A perfect 1-of-1 hour reads ~0.64, not 1.0 — thin evidence stays humble.
        val h9 = log.responseHistory(now, hourOfDay = 9)
        assertEquals("1 of 1: (1 + 5·0.5625)/6", 0.635417f, h9.acceptRateThisHour, 1e-4f)
        assertEquals(1.0f, h9.acceptRateThisHourRaw, 1e-6f)
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

    @Test
    fun `a tap alone counts as success under the launch policy`() = runTest {
        // Cooldown and earned trust start from the TAP while CLICK_IS_SUCCESS
        // is on — no host completion report needed.
        val id = log.logDecision(shownDecision(), features, t0, 11)
        log.recordAccepted(id, t0 + 10_000)
        val history = log.responseHistory(t0 + 10_000 + 30 * minute, 12)
        assertEquals(30, history.minutesSinceLastCompletedBreak)
        assertEquals("tap counts toward the completion-rate stat", 1f, history.breakCompletionRate, 0f)
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
        // TEMPORARY click-success policy: a tap is a full positive. When
        // CLICK_IS_SUCCESS flips back, this reverts to 0.2f (faint praise).
        assertEquals(1.0f, weight, 0f)
    }

    @Test
    fun `training weights follow the click-success launch policy`() = runTest {
        val completed = log.logDecision(shownDecision(), features, t0, 15)
        log.recordAccepted(completed, t0 + 5_000)
        log.recordCompleted(completed, t0 + 60_000)
        val fastDismissed = log.logDecision(shownDecision(), features, t0 + hour, 15)
        log.recordDismissed(fastDismissed, t0 + hour + 2_000)
        val snoozed = log.logDecision(shownDecision(), features, t0 + 2 * hour, 15)
        log.recordSnoozed(snoozed, t0 + 2 * hour + 8_000)

        val weights = log.trainingRows().map { (_, weight) -> weight }
        assertEquals(listOf(1.0f, -1.0f, 0.1f), weights)

        // Launch policy: taps count as full success (CLICK_IS_SUCCESS).
        // Completion still never ranks BELOW a tap, and the negatives keep
        // their hierarchy regardless of the policy flag.
        assertTrue(PromptOutcome.CLICK_IS_SUCCESS)
        assertTrue(PromptOutcome.COMPLETED.trainingWeight >= PromptOutcome.ACCEPTED.trainingWeight)
        assertTrue(PromptOutcome.ACCEPTED.isSuccess)
        assertTrue(PromptOutcome.DISMISSED_FAST.trainingWeight < PromptOutcome.DISMISSED_SLOW.trainingWeight)
        assertTrue(PromptOutcome.SNOOZED.trainingWeight > 0f)
        assertTrue("snooze is a timing signal, never a success", !PromptOutcome.SNOOZED.isSuccess)
        assertEquals("PENDING must never train", 0f, PromptOutcome.PENDING.trainingWeight, 0f)
    }

    // ------------------------------------------------------------ app-open attribution

    @Test
    fun `app open within the window resolves a pending prompt to OPENED_APP`() = runTest {
        val id = log.logDecision(shownDecision(), features, t0, 15)
        log.resolveAppOpenOutcomes(nowMs = t0 + 5 * minute)
        val row = dao.byId(id)!!
        assertEquals(PromptOutcome.OPENED_APP.name, row.outcome)
        assertEquals("delay = prompt → app open", 300, row.responseDelaySec)
        assertTrue("retention counts as success under the launch policy", row.outcomeEnum.isSuccess)
    }

    @Test
    fun `app open outside the window leaves the prompt pending`() = runTest {
        val id = log.logDecision(shownDecision(), features, t0, 15)
        log.resolveAppOpenOutcomes(nowMs = t0 + 15 * minute) // window is 10 min
        assertEquals(PromptOutcome.PENDING.name, dao.byId(id)!!.outcome)
    }

    @Test
    fun `explicit responses beat the app-open inference in both orders`() = runTest {
        // Dismissal first: the later open never overrides it.
        val dismissed = log.logDecision(shownDecision(), features, t0, 15)
        log.recordDismissed(dismissed, t0 + 2_000)
        log.resolveAppOpenOutcomes(t0 + 3 * minute)
        assertEquals(PromptOutcome.DISMISSED_FAST.name, dao.byId(dismissed)!!.outcome)

        // Inference first: the racing tap (which itself opened the app)
        // refines OPENED_APP to the more specific ACCEPTED.
        val tapped = log.logDecision(shownDecision(), features, t0 + hour, 15)
        log.resolveAppOpenOutcomes(t0 + hour + 10_000)
        assertEquals(PromptOutcome.OPENED_APP.name, dao.byId(tapped)!!.outcome)
        log.recordAccepted(tapped, t0 + hour + 12_000)
        assertEquals(PromptOutcome.ACCEPTED.name, dao.byId(tapped)!!.outcome)
    }

    @Test
    fun `wind-down prompts are never credited by an app open`() = runTest {
        // A wind-down's success is the screen going DARK; opening the app at
        // 1 AM is the opposite outcome.
        val id = log.logDecision(windDownDecision(), features, t0, 1)
        log.resolveAppOpenOutcomes(t0 + 5 * minute)
        assertEquals(PromptOutcome.PENDING.name, dao.byId(id)!!.outcome)
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
    fun `propensity trail persists on the logged row`() = runTest {
        val id = log.logDecision(
            shownDecision().copy(
                appliedThreshold = 0.40f,
                explorationEpsilon = 0.05f,
                explored = true,
            ),
            features, t0, 15,
        )
        val row = dao.byId(id)!!
        assertEquals(0.40f, row.appliedThreshold)
        assertEquals(0.05f, row.explorationEpsilon, 0f)
        assertTrue(row.explored)
    }

    @Test
    fun `purge respects retention`() = runTest {
        log.logDecision(shownDecision(), features, t0 - 100 * day, 15)
        log.logDecision(shownDecision(), features, t0 - 10 * day, 15)
        log.purgeOldRows(t0) // retention 90d
        assertEquals(1, dao.rows.size)
    }
}
