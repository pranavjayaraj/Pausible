package com.reset.sense.signals.usage

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSessionNormalizerTest {

    private val normalizer = AppSessionNormalizer(debounceMs = 2_000)

    private fun fg(pkg: String, at: Long, cls: String? = null) =
        RawUsageEvent(RawEventType.FOREGROUND, pkg, at, cls)

    private fun bg(pkg: String, at: Long, cls: String? = null) =
        RawUsageEvent(RawEventType.BACKGROUND, pkg, at, cls)

    @Test
    fun `simple legacy stream produces one session per app`() {
        val sessions = normalizer.normalize(
            listOf(fg("a", 0), bg("a", 10_000), fg("b", 10_000), bg("b", 25_000)),
            endOfStreamMs = 30_000,
        )
        assertEquals(
            listOf(AppSession("a", 0, 10_000), AppSession("b", 10_000, 25_000)),
            sessions,
        )
    }

    @Test
    fun `intra-app activity transition does not split the session`() {
        // Activity A pauses, activity B of the SAME app resumes 500ms later.
        val sessions = normalizer.normalize(
            listOf(
                fg("app", 0, "app/ActivityA"),
                bg("app", 10_000, "app/ActivityA"),
                fg("app", 10_500, "app/ActivityB"),
                bg("app", 20_000, "app/ActivityB"),
            ),
            endOfStreamMs = 30_000,
        )
        assertEquals("debounce must merge", listOf(AppSession("app", 0, 20_000)), sessions)
    }

    @Test
    fun `paused then stopped does not double-close`() {
        // API 29+: backgrounding fires PAUSED then STOPPED for the same activity.
        val sessions = normalizer.normalize(
            listOf(
                fg("a", 0, "a/Main"),
                bg("a", 10_000, "a/Main"),  // PAUSED
                fg("b", 10_100, "b/Main"),
                bg("a", 10_200, "a/Main"),  // STOPPED — must be a no-op
                bg("b", 20_000, "b/Main"),
            ),
            endOfStreamMs = 30_000,
        )
        assertEquals(
            listOf(AppSession("a", 0, 10_000), AppSession("b", 10_100, 20_000)),
            sessions,
        )
    }

    @Test
    fun `split screen keeps two concurrent sessions`() {
        val sessions = normalizer.normalize(
            listOf(
                fg("a", 0, "a/Main"),
                fg("b", 1_000, "b/Main"),   // both resumed (multi-window)
                bg("a", 15_000, "a/Main"),
                bg("b", 20_000, "b/Main"),
            ),
            endOfStreamMs = 30_000,
        )
        assertEquals(
            listOf(AppSession("a", 0, 15_000), AppSession("b", 1_000, 20_000)),
            sessions,
        )
    }

    @Test
    fun `repeated resumed for the same activity is idempotent`() {
        val sessions = normalizer.normalize(
            listOf(
                fg("a", 0, "a/Main"),
                fg("a", 5_000, "a/Main"), // duplicate RESUMED
                bg("a", 10_000, "a/Main"),
            ),
            endOfStreamMs = 30_000,
        )
        assertEquals(listOf(AppSession("a", 0, 10_000)), sessions)
    }

    @Test
    fun `open session closes at end of stream`() {
        val sessions = normalizer.normalize(listOf(fg("a", 5_000)), endOfStreamMs = 60_000)
        assertEquals(listOf(AppSession("a", 5_000, 60_000)), sessions)
    }

    @Test
    fun `background without foreground is ignored`() {
        val sessions = normalizer.normalize(
            listOf(bg("ghost", 1_000), fg("a", 2_000), bg("a", 9_000)),
            endOfStreamMs = 10_000,
        )
        assertEquals(listOf(AppSession("a", 2_000, 9_000)), sessions)
    }

    @Test
    fun `gap longer than debounce creates separate sessions`() {
        val sessions = normalizer.normalize(
            listOf(fg("a", 0), bg("a", 10_000), fg("a", 20_000), bg("a", 30_000)),
            endOfStreamMs = 40_000,
        )
        assertEquals(
            listOf(AppSession("a", 0, 10_000), AppSession("a", 20_000, 30_000)),
            sessions,
        )
    }

    @Test
    fun `no merge across an intervening app`() {
        // a → b → a, with the second `a` arriving within debounce of a's close;
        // b in between means it was a REAL switch, not an intra-app blip.
        val sessions = normalizer.normalize(
            listOf(
                fg("a", 0), bg("a", 10_000),
                fg("b", 10_100), bg("b", 11_000),
                fg("a", 11_500), bg("a", 20_000),
            ),
            endOfStreamMs = 30_000,
        )
        assertEquals(3, sessions.size)
        assertEquals(listOf("a", "b", "a"), sessions.map { it.packageName })
    }
}
