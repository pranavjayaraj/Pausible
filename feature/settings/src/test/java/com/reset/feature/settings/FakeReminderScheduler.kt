package com.reset.feature.settings

import com.reset.model.domain.ReminderScheduler

/** Counts re-arm calls so tests can assert the reminder chain is refreshed on toggle. */
class FakeReminderScheduler : ReminderScheduler {

    var preferencesChangedCount = 0
        private set

    override suspend fun onPreferencesChanged() {
        preferencesChangedCount++
    }

    override suspend fun ensureScheduled() = Unit
}
