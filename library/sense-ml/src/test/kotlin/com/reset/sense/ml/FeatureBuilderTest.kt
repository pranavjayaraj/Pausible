package com.reset.sense.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureBuilderTest {

    private val usage = UsageSnapshot(
        foregroundDurationSec = 150,   // -> 0.5
        appSwitchCount = 10,           // -> 0.5
        uniqueAppCount = 5,            // -> 0.5
        longestSessionSec = 900,       // -> 0.5
        screenOnTimeSec = 300,         // -> 1.0
        continuousScreenOnMin = 90,    // -> clipped 1.0
        distractingReturnCount = 1,    // -> 0.2
        coldOpenCount = 5,             // -> 0.5
        unlockCountLastHour = 30,      // -> clipped 1.0
        foregroundCategory = AppCategory.SOCIAL,
    )
    private val device = DeviceSnapshot(
        activityState = ActivityState.ON_FOOT,
        minutesInCurrentActivity = 60, // -> 0.5
        charging = true,
        batteryPercent = 50,           // -> 0.5
        minutesSinceFirstUnlockToday = 480, // -> 0.5
    )
    private val time = TimeContext(hourOfDay = 6, minuteOfHour = 0, dayOfWeek = 0)
    private val history = ResponseHistory(
        minutesSinceLastCompletedBreak = 120, // -> 0.5
        dismissCount24h = 10,                 // -> clipped 1.0
        snoozeCount24h = 1,                   // -> 0.2
        acceptRate7d = 0.8f,
        acceptRateThisHour = 0.3f,
        avgResponseDelaySec = 60f,            // -> 0.5
        breakCompletionRate = 0.9f,
    )

    @Test
    fun `vector has schema size and normalized values`() {
        val f = FeatureBuilder.build(usage, device, time, history)
        assertEquals(FeatureSchema.FEATURE_COUNT, f.size)

        assertEquals(0.5f, f[FeatureSchema.IDX_FOREGROUND_DURATION], 1e-6f)
        assertEquals(0.5f, f[FeatureSchema.IDX_APP_SWITCH_COUNT], 1e-6f)
        assertEquals(1.0f, f[FeatureSchema.IDX_SCREEN_ON_TIME], 1e-6f)
        assertEquals("clipping applies", 1.0f, f[FeatureSchema.IDX_CONTINUOUS_SCREEN_ON], 1e-6f)
        assertEquals(1.0f, f[FeatureSchema.IDX_UNLOCKS_LAST_HOUR], 1e-6f)
        assertEquals(0.5f, f[FeatureSchema.IDX_MIN_SINCE_LAST_BREAK], 1e-6f)
        assertEquals(1.0f, f[FeatureSchema.IDX_DISMISS_24H], 1e-6f)
        assertEquals(0.8f, f[FeatureSchema.IDX_ACCEPT_RATE_7D], 1e-6f)
    }

    @Test
    fun `category one-hot sets exactly one slot`() {
        val f = FeatureBuilder.build(usage, device, time, history)
        val catSlots = (FeatureSchema.IDX_CAT_FIRST until FeatureSchema.IDX_CAT_FIRST + 6).map { f[it] }
        assertEquals(1f, catSlots.sum(), 1e-6f)
        assertEquals("SOCIAL is ordinal 1", 1f, f[FeatureSchema.IDX_CAT_FIRST + 1], 1e-6f)
    }

    @Test
    fun `activity one-hot sets exactly one slot and on_foot lands in slot 1`() {
        val f = FeatureBuilder.build(usage, device, time, history)
        val slots = (FeatureSchema.IDX_ACTIVITY_FIRST until FeatureSchema.IDX_ACTIVITY_FIRST + 3).map { f[it] }
        assertEquals(1f, slots.sum(), 1e-6f)
        assertEquals(1f, f[FeatureSchema.IDX_ACTIVITY_FIRST + 1], 1e-6f)
    }

    @Test
    fun `cyclical encoding wraps midnight continuously`() {
        val justBefore = FeatureBuilder.build(usage, device, TimeContext(23, 59, 0), history)
        val justAfter = FeatureBuilder.build(usage, device, TimeContext(0, 1, 0), history)
        val dSin = justBefore[FeatureSchema.IDX_HOUR_SIN] - justAfter[FeatureSchema.IDX_HOUR_SIN]
        val dCos = justBefore[FeatureSchema.IDX_HOUR_COS] - justAfter[FeatureSchema.IDX_HOUR_COS]
        assertTrue("midnight must be continuous, got dSin=$dSin dCos=$dCos",
            Math.abs(dSin) < 0.01f && Math.abs(dCos) < 0.01f)
    }

    @Test
    fun `late night flag covers 23h to 5h`() {
        assertEquals(1f, FeatureBuilder.build(usage, device, TimeContext(23, 0, 0), history)[FeatureSchema.IDX_LATE_NIGHT], 0f)
        assertEquals(1f, FeatureBuilder.build(usage, device, TimeContext(4, 59, 0), history)[FeatureSchema.IDX_LATE_NIGHT], 0f)
        assertEquals(0f, FeatureBuilder.build(usage, device, TimeContext(5, 0, 0), history)[FeatureSchema.IDX_LATE_NIGHT], 0f)
    }

    @Test
    fun `cold start defaults produce a valid vector`() {
        val f = FeatureBuilder.build(usage, device, time, ResponseHistory())
        assertEquals("no break ever -> saturated", 1f, f[FeatureSchema.IDX_MIN_SINCE_LAST_BREAK], 1e-6f)
        assertEquals(0.5f, f[FeatureSchema.IDX_ACCEPT_RATE_7D], 1e-6f)
        // Everything except the cyclical features stays in [0,1]
        f.forEachIndexed { i, v ->
            if (i != FeatureSchema.IDX_HOUR_SIN && i != FeatureSchema.IDX_HOUR_COS &&
                i != FeatureSchema.IDX_DOW_SIN && i != FeatureSchema.IDX_DOW_COS
            ) {
                assertTrue("f[$i]=$v out of range", v in 0f..1f)
            }
        }
    }
}
