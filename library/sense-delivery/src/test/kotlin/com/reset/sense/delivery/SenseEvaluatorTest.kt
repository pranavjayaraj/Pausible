package com.reset.sense.delivery

import com.reset.sense.ml.AcceptanceModel
import com.reset.sense.ml.ActivityState
import com.reset.sense.ml.AppCategory
import com.reset.sense.ml.BreakDecisionEngine
import com.reset.sense.ml.BreakType
import com.reset.sense.ml.Decision
import com.reset.sense.ml.DeviceSnapshot
import com.reset.sense.ml.GateReason
import com.reset.sense.ml.PromptAction
import com.reset.sense.ml.TimeContext
import com.reset.sense.ml.UsageSnapshot
import com.reset.sense.signals.SenseAccuracyTier
import com.reset.sense.signals.SenseSnapshots
import com.reset.sense.store.DecisionEntity
import com.reset.sense.store.DecisionLogDao
import com.reset.sense.store.PromptOutcome
import com.reset.sense.store.SenseDecisionLog
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SenseEvaluatorTest {

    // ---------------------------------------------------------------- fakes

    private class FakeDao : DecisionLogDao {
        val rows = mutableListOf<DecisionEntity>()
        private var nextId = 1L

        override suspend fun insert(entity: DecisionEntity): Long {
            val id = nextId++
            rows += entity.copy(id = id)
            return id
        }

        override suspend fun byId(id: Long) = rows.find { it.id == id }
        override suspend fun shownSince(sinceMs: Long) =
            rows.filter {
                it.wasShown && it.outcome != PromptOutcome.NOT_SHOWN.name && it.timestampMs >= sinceMs
            }.sortedBy { it.timestampMs }

        override suspend fun updateOutcome(id: Long, outcome: String, atMs: Long, delaySec: Int?) {
            val i = rows.indexOfFirst { it.id == id }
            if (i >= 0) rows[i] = rows[i].copy(outcome = outcome, outcomeAtMs = atMs, responseDelaySec = delaySec)
        }

        override suspend fun lastSuccessAtMs(successOutcomes: List<String>) =
            rows.filter { it.outcome in successOutcomes }.mapNotNull { it.outcomeAtMs }.maxOrNull()

        override suspend fun labeledOutcomeCount() =
            rows.count {
                it.wasShown && it.outcome != PromptOutcome.PENDING.name &&
                    it.outcome != PromptOutcome.NOT_SHOWN.name
            }

        override suspend fun pendingOlderThan(cutoffMs: Long) =
            rows.filter { it.wasShown && it.outcome == PromptOutcome.PENDING.name && it.timestampMs < cutoffMs }
                .map { it.id }

        override suspend fun labeledTrainingRows() =
            rows.filter { it.featuresCsv != null && it.outcome != PromptOutcome.PENDING.name }

        override suspend fun purgeOlderThan(cutoffMs: Long): Int {
            val before = rows.size
            rows.removeAll { it.timestampMs < cutoffMs }
            return before - rows.size
        }

        override suspend fun recent(limit: Int) =
            rows.sortedByDescending { it.timestampMs }.take(limit)
    }

    private class FakePresenter(
        var enabled: Boolean = true,
        /** Simulates the last-moment withhold (host foregrounded mid-tick). */
        var withholdNext: Boolean = false,
    ) : BreakPresenter {
        var presented: Pair<Decision, Long>? = null
        override fun canPresent() = enabled
        override fun present(decision: Decision, decisionId: Long): Boolean {
            if (withholdNext) return false
            presented = decision to decisionId
            return true
        }
        override fun dismissCurrent() { presented = null }
    }

    private class FakeSnapshotSource(var snapshots: SenseSnapshots) : SnapshotSource {
        override suspend fun capture(nowMs: Long) = snapshots
    }

    // ---------------------------------------------------------------- fixtures

    private val noonTuesday = 1_750_000_000_000L

    private fun snapshots(
        continuousScreenOnMin: Int = 55,
        screenOnTimeSec: Int = 295,
        activity: ActivityState = ActivityState.STILL,
        hour: Int = 15,
        appSwitchCount: Int = 2,
        distractingReturnCount: Int = 0,
        coldOpenCount: Int = 1,
        lastScreenOffMs: Long? = null,
    ) = SenseSnapshots(
        usage = UsageSnapshot(
            foregroundDurationSec = 290,
            appSwitchCount = appSwitchCount,
            uniqueAppCount = 1,
            longestSessionSec = 1800,
            screenOnTimeSec = screenOnTimeSec,
            continuousScreenOnMin = continuousScreenOnMin,
            distractingReturnCount = distractingReturnCount,
            coldOpenCount = coldOpenCount,
            unlockCountLastHour = 2,
            foregroundCategory = AppCategory.WORK,
        ),
        device = DeviceSnapshot(
            activityState = activity,
            minutesInCurrentActivity = 100,
            charging = true,
            batteryPercent = 80,
            minutesSinceFirstUnlockToday = 400,
        ),
        time = TimeContext(hourOfDay = hour, minuteOfHour = 0, dayOfWeek = 1),
        accuracyTier = SenseAccuracyTier.FULL,
        lastScreenOffMs = lastScreenOffMs,
    )

    /** Heavy 1 AM doomscroll — quiet hours, but demonstrably awake. */
    private fun nightSnapshots(lastScreenOffMs: Long? = null) = snapshots(
        hour = 1,
        appSwitchCount = 14,
        distractingReturnCount = 5,
        coldOpenCount = 6,
        lastScreenOffMs = lastScreenOffMs,
    )

    private fun evaluator(
        dao: FakeDao = FakeDao(),
        presenter: FakePresenter = FakePresenter(),
        source: FakeSnapshotSource = FakeSnapshotSource(snapshots()),
        model: AcceptanceModel? = null,
        quietHours: QuietHours = QuietHours.DEFAULT,
        hostForeground: Boolean = false,
    ) = Evaluation(
        dao = dao,
        presenter = presenter,
        evaluator = SenseEvaluator(
            snapshotSource = source,
            decisionLog = SenseDecisionLog(dao),
            engine = BreakDecisionEngine(model = model),
            presenter = presenter,
            quietHoursSource = { quietHours },
            hostStateSource = { hostForeground },
        ),
    )

    private data class Evaluation(
        val dao: FakeDao,
        val presenter: FakePresenter,
        val evaluator: SenseEvaluator,
    )

    // ---------------------------------------------------------------- tests

    @Test
    fun `deep focus tick prompts, logs features, and presents`() = runTest {
        val (dao, presenter, evaluator) = evaluator()
        val decision = evaluator.evaluateNow(noonTuesday)

        assertTrue(decision.action != PromptAction.SUPPRESS)
        val row = dao.rows.single()
        assertNotNull("shown prompt must persist features", row.featuresCsv)
        assertEquals(34, row.featuresCsv!!.split(',').size)
        assertEquals(row.id, presenter.presented!!.second)
    }

    @Test
    fun `suppressed tick logs without features and presents nothing`() = runTest {
        val (dao, presenter, evaluator) = evaluator(
            source = FakeSnapshotSource(
                snapshots(activity = ActivityState.IN_VEHICLE, screenOnTimeSec = 0),
            ),
        )
        val decision = evaluator.evaluateNow(noonTuesday)

        assertEquals(PromptAction.SUPPRESS, decision.action)
        assertEquals(GateReason.VEHICLE, decision.gateReason)
        assertNull(dao.rows.single().featuresCsv)
        assertNull(presenter.presented)
    }

    @Test
    fun `vehicle gate lifts for passengers with active screen use`() = runTest {
        val (_, _, evaluator) = evaluator(
            source = FakeSnapshotSource(
                snapshots(activity = ActivityState.IN_VEHICLE, screenOnTimeSec = 295),
            ),
        )
        val decision = evaluator.evaluateNow(noonTuesday)
        assertTrue(decision.gateReason != GateReason.VEHICLE)
    }

    @Test
    fun `notifications disabled logs diagnostics but never a shown row`() = runTest {
        val (dao, presenter, evaluator) = evaluator(presenter = FakePresenter(enabled = false))
        val decision = evaluator.evaluateNow(noonTuesday)

        assertEquals(PromptAction.SUPPRESS, decision.action)
        assertEquals("notifications_disabled", decision.reason)
        assertNull(presenter.presented)
        assertTrue("no shown rows to rot into IGNORED", dao.rows.none { it.wasShown })
    }

    @Test
    fun `daily cap ends prompting for the day`() = runTest {
        val (dao, presenter, evaluator) = evaluator()
        // 3 prompts already shown today (cap = 3). Outcomes land > 20 min ago:
        // under CLICK_IS_SUCCESS a tap starts the cooldown, and this test is
        // about the daily cap, not the cooldown gate.
        repeat(3) {
            dao.insert(
                DecisionEntity(
                    timestampMs = noonTuesday - (it + 2) * 60 * 60_000L,
                    hourOfDay = 10,
                    action = PromptAction.FULL_PROMPT.name,
                    breakType = "STRETCH",
                    blendedScore = 0.7f, ruleScore = 0.7f, modelScore = null,
                    modelAlpha = 0f, gateReason = null,
                    featuresCsv = "0.5", outcome = PromptOutcome.ACCEPTED.name,
                    outcomeAtMs = noonTuesday - (it + 2) * 60 * 60_000L, responseDelaySec = 5,
                ),
            )
        }
        val decision = evaluator.evaluateNow(noonTuesday)
        assertEquals(GateReason.DAILY_CAP, decision.gateReason)
        assertNull(presenter.presented)
    }

    @Test
    fun `stale pending prompts are swept before history is read`() = runTest {
        val (dao, _, evaluator) = evaluator()
        val staleId = dao.insert(
            DecisionEntity(
                timestampMs = noonTuesday - 2 * 60 * 60_000L, // 2h old, PENDING
                hourOfDay = 13,
                action = PromptAction.FULL_PROMPT.name,
                breakType = "STRETCH",
                blendedScore = 0.7f, ruleScore = 0.7f, modelScore = null,
                modelAlpha = 0f, gateReason = null, featuresCsv = "0.5",
            ),
        )
        evaluator.evaluateNow(noonTuesday)
        assertEquals(PromptOutcome.IGNORED.name, dao.byId(staleId)!!.outcome)
    }

    @Test
    fun `last-moment withhold marks the row NOT_SHOWN and spends no budget`() = runTest {
        // The cold-start race: evaluator's early gate passed, but the host
        // reached the foreground before notify() — presenter withholds.
        val presenter = FakePresenter(withholdNext = true)
        val (dao, _, evaluator) = evaluator(presenter = presenter)
        evaluator.evaluateNow(noonTuesday)

        val row = dao.rows.single()
        assertEquals(PromptOutcome.NOT_SHOWN.name, row.outcome)
        assertNull(presenter.presented)
        // The withheld prompt must not consume the daily cap: a normal tick
        // right after still prompts.
        presenter.withholdNext = false
        val next = evaluator.evaluateNow(noonTuesday + 60_000)
        assertTrue(next.action != PromptAction.SUPPRESS)
        assertNotNull(presenter.presented)
    }

    @Test
    fun `tick while the app is open suppresses without a shown row`() = runTest {
        // Deep-focus context that would normally prompt — but the user is
        // already IN the app, so the notification channel must stay silent.
        val (dao, presenter, evaluator) = evaluator(hostForeground = true)
        val decision = evaluator.evaluateNow(noonTuesday)

        assertEquals(PromptAction.SUPPRESS, decision.action)
        assertEquals("host_foreground", decision.reason)
        assertNull(presenter.presented)
        assertTrue("no shown row to rot into IGNORED", dao.rows.none { it.wasShown })
    }

    @Test
    fun `user-configured quiet hours are respected`() = runTest {
        // User narrows quiet hours to 02:00–06:00; a 1 AM doomscroll is now
        // OUTSIDE the window, so the normal engine path runs instead of the
        // wind-down constraint (late-night mapping still picks WIND_DOWN).
        val (_, presenter, evaluator) = evaluator(
            source = FakeSnapshotSource(nightSnapshots()),
            quietHours = QuietHours(startHour = 2, endHour = 6),
        )
        val decision = evaluator.evaluateNow(noonTuesday)
        assertEquals("normal thresholds apply outside quiet hours", PromptAction.FULL_PROMPT, decision.action)
        assertNotNull(presenter.presented)
    }

    // ------------------------------------------------------------ wind-down loop

    @Test
    fun `night doomscroll tick presents a silent wind-down and logs features`() = runTest {
        val (dao, presenter, evaluator) = evaluator(source = FakeSnapshotSource(nightSnapshots()))
        val decision = evaluator.evaluateNow(noonTuesday)

        assertEquals(PromptAction.SOFT_NUDGE, decision.action)
        assertEquals(BreakType.WIND_DOWN, decision.breakType)
        assertNotNull(presenter.presented)
        assertNotNull("wind-down rows are training data too", dao.rows.single().featuresCsv)
    }

    @Test
    fun `second wind-down the same night is capped`() = runTest {
        val (_, presenter, evaluator) = evaluator(source = FakeSnapshotSource(nightSnapshots()))
        // First tick shows the nightly wind-down (row logged 30s in the past
        // relative to the second tick, safely after any quiet-hours start).
        evaluator.evaluateNow(noonTuesday - 30_000)
        presenter.presented = null

        val second = evaluator.evaluateNow(noonTuesday)
        assertEquals(PromptAction.SUPPRESS, second.action)
        assertEquals(GateReason.NIGHT_CAP, second.gateReason)
        assertNull(presenter.presented)
    }

    @Test
    fun `screens-off soon after the wind-down resolves it to COMPLETED`() = runTest {
        val dao = FakeDao()
        val promptAt = noonTuesday - 15 * 60_000L
        val (_, _, evaluator) = evaluator(
            dao = dao,
            // Next tick reports the screen went dark 5 min after the prompt.
            source = FakeSnapshotSource(nightSnapshots(lastScreenOffMs = promptAt + 5 * 60_000L)),
        )
        val windDownRow = dao.insert(
            DecisionEntity(
                timestampMs = promptAt,
                hourOfDay = 1,
                action = PromptAction.SOFT_NUDGE.name,
                breakType = BreakType.WIND_DOWN.name,
                blendedScore = 0.6f, ruleScore = 0.6f, modelScore = null,
                modelAlpha = 0f, gateReason = null, featuresCsv = "0.5",
            ),
        )
        evaluator.evaluateNow(noonTuesday)
        assertEquals(
            "putting the phone down IS the wind-down's completion",
            PromptOutcome.COMPLETED.name,
            dao.byId(windDownRow)!!.outcome,
        )
    }

    @Test
    fun `evaluator uses the real bundled model end to end`() = runTest {
        val json = checkNotNull(javaClass.classLoader?.getResourceAsStream("model.json"))
            .bufferedReader().readText()
        val (dao, _, evaluator) = evaluator(model = AcceptanceModel.fromJson(json))
        // Give the user enough labeled history to ramp α above zero.
        repeat(250) {
            val at = noonTuesday - 40L * 24 * 60 * 60_000L // outside 30d stats
            dao.insert(
                DecisionEntity(
                    timestampMs = at,
                    hourOfDay = 10,
                    action = PromptAction.FULL_PROMPT.name,
                    breakType = "STRETCH",
                    blendedScore = 0.5f, ruleScore = 0.5f, modelScore = null,
                    modelAlpha = 0f, gateReason = null, featuresCsv = null,
                    outcome = PromptOutcome.ACCEPTED.name,
                    // Old outcome times too: under CLICK_IS_SUCCESS a recent
                    // tap would trip the cooldown gate and mask the α check.
                    outcomeAtMs = at + 5_000, responseDelaySec = 5,
                ),
            )
        }
        val decision = evaluator.evaluateNow(noonTuesday)
        assertNotNull("model score must be blended in", decision.modelScore)
        assertTrue(decision.modelAlpha > 0f)
    }
}
