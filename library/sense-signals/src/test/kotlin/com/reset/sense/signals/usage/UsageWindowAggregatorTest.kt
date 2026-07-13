package com.reset.sense.signals.usage

import com.reset.sense.ml.AppCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageWindowAggregatorTest {

    private val aggregator = UsageWindowAggregator(
        windowMs = 5 * 60_000L,
        lookbackMs = 30 * 60_000L,
    )

    private val now = 10 * 60 * 60 * 1000L // 10:00 into the day, arbitrary epoch base
    private val categories = mapOf(
        "work.app" to AppCategory.WORK,
        "social.app" to AppCategory.SOCIAL,
        "chat.app" to AppCategory.CHAT,
        "video.app" to AppCategory.VIDEO,
    )

    private fun categoryOf(pkg: String) = categories[pkg] ?: AppCategory.OTHER

    private fun fg(pkg: String, at: Long) = RawUsageEvent(RawEventType.FOREGROUND, pkg, at)
    private fun bg(pkg: String, at: Long) = RawUsageEvent(RawEventType.BACKGROUND, pkg, at)
    private fun screenOn(at: Long) = RawUsageEvent(RawEventType.SCREEN_ON, "android", at)
    private fun screenOff(at: Long) = RawUsageEvent(RawEventType.SCREEN_OFF, "android", at)
    private fun unlock(at: Long) = RawUsageEvent(RawEventType.UNLOCK, "android", at)

    @Test
    fun `single app filling the window`() {
        val result = aggregator.aggregate(
            events = listOf(screenOn(now - 50 * 60_000), fg("work.app", now - 50 * 60_000)),
            nowMs = now,
            categoryOf = ::categoryOf,
        )
        val usage = result.usage
        assertEquals("full window in foreground", 300, usage.foregroundDurationSec)
        assertEquals(0, usage.appSwitchCount)
        assertEquals(1, usage.uniqueAppCount)
        assertEquals("30-min lookback caps the session", 1800, usage.longestSessionSec)
        assertEquals(300, usage.screenOnTimeSec)
        assertEquals(50, usage.continuousScreenOnMin)
        assertEquals(AppCategory.WORK, usage.foregroundCategory)
    }

    @Test
    fun `app switches counted only inside the window`() {
        val events = listOf(
            // Old switching (outside 5-min window)
            fg("work.app", now - 20 * 60_000), bg("work.app", now - 19 * 60_000),
            fg("chat.app", now - 19 * 60_000), bg("chat.app", now - 18 * 60_000),
            fg("work.app", now - 18 * 60_000), bg("work.app", now - 4 * 60_000),
            // In-window: work → chat → social
            fg("chat.app", now - 4 * 60_000), bg("chat.app", now - 2 * 60_000),
            fg("social.app", now - 2 * 60_000),
        )
        val usage = aggregator.aggregate(events, now, ::categoryOf).usage
        assertEquals("work→chat + chat→social", 2, usage.appSwitchCount)
        assertEquals(3, usage.uniqueAppCount)
        assertEquals(AppCategory.SOCIAL, usage.foregroundCategory)
    }

    @Test
    fun `distracting returns count re-entries in lookback`() {
        val events = listOf(
            // social.app visited 3 times in lookback → 2 returns
            fg("social.app", now - 25 * 60_000), bg("social.app", now - 24 * 60_000),
            fg("work.app", now - 24 * 60_000), bg("work.app", now - 20 * 60_000),
            fg("social.app", now - 20 * 60_000), bg("social.app", now - 18 * 60_000),
            fg("chat.app", now - 18 * 60_000), bg("chat.app", now - 10 * 60_000),
            fg("social.app", now - 10 * 60_000), bg("social.app", now - 8 * 60_000),
            // work.app re-entries never count (not a distracting category)
            fg("work.app", now - 8 * 60_000),
        )
        val usage = aggregator.aggregate(events, now, ::categoryOf).usage
        assertEquals(2, usage.distractingReturnCount)
    }

    @Test
    fun `screen streak resets when screen went off`() {
        val events = listOf(
            fg("work.app", now - 60 * 60_000),
            screenOn(now - 60 * 60_000),
            screenOff(now - 10 * 60_000), // off for 2 minutes
            screenOn(now - 8 * 60_000),
        )
        val usage = aggregator.aggregate(events, now, ::categoryOf).usage
        assertEquals("streak restarts at last screen-on", 8, usage.continuousScreenOnMin)
        // window is fully after the last screen-on → all 300s counted
        assertEquals(300, usage.screenOnTimeSec)
    }

    @Test
    fun `screen currently off means zero streak`() {
        val events = listOf(
            fg("work.app", now - 30 * 60_000),
            screenOn(now - 30 * 60_000),
            screenOff(now - 60_000),
        )
        val usage = aggregator.aggregate(events, now, ::categoryOf).usage
        assertEquals(0, usage.continuousScreenOnMin)
        assertEquals("4 of 5 window minutes were on", 240, usage.screenOnTimeSec)
    }

    @Test
    fun `unlocks cold opens and first unlock minutes`() {
        val events = listOf(
            unlock(now - 9 * 60 * 60_000L), // first unlock of the day, 9h ago
            unlock(now - 50 * 60_000),
            unlock(now - 30 * 60_000),
            unlock(now - 5 * 60_000),
            fg("chat.app", now - 4 * 60_000),
        )
        val result = aggregator.aggregate(
            events, now, ::categoryOf,
            notificationDrivenOpensLastHour = 1,
        )
        assertEquals(3, result.usage.unlockCountLastHour)
        assertEquals("3 unlocks − 1 notification-driven", 2, result.usage.coldOpenCount)
        assertEquals(9 * 60, result.minutesSinceFirstUnlockToday)
    }

    @Test
    fun `last screen off is surfaced for the wind-down success signal`() {
        val offAt = now - 60_000
        val events = listOf(
            fg("work.app", now - 30 * 60_000),
            screenOn(now - 30 * 60_000),
            screenOff(now - 20 * 60_000),
            screenOn(now - 15 * 60_000),
            screenOff(offAt), // most recent off
        )
        assertEquals(offAt, aggregator.aggregate(events, now, ::categoryOf).lastScreenOffMs)
        assertEquals(null, aggregator.aggregate(emptyList(), now, ::categoryOf).lastScreenOffMs)
    }

    @Test
    fun `no events degrades to zeroes and OTHER`() {
        val result = aggregator.aggregate(emptyList(), now, ::categoryOf)
        val usage = result.usage
        assertEquals(0, usage.foregroundDurationSec)
        assertEquals(0, usage.continuousScreenOnMin)
        assertEquals(0, usage.unlockCountLastHour)
        assertEquals(AppCategory.OTHER, usage.foregroundCategory)
        assertEquals(0, result.minutesSinceFirstUnlockToday)
    }

    @Test
    fun `foreground duration clips to window overlap`() {
        val events = listOf(
            fg("work.app", now - 3 * 60_000), // only last 3 min in foreground
        )
        val usage = aggregator.aggregate(events, now, ::categoryOf).usage
        assertEquals(180, usage.foregroundDurationSec)
    }
}
