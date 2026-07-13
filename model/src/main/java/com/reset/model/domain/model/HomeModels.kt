package com.reset.model.domain.model

/** User preferences persisted across launches. */
data class HomePreferences(
    val durationMin: Int = DEFAULT_DURATION_MIN,
    val remindersEnabled: Boolean = true,
    val remindersEveryMin: Int = DEFAULT_REMINDER_EVERY_MIN,
    val remindersStartHour: Int = DEFAULT_REMINDER_START_HOUR,
    val remindersEndHour: Int = DEFAULT_REMINDER_END_HOUR,
    /** Sense quiet hours — the engine never prompts (wind-down excepted) inside
     *  [quietHoursStartHour, quietHoursEndHour); wraps midnight. Distinct from
     *  the reminder window above, which is when the reminder chain fires. */
    val quietHoursStartHour: Int = DEFAULT_QUIET_HOURS_START,
    val quietHoursEndHour: Int = DEFAULT_QUIET_HOURS_END,
) {
    companion object {
        /** The Meditate hero card's preset interval, per the design. */
        const val DEFAULT_DURATION_MIN = 25

        const val DEFAULT_REMINDER_EVERY_MIN = 60
        const val DEFAULT_REMINDER_START_HOUR = 9
        const val DEFAULT_REMINDER_END_HOUR = 18

        const val DEFAULT_QUIET_HOURS_START = 22
        const val DEFAULT_QUIET_HOURS_END = 7

        /** Reminder cadences offered by the Settings frequency grid, in minutes.
         *  2 min is a temporary testing cadence — remove before release. */
        val REMINDER_EVERY_OPTIONS = listOf(2, 30, 60, 90, 120)

        /** Earliest / latest hour a reminder window boundary may take (24h clock). */
        const val MIN_HOUR = 0
        const val MAX_HOUR = 24
    }
}

/** Aggregate mindfulness history shown on Home and Stats. */
data class SessionStats(
    val sessions: Int = 0,
    val totalMin: Int = 0,
    val streak: Int = 0,
    val breaksTaken: Int = 0,
) {
    /** No sits recorded yet — Home renders its "your first sit" empty state. */
    val isFirstSit: Boolean get() = sessions == 0
}

/**
 * Focus minutes for the trailing week, bucketed by weekday for the Stats bar chart.
 * [minutesPerDay] is Monday-first and always [DAYS_PER_WEEK] long; [todayIndex] marks the
 * highlighted bar.
 */
data class WeeklyFocus(
    val minutesPerDay: List<Int> = List(DAYS_PER_WEEK) { 0 },
    val todayIndex: Int = 0,
) {
    companion object {
        const val DAYS_PER_WEEK = 7
    }
}

/**
 * A saved session-builder configuration ("My sessions" in the builder). Option fields hold
 * the feature's stable option keys (sound/intent/reminder), never display strings.
 */
@kotlinx.serialization.Serializable
data class SessionPreset(
    val name: String,
    val durationMin: Int,
    val soundKey: String,
    val layerSoundKey: String? = null,
    val mixPercent: Int = DEFAULT_MIX_PERCENT,
    val paceSec: Int,
    val bellMin: Int = 0,
    val gong: Boolean = true,
    val guidedVoice: Boolean = true,
    val warmup: Boolean = true,
    val intentKey: String,
    val remindKey: String,
) {
    companion object {
        const val DEFAULT_MIX_PERCENT = 60

        /** The builder keeps only the most recent presets, like the design. */
        const val MAX_PRESETS = 4
    }
}

/** Which singing-bowl chime to play. */
enum class ChimeKind { Start, End }

/**
 * One "science of closing your eyes" card. The copy lives in string resources
 * ([com.reset.feature.home.R.array.afk_eye_fact_texts] / `_sources`); a card is
 * identified purely by its [index] so the domain layer stays free of UI strings.
 */
data class EyeFact(val index: Int) {
    companion object {
        /** Must match the size of the parallel eye-fact string arrays. */
        const val COUNT = 5
    }
}
