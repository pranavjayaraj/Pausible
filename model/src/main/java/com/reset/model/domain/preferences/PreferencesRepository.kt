package com.reset.model.domain.preferences

import com.reset.model.domain.model.HomePreferences
import kotlinx.coroutines.flow.Flow

/**
 * Persistence boundary for user settings (session duration, reminder cadence/window,
 * Sense quiet hours). Reads are exposed as a cold [Flow] so ViewModels can drive state
 * reactively; writes are suspending.
 */
interface PreferencesRepository {

    val preferences: Flow<HomePreferences>

    suspend fun setDuration(minutes: Int)

    suspend fun setRemindersEnabled(enabled: Boolean)

    suspend fun setReminderEveryMin(minutes: Int)

    suspend fun setReminderStartHour(hour: Int)

    suspend fun setReminderEndHour(hour: Int)

    /** Sense quiet-hours bounds, 24h clock 0..23; the window wraps midnight. */
    suspend fun setQuietHoursStart(hour: Int)

    suspend fun setQuietHoursEnd(hour: Int)
}
