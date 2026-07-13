package com.reset.sense.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BreakDecisionEngineTest {

    // ------------------------------------------------------------ fixtures

    private fun deepFocusInput(
        activity: ActivityState = ActivityState.STILL,
        hour: Int = 15,
        activeUse: Boolean = true,
        promptsToday: Int = 0,
        continuousScreenOnMin: Int = 55,
        appSwitchCount: Int = 2,
        distractingReturnCount: Int = 0,
        coldOpenCount: Int = 1,
        windDownShownTonight: Int = 0,
        history: ResponseHistory = ResponseHistory(minutesSinceLastCompletedBreak = 180),
    ) = DecisionInput(
        usage = UsageSnapshot(
            foregroundDurationSec = 290,
            appSwitchCount = appSwitchCount,
            uniqueAppCount = 1,
            longestSessionSec = 1800,
            screenOnTimeSec = 295,
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
            minutesSinceFirstUnlockToday = 480,
        ),
        time = TimeContext(hourOfDay = hour, minuteOfHour = 0, dayOfWeek = 2),
        history = history,
        gate = GateContext(
            activePhoneUse = activeUse,
            promptsShownToday = promptsToday,
            windDownShownTonight = windDownShownTonight,
        ),
    )

    /** Heavy 1 AM social doomscroll — the wind-down exception's target context. */
    private fun nightDoomscrollInput(
        hour: Int = 1,
        windDownShownTonight: Int = 0,
        history: ResponseHistory = ResponseHistory(minutesSinceLastCompletedBreak = 300),
    ) = deepFocusInput(
        hour = hour,
        continuousScreenOnMin = 55,
        appSwitchCount = 14,
        distractingReturnCount = 5,
        coldOpenCount = 6,
        windDownShownTonight = windDownShownTonight,
        history = history,
    )

    private fun rulesOnlyEngine(mode: SenseMode = SenseMode.BALANCED) =
        BreakDecisionEngine(model = null, mode = mode)

    // ------------------------------------------------------------ hard gates

    @Test
    fun `driving without active use suppresses before scoring`() {
        val d = rulesOnlyEngine().decide(deepFocusInput(activity = ActivityState.IN_VEHICLE, activeUse = false))
        assertEquals(PromptAction.SUPPRESS, d.action)
        assertEquals(GateReason.VEHICLE, d.gateReason)
        assertNull("model must not run behind a gate", d.modelScore)
    }

    @Test
    fun `driving with sustained phone use lifts the vehicle gate (passenger)`() {
        val d = rulesOnlyEngine().decide(deepFocusInput(activity = ActivityState.IN_VEHICLE, activeUse = true))
        assertNotEquals(GateReason.VEHICLE, d.gateReason)
    }

    @Test
    fun `quiet hours suppress idle-ish use including midnight wrap`() {
        // Short screen-on streaks (an alarm check) never earn a night nudge.
        val idle23 = deepFocusInput(hour = 23, continuousScreenOnMin = 5)
        val idle3 = deepFocusInput(hour = 3, continuousScreenOnMin = 5)
        assertEquals(GateReason.QUIET_HOURS, rulesOnlyEngine().decide(idle23).gateReason)
        assertEquals(GateReason.QUIET_HOURS, rulesOnlyEngine().decide(idle3).gateReason)
        assertNotEquals(GateReason.QUIET_HOURS, rulesOnlyEngine().decide(deepFocusInput(hour = 8)).gateReason)
    }

    // ------------------------------------------------------------ ε-exploration

    /** Calm-but-borderline context: scores ≈0.385, just under BALANCED's 0.40 soft bar. */
    private fun borderlineInput() = deepFocusInput(
        continuousScreenOnMin = 36,
        appSwitchCount = 2,
        coldOpenCount = 0,
    ).let { it.copy(device = it.device.copy(charging = false)) }

    @Test
    fun `epsilon zero never explores - todays default`() {
        val engine = BreakDecisionEngine(model = null) // ε defaults to 0
        repeat(50) {
            val d = engine.decide(borderlineInput())
            assertEquals(PromptAction.SUPPRESS, d.action)
            assertTrue(!d.explored)
        }
    }

    @Test
    fun `borderline suppress explores as a soft nudge when epsilon fires`() {
        val engine = BreakDecisionEngine(model = null, explorationEpsilon = 1f)
        val d = engine.decide(borderlineInput())
        assertEquals("softest intensity only", PromptAction.SOFT_NUDGE, d.action)
        assertTrue(d.explored)
        assertTrue(d.reason.endsWith("EXPLORED"))
        assertEquals(SenseMode.BALANCED.softThreshold, d.appliedThreshold)
        assertEquals(1f, d.explorationEpsilon)
    }

    @Test
    fun `far-below-threshold contexts are never explored`() {
        val engine = BreakDecisionEngine(model = null, explorationEpsilon = 1f)
        // Minimal context: idle pickup, scores well under the 0.30 band floor.
        val d = engine.decide(
            deepFocusInput(continuousScreenOnMin = 2, appSwitchCount = 0, coldOpenCount = 0)
                .let { it.copy(device = it.device.copy(charging = false, minutesInCurrentActivity = 5)) },
        )
        assertEquals(PromptAction.SUPPRESS, d.action)
        assertTrue("exploration only probes borderline calls", !d.explored)
    }

    @Test
    fun `hard gates and wind-down are never explored`() {
        val engine = BreakDecisionEngine(model = null, explorationEpsilon = 1f)
        val gated = engine.decide(deepFocusInput(promptsToday = 3))
        assertEquals(GateReason.DAILY_CAP, gated.gateReason)
        assertTrue(!gated.explored)

        val night = engine.decide(deepFocusInput(hour = 23)) // wind-down mode, below 0.55
        assertEquals(PromptAction.SUPPRESS, night.action)
        assertTrue("sleep context is never explored", !night.explored)
        assertEquals(0f, night.explorationEpsilon)
    }

    // ------------------------------------------------------------ earned-trust daily cap

    @Test
    fun `default user is capped at three prompts`() {
        val d = rulesOnlyEngine().decide(deepFocusInput(promptsToday = 3))
        assertEquals(GateReason.DAILY_CAP, d.gateReason)
    }

    @Test
    fun `high completion rate with enough evidence earns a higher cap`() {
        val trusted = ResponseHistory(
            minutesSinceLastCompletedBreak = 180,
            breakCompletionRate = 0.8f,
            labeledOutcomeCount = 50,
        )
        // 4th prompt allowed for the trusted user…
        val fourth = rulesOnlyEngine().decide(deepFocusInput(promptsToday = 3, history = trusted))
        assertNotEquals(GateReason.DAILY_CAP, fourth.gateReason)
        // …but the earned cap still holds at 5.
        val sixth = rulesOnlyEngine().decide(deepFocusInput(promptsToday = 5, history = trusted))
        assertEquals(GateReason.DAILY_CAP, sixth.gateReason)
    }

    @Test
    fun `low completion rate keeps the base cap regardless of volume`() {
        val dismissive = ResponseHistory(
            minutesSinceLastCompletedBreak = 180,
            breakCompletionRate = 0.3f,
            labeledOutcomeCount = 200,
        )
        val d = rulesOnlyEngine().decide(deepFocusInput(promptsToday = 3, history = dismissive))
        assertEquals(GateReason.DAILY_CAP, d.gateReason)
    }

    @Test
    fun `perfect completion on thin evidence is not yet trust`() {
        val newUser = ResponseHistory(
            minutesSinceLastCompletedBreak = 180,
            breakCompletionRate = 1.0f,
            labeledOutcomeCount = 5, // below the 20-outcome evidence floor
        )
        val d = rulesOnlyEngine().decide(deepFocusInput(promptsToday = 3, history = newUser))
        assertEquals(GateReason.DAILY_CAP, d.gateReason)
    }

    // ------------------------------------------------------------ wind-down exception

    @Test
    fun `night doomscroll gets exactly one silent wind-down`() {
        val d = rulesOnlyEngine().decide(nightDoomscrollInput())
        assertEquals("never a heads-up at night", PromptAction.SOFT_NUDGE, d.action)
        assertEquals(BreakType.WIND_DOWN, d.breakType)
        assertNull(d.gateReason)
        assertTrue(d.reason.startsWith("wind_down"))
    }

    @Test
    fun `nightly cap suppresses the second wind-down`() {
        val d = rulesOnlyEngine().decide(nightDoomscrollInput(windDownShownTonight = 1))
        assertEquals(PromptAction.SUPPRESS, d.action)
        assertEquals(GateReason.NIGHT_CAP, d.gateReason)
    }

    @Test
    fun `mild night use stays below the wind-down threshold`() {
        // Long screen-on but calm usage (the default deep-focus profile at 23:00)
        // scores ~0.42 — under the 0.55 wind-down bar. Awake ≠ automatically nudged.
        val d = rulesOnlyEngine().decide(deepFocusInput(hour = 23))
        assertEquals(PromptAction.SUPPRESS, d.action)
        assertNull("scored suppression, not a gate", d.gateReason)
        assertTrue(d.reason.startsWith("wind_down"))
    }

    @Test
    fun `dismissed night prompts extinguish the exception via dead hour`() {
        val d = rulesOnlyEngine().decide(
            nightDoomscrollInput(
                history = ResponseHistory(
                    minutesSinceLastCompletedBreak = 300,
                    acceptRateThisHour = 0f,
                    promptsShownThisHourHistoric = 8,
                ),
            ),
        )
        assertEquals(GateReason.DEAD_HOUR, d.gateReason)
    }

    @Test
    fun `cooldown applies inside quiet hours too`() {
        val d = rulesOnlyEngine().decide(
            nightDoomscrollInput(history = ResponseHistory(minutesSinceLastCompletedBreak = 5)),
        )
        assertEquals(GateReason.COOLDOWN, d.gateReason)
    }

    @Test
    fun `cooldown suppresses right after a completed break`() {
        val d = rulesOnlyEngine().decide(
            deepFocusInput(history = ResponseHistory(minutesSinceLastCompletedBreak = 5)),
        )
        assertEquals(GateReason.COOLDOWN, d.gateReason)
    }

    @Test
    fun `daily cap suppresses the fourth prompt`() {
        val d = rulesOnlyEngine().decide(deepFocusInput(promptsToday = 3))
        assertEquals(GateReason.DAILY_CAP, d.gateReason)
    }

    @Test
    fun `dead hour suppresses when history shows near-zero acceptance`() {
        val d = rulesOnlyEngine().decide(
            deepFocusInput(
                history = ResponseHistory(
                    minutesSinceLastCompletedBreak = 180,
                    acceptRateThisHour = 0.0f,
                    promptsShownThisHourHistoric = 8,
                ),
            ),
        )
        assertEquals(GateReason.DEAD_HOUR, d.gateReason)
    }

    // ------------------------------------------------------------ scoring & blend

    @Test
    fun `deep focus while stationary triggers a prompt on rules alone`() {
        val d = rulesOnlyEngine().decide(deepFocusInput())
        assertTrue("expected a prompt, got $d", d.action != PromptAction.SUPPRESS)
        assertEquals(0f, d.modelAlpha, 0f)
        assertNull(d.modelScore)
    }

    @Test
    fun `alpha ramps with labeled outcomes and blends model in`() {
        val model = AcceptanceModel.fromJson(
            checkNotNull(javaClass.classLoader?.getResourceAsStream("model.json"))
                .bufferedReader().readText(),
        )
        val engine = BreakDecisionEngine(model = model)

        val fresh = engine.decide(deepFocusInput(history = ResponseHistory(minutesSinceLastCompletedBreak = 180, labeledOutcomeCount = 0)))
        assertEquals("fresh user runs pure rules", 0f, fresh.modelAlpha, 1e-6f)

        val mature = engine.decide(deepFocusInput(history = ResponseHistory(minutesSinceLastCompletedBreak = 180, labeledOutcomeCount = 400)))
        assertEquals("mature user runs at max alpha", 0.7f, mature.modelAlpha, 1e-6f)
        assertNotNull(mature.modelScore)
    }

    @Test
    fun `assertive mode prompts where gentle stays silent`() {
        // Mild context: moderate score territory.
        val mild = deepFocusInput().let {
            it.copy(usage = it.usage.copy(continuousScreenOnMin = 25, appSwitchCount = 6, longestSessionSec = 700))
        }
        val gentle = rulesOnlyEngine(SenseMode.GENTLE).decide(mild)
        val assertive = rulesOnlyEngine(SenseMode.ASSERTIVE).decide(mild)
        assertTrue(
            "assertive (${assertive.action}) should be >= gentle (${gentle.action})",
            assertive.action.ordinal >= gentle.action.ordinal,
        )
    }

    // ------------------------------------------------------------ break types

    @Test
    fun `on foot maps to passive reminder`() {
        val d = rulesOnlyEngine(SenseMode.ASSERTIVE).decide(deepFocusInput(activity = ActivityState.ON_FOOT))
        if (d.action != PromptAction.SUPPRESS) assertEquals(BreakType.PASSIVE_REMINDER, d.breakType)
    }

    @Test
    fun `long stationary overuse maps to recovery break`() {
        val d = rulesOnlyEngine().decide(deepFocusInput())
        assertTrue(d.action != PromptAction.SUPPRESS)
        assertEquals(BreakType.RECOVERY_BREAK, d.breakType)
    }

    @Test
    fun `fragmented switching maps to breathing reset`() {
        val input = deepFocusInput().let {
            it.copy(
                usage = it.usage.copy(appSwitchCount = 14, continuousScreenOnMin = 20, uniqueAppCount = 6),
                device = it.device.copy(minutesInCurrentActivity = 30),
            )
        }
        val d = rulesOnlyEngine(SenseMode.ASSERTIVE).decide(input)
        assertTrue(d.action != PromptAction.SUPPRESS)
        assertEquals(BreakType.BREATHING_RESET, d.breakType)
    }

    @Test
    fun `suppressed decisions carry no break type`() {
        val d = rulesOnlyEngine().decide(deepFocusInput(promptsToday = 3))
        assertEquals(BreakType.NONE, d.breakType)
    }
}
