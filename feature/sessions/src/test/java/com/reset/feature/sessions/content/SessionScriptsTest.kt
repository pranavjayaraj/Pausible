package com.reset.feature.sessions.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionScriptsTest {

    private val all = listOf(
        SessionScripts.THE_SIGH,
        SessionScripts.HORIZON,
        SessionScripts.THE_UNFOLD,
        SessionScripts.EMBER,
        SessionScripts.UNCLENCH,
        SessionScripts.THE_LOOP,
        SessionScripts.WRISTS_AND_HANDS,
        SessionScripts.FERN,
        SessionScripts.THE_CLIMB,
        SessionScripts.STEP_OUTSIDE,
        SessionScripts.WARMTH,
        SessionScripts.THREE_GOOD_THINGS,
        SessionScripts.MINI_UNPACK,
        SessionScripts.THE_SETTLE,
        SessionScripts.THE_SWEEP,
        SessionScripts.THE_PLUNGE,
        SessionScripts.REACH_OUT,
        SessionScripts.THE_DETOUR,
        SessionScripts.THE_FIRST_MINUTE,
        SessionScripts.HEAD_DOWN,
        SessionScripts.TOP_UP,
        SessionScripts.THE_LOG,
    )

    // ------------------------------------------------------------ integrity

    @Test
    fun `every script keeps the microbreak duration promise`() {
        for (script in all) {
            assertTrue(
                "${script.id} runs ${script.totalSec}s — a microbreak is 30s..4min",
                script.totalSec in 30..240,
            )
        }
    }

    @Test
    fun `every copy pool offers variety`() {
        for (script in all) {
            val pools = buildList {
                add(script.arrivalDefault)
                addAll(script.arrivalByTrigger.values)
                add(script.landingClose)
                add(script.landingBridge)
                script.guide.forEach { step ->
                    when (step) {
                        is GuideStep.Breath -> add(step.cue)
                        is GuideStep.Move -> add(step.instruction)
                        is GuideStep.Prompt -> add(step.text)
                        is GuideStep.LaunchAction -> add(step.prompt)
                    }
                }
            }
            pools.forEach { pool ->
                assertTrue(
                    "${script.id}: pools need ≥2 variants so session #40 ≠ session #1",
                    pool.lines.size >= 2,
                )
            }
        }
    }

    @Test
    fun `script ids are unique and stable-looking`() {
        assertEquals(all.size, all.map { it.id }.toSet().size)
        all.forEach { assertTrue("${it.id} should be versioned", it.id.endsWith("_v1")) }
    }

    // ------------------------------------------------------------ breath physiology

    @Test
    fun `sigh and ember are exhale-weighted`() {
        // Downshift patterns must exhale longer than they inhale.
        with(BreathPattern.SIGH) { assertTrue(exhaleMs > inhaleMs + secondInhaleMs) }
        with(BreathPattern.EMBER) { assertTrue(exhaleMs > inhaleMs) }
    }

    @Test
    fun `ember runs at the slow wind-down cadence`() {
        assertTrue("wind-down cycle should be ≥ 10s", BreathPattern.EMBER.cycleMs >= 10_000)
    }

    // ------------------------------------------------------------ copy rotation

    @Test
    fun `copy rotation is deterministic and walks the pool`() {
        val pool = SessionScripts.THE_SIGH.arrivalDefault
        val first = pool.pick(seed = 7, completedCount = 0)
        assertEquals("same inputs, same line", first, pool.pick(seed = 7, completedCount = 0))
        assertNotEquals("next session, next line", first, pool.pick(seed = 7, completedCount = 1))
        // Walks the whole pool before repeating.
        val seen = (0 until pool.lines.size).map { pool.pick(seed = 7, completedCount = it) }.toSet()
        assertEquals(pool.lines.size, seen.size)
    }

    @Test
    fun `arrival lines are context-aware with a fallback`() {
        val sigh = SessionScripts.THE_SIGH
        val fragmented = sigh.arrivalFor("BREATHING_RESET")
        val unknown = sigh.arrivalFor("SOMETHING_NEW")
        val none = sigh.arrivalFor(null)
        assertNotEquals(fragmented, sigh.arrivalDefault)
        assertEquals(sigh.arrivalDefault, unknown)
        assertEquals(sigh.arrivalDefault, none)
    }

    // ------------------------------------------------------------ catalog mapping

    @Test
    fun `every sense trigger resolves to its script, with a safe fallback`() {
        assertEquals(SessionScripts.THE_UNFOLD, SessionScripts.forBreak("STRETCH"))
        assertEquals(SessionScripts.THE_UNFOLD, SessionScripts.forBreak("RECOVERY_BREAK"))
        assertEquals(SessionScripts.HORIZON, SessionScripts.forBreak("EYE_BREAK"))
        assertEquals(SessionScripts.EMBER, SessionScripts.forBreak("WIND_DOWN"))
        assertEquals("unknown triggers land somewhere safe", SessionScripts.THE_SIGH, SessionScripts.forBreak("SOMETHING_NEW"))
        assertEquals("no trigger lands somewhere safe", SessionScripts.THE_SIGH, SessionScripts.forBreak(null))
    }
}
