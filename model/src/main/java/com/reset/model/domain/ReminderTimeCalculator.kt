package com.reset.model.domain

import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Pure time math for the reminder schedule: given "now" and the persisted cadence/window,
 * yields the epoch millis of the next reminder. Keeping it here (not in the worker) makes
 * the windowing rules unit-testable without Android.
 */
object ReminderTimeCalculator {

    /**
     * The next reminder fires [everyMin] minutes from now, clamped into the daily window:
     * a candidate before today's [startHour] snaps forward to it, and a candidate at/after
     * [endHour] rolls over to [startHour] the next day.
     */
    fun nextTriggerMillis(
        nowMillis: Long,
        everyMin: Int,
        startHour: Int,
        endHour: Int,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): Long {
        val candidate = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            add(Calendar.MINUTE, everyMin)
        }
        val dayStart = (candidate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val windowStart = dayStart.timeInMillis + TimeUnit.HOURS.toMillis(startHour.toLong())
        val windowEnd = dayStart.timeInMillis + TimeUnit.HOURS.toMillis(endHour.toLong())
        return when {
            candidate.timeInMillis < windowStart -> windowStart
            candidate.timeInMillis >= windowEnd -> {
                dayStart.add(Calendar.DAY_OF_MONTH, 1)
                dayStart.timeInMillis + TimeUnit.HOURS.toMillis(startHour.toLong())
            }
            else -> candidate.timeInMillis
        }
    }

    /** True when [nowMillis] falls inside the `[startHour, endHour)` daily window —
     *  including a window that wraps midnight (`startHour > endHour`, e.g. quiet hours'
     *  default 22..7), where membership is the union of "after start" and "before end". */
    fun isWithinWindow(
        nowMillis: Long,
        startHour: Int,
        endHour: Int,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): Boolean {
        val dayStart = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val windowStart = dayStart.timeInMillis + TimeUnit.HOURS.toMillis(startHour.toLong())
        val windowEnd = dayStart.timeInMillis + TimeUnit.HOURS.toMillis(endHour.toLong())
        return if (startHour <= endHour) {
            nowMillis in windowStart until windowEnd
        } else {
            nowMillis >= windowStart || nowMillis < windowEnd
        }
    }
}
