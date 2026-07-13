package com.reset.repository.notification

import android.content.Context
import androidx.work.ExistingWorkPolicy
import com.reset.model.domain.preferences.PreferencesRepository
import com.reset.model.domain.ReminderScheduler
import com.reset.model.domain.ReminderTimeCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * [ReminderScheduler] over [ReminderNotificationWork] unique work. Settings changes
 * REPLACE the pending chain (new cadence/window takes effect immediately); app-start
 * syncs KEEP it (an in-flight countdown is never reset).
 */
class ReminderSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
) : ReminderScheduler {

    override suspend fun onPreferencesChanged() = sync(ExistingWorkPolicy.REPLACE)

    override suspend fun ensureScheduled() = sync(ExistingWorkPolicy.KEEP)

    private suspend fun sync(policy: ExistingWorkPolicy) {
        val prefs = preferencesRepository.preferences.first()
        if (!prefs.remindersEnabled) {
            ReminderNotificationWork.cancel(context)
            return
        }
        val now = System.currentTimeMillis()
        val delay = ReminderTimeCalculator.nextTriggerMillis(
            nowMillis = now,
            everyMin = prefs.remindersEveryMin,
            startHour = prefs.remindersStartHour,
            endHour = prefs.remindersEndHour,
        ) - now
        ReminderNotificationWork.schedule(context, delay, policy)
    }
}
