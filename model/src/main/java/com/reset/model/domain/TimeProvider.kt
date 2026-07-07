package com.reset.model.domain

/**
 * Clock seam for everything calendar-shaped (streaks, weekly buckets, the time-of-day
 * builder suggestion, the fact-of-the-day). Pure data so ViewModels and the repository
 * stay deterministic under test.
 */
interface TimeProvider {

    /** Whole days since the Unix epoch, in the device's timezone. */
    fun todayEpochDay(): Long

    /** 0 = Monday … 6 = Sunday, matching [com.reset.model.domain.model.WeeklyFocus]. */
    fun dayOfWeekIndex(): Int

    /** 0–23, drives the builder's time-of-day suggestion. */
    fun hourOfDay(): Int

    /** 1–31, rotates the Home fact of the day. */
    fun dayOfMonth(): Int
}
