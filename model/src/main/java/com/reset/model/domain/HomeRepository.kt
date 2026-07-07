package com.reset.model.domain

import com.reset.model.domain.model.HomePreferences
import com.reset.model.domain.model.SessionPreset
import com.reset.model.domain.model.SessionStats
import com.reset.model.domain.model.WeeklyFocus
import kotlinx.coroutines.flow.Flow

/**
 * Persistence boundary for the mindfulness features. Reads are exposed as cold [Flow]s so
 * the ViewModel can drive State reactively; writes are suspending.
 */
interface HomeRepository {

    val preferences: Flow<HomePreferences>

    val stats: Flow<SessionStats>

    /** Trailing-week focus minutes for the Stats bar chart. */
    val weeklyFocus: Flow<WeeklyFocus>

    /** Saved session-builder presets, most recent last. */
    val presets: Flow<List<SessionPreset>>

    suspend fun setDuration(minutes: Int)

    suspend fun setRemindersEnabled(enabled: Boolean)

    suspend fun setReminderEveryMin(minutes: Int)

    suspend fun setReminderStartHour(hour: Int)

    suspend fun setReminderEndHour(hour: Int)

    /** Records a finished focus session: counts, total time, today's bucket, streak. */
    suspend fun recordFocusSession(minutes: Int)

    /** Records a completed mindful break and keeps the daily streak alive. */
    suspend fun recordBreak()

    /** Saves (or replaces, by name) a builder preset, keeping the most recent
     *  [SessionPreset.MAX_PRESETS]. */
    suspend fun savePreset(preset: SessionPreset)
}
