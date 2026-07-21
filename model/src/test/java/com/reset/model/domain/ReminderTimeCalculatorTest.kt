package com.reset.model.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ReminderTimeCalculatorTest {

    private val zone = TimeZone.getTimeZone("UTC")

    private fun at(hour: Int, minute: Int = 0): Long =
        Calendar.getInstance(zone).apply {
            set(2026, Calendar.JULY, 1, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun `candidate inside the window fires exactly one cadence later`() {
        val next = ReminderTimeCalculator.nextTriggerMillis(
            nowMillis = at(10, 0), everyMin = 60, startHour = 9, endHour = 18, timeZone = zone,
        )
        assertEquals(at(11, 0), next)
    }

    @Test
    fun `candidate before the window snaps to today's start hour`() {
        val next = ReminderTimeCalculator.nextTriggerMillis(
            nowMillis = at(7, 0), everyMin = 30, startHour = 9, endHour = 18, timeZone = zone,
        )
        assertEquals(at(9, 0), next)
    }

    @Test
    fun `candidate past the window rolls over to tomorrow's start hour`() {
        val next = ReminderTimeCalculator.nextTriggerMillis(
            nowMillis = at(17, 45), everyMin = 30, startHour = 9, endHour = 18, timeZone = zone,
        )
        // 18:15 is at/after the 18:00 close → 09:00 the next day.
        assertEquals(at(9, 0) + 24 * 60 * 60 * 1000L, next)
    }

    @Test
    fun `candidate exactly at the end hour rolls over`() {
        val next = ReminderTimeCalculator.nextTriggerMillis(
            nowMillis = at(17, 0), everyMin = 60, startHour = 9, endHour = 18, timeZone = zone,
        )
        assertEquals(at(9, 0) + 24 * 60 * 60 * 1000L, next)
    }

    @Test
    fun `midnight-to-midnight window never rolls a candidate forward`() {
        val next = ReminderTimeCalculator.nextTriggerMillis(
            nowMillis = at(23, 30), everyMin = 30, startHour = 0, endHour = 24, timeZone = zone,
        )
        assertEquals(at(23, 30) + 30 * 60 * 1000L, next)
    }

    @Test
    fun `window membership is start-inclusive and end-exclusive`() {
        assertTrue(ReminderTimeCalculator.isWithinWindow(at(9, 0), 9, 18, zone))
        assertTrue(ReminderTimeCalculator.isWithinWindow(at(17, 59), 9, 18, zone))
        assertFalse(ReminderTimeCalculator.isWithinWindow(at(18, 0), 9, 18, zone))
        assertFalse(ReminderTimeCalculator.isWithinWindow(at(8, 59), 9, 18, zone))
    }

    @Test
    fun `a window that wraps midnight (quiet hours' default 22 to 7) is a union, not empty`() {
        assertTrue(ReminderTimeCalculator.isWithinWindow(at(23, 0), 22, 7, zone))
        assertTrue(ReminderTimeCalculator.isWithinWindow(at(22, 0), 22, 7, zone))
        assertTrue(ReminderTimeCalculator.isWithinWindow(at(3, 0), 22, 7, zone))
        assertTrue(ReminderTimeCalculator.isWithinWindow(at(6, 59), 22, 7, zone))
        assertFalse(ReminderTimeCalculator.isWithinWindow(at(7, 0), 22, 7, zone))
        assertFalse(ReminderTimeCalculator.isWithinWindow(at(12, 0), 22, 7, zone))
        assertFalse(ReminderTimeCalculator.isWithinWindow(at(21, 59), 22, 7, zone))
    }
}
