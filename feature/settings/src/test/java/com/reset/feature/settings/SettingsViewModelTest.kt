package com.reset.feature.settings

import androidx.lifecycle.SavedStateHandle
import com.reset.feature.settings.navigation.SettingsIntent
import com.reset.model.domain.model.HomePreferences
import com.reset.navigation.NavEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.test

class SettingsViewModelTest {

    private fun viewModel(
        preferencesRepository: FakePreferencesRepository = FakePreferencesRepository(),
        reminderScheduler: FakeReminderScheduler = FakeReminderScheduler(),
        navigator: FakeNavigator = FakeNavigator(),
    ) = SettingsViewModel(SavedStateHandle(), preferencesRepository, reminderScheduler, navigator)

    @Test
    fun `loads persisted preferences into state`() = runTest {
        val repo = FakePreferencesRepository().apply {
            preferencesFlow.value = HomePreferences(
                quietHoursEnabled = false,
                quietHoursStartHour = 21,
                quietHoursEndHour = 6,
                remindersEnabled = false,
                soundsEnabled = false,
            )
        }

        viewModel(preferencesRepository = repo).test(this) {
            expectInitialState()
            runOnCreate()

            val loaded = awaitUntil { it.loaded }
            assertFalse(loaded.quietHoursEnabled)
            assertEquals(21, loaded.quietHoursStart)
            assertEquals(6, loaded.quietHoursEnd)
            assertFalse(loaded.remindersEnabled)
            assertFalse(loaded.soundsEnabled)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `quiet hours toggle persists and flows back into state`() = runTest {
        val repo = FakePreferencesRepository()

        viewModel(preferencesRepository = repo).test(this) {
            expectInitialState()
            runOnCreate()
            awaitUntil { it.loaded }

            containerHost.handleSettingsIntent(SettingsIntent.ToggleQuietHours)

            awaitUntil { !it.quietHoursEnabled }
            assertFalse("persisted, not just local state", repo.preferencesFlow.value.quietHoursEnabled)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `break reminders toggle persists and re-arms the reminder chain`() = runTest {
        val repo = FakePreferencesRepository()
        val scheduler = FakeReminderScheduler()

        viewModel(preferencesRepository = repo, reminderScheduler = scheduler).test(this) {
            expectInitialState()
            runOnCreate()
            awaitUntil { it.loaded }

            containerHost.handleSettingsIntent(SettingsIntent.ToggleBreakReminders)

            awaitUntil { !it.remindersEnabled }
            assertFalse(repo.preferencesFlow.value.remindersEnabled)
            assertEquals(1, scheduler.preferencesChangedCount)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `sounds toggle persists`() = runTest {
        val repo = FakePreferencesRepository()

        viewModel(preferencesRepository = repo).test(this) {
            expectInitialState()
            runOnCreate()
            awaitUntil { it.loaded }

            containerHost.handleSettingsIntent(SettingsIntent.ToggleSounds)

            awaitUntil { !it.soundsEnabled }
            assertFalse(repo.preferencesFlow.value.soundsEnabled)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `hour picker opens, persists the chosen bound, and closes`() = runTest {
        val repo = FakePreferencesRepository()

        viewModel(preferencesRepository = repo).test(this) {
            expectInitialState()
            runOnCreate()
            awaitUntil { it.loaded }

            containerHost.handleSettingsIntent(
                SettingsIntent.OpenHourPicker(HourPickerTarget.QuietStart),
            )
            awaitUntil { it.hourPicker == HourPickerTarget.QuietStart }

            containerHost.handleSettingsIntent(SettingsIntent.SelectHour(23))

            val updated = awaitUntil { it.quietHoursStart == 23 }
            assertNull(updated.hourPicker)
            assertEquals(23, repo.preferencesFlow.value.quietHoursStartHour)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `hour picker dismiss closes without persisting`() = runTest {
        val repo = FakePreferencesRepository()

        viewModel(preferencesRepository = repo).test(this) {
            expectInitialState()
            runOnCreate()
            awaitUntil { it.loaded }

            containerHost.handleSettingsIntent(
                SettingsIntent.OpenHourPicker(HourPickerTarget.QuietEnd),
            )
            awaitUntil { it.hourPicker == HourPickerTarget.QuietEnd }

            containerHost.handleSettingsIntent(SettingsIntent.DismissHourPicker)

            val closed = awaitUntil { it.hourPicker == null }
            assertEquals(
                HomePreferences.DEFAULT_QUIET_HOURS_END,
                repo.preferencesFlow.value.quietHoursEndHour,
            )
            assertTrue(closed.loaded)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `back press pops via the navigator`() = runTest {
        val navigator = FakeNavigator()

        viewModel(navigator = navigator).test(this) {
            expectInitialState()
            containerHost.handleSettingsIntent(SettingsIntent.HandleBackPress)

            assertEquals(NavEvent.Pop, navigator.events.first())

            cancelAndIgnoreRemainingItems()
        }
    }
}
