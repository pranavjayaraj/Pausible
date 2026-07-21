package com.reset.feature.home

import androidx.compose.ui.unit.dp
import java.util.Calendar

/** All timing, dimension, and formatting constants for the Home feature. */
object HomeConstants {

    // ── Timing ────────────────────────────────────────────────
    /** How long the "Nice breather!" banner stays up. */
    const val CELEBRATION_VISIBLE_MS = 3_200L

    // ── Dimens ────────────────────────────────────────────────
    val sectionSpacing = 22.dp
    val cardSpacing = 14.dp
    val gridSpacing = 10.dp
    val bannerMascotSize = 46.dp
    val checkInMascotSize = 40.dp
    val quickRowIconSize = 44.dp

    /** [0, 24) hour → coarse day period; [DayPeriod.NIGHT] also drives the check-in card's
     *  night theme. Boundaries mirror the design's "post-22:00" night cue. */
    fun dayPeriodFor(hour: Int): DayPeriod = when (hour) {
        in 5..11 -> DayPeriod.MORNING
        in 12..16 -> DayPeriod.AFTERNOON
        in 17..21 -> DayPeriod.EVENING
        else -> DayPeriod.NIGHT
    }

    /** Formats an epoch-millis instant as a lowercase "5:00 pm" clock time, device timezone. */
    fun formatClockTime(epochMillis: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = epochMillis }
        val hour24 = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val period = if (hour24 < 12) "am" else "pm"
        val hour12 = when (val h = hour24 % 12) {
            0 -> 12
            else -> h
        }
        return "$hour12:${minute.toString().padStart(2, '0')} $period"
    }
}
