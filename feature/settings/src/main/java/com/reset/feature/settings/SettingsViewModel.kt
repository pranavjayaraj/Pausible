package com.reset.feature.settings

import androidx.lifecycle.SavedStateHandle
import com.reset.core.mvi.BaseViewModel
import com.reset.feature.settings.navigation.SettingsIntent
import com.reset.feature.settings.navigation.SettingsSideEffect
import com.reset.model.domain.ReminderScheduler
import com.reset.model.domain.preferences.PreferencesRepository
import com.reset.navigation.Navigator
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.reduce
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val preferencesRepository: PreferencesRepository,
    private val reminderScheduler: ReminderScheduler,
    private val navigator: Navigator,
) : BaseViewModel<SettingsState, SettingsSideEffect>(savedStateHandle) {

    override fun initialState() = SettingsState.getDefault()

    override fun initData() {
        observePreferences()
    }

    fun handleSettingsIntent(intent: SettingsIntent) = when (intent) {
        SettingsIntent.HandleBackPress -> close()
        SettingsIntent.ToggleQuietHours -> toggleQuietHours()
        SettingsIntent.ToggleBreakReminders -> toggleBreakReminders()
        SettingsIntent.ToggleSounds -> toggleSounds()
        is SettingsIntent.OpenHourPicker -> openHourPicker(intent.target)
        SettingsIntent.DismissHourPicker -> dismissHourPicker()
        is SettingsIntent.SelectHour -> selectHour(intent.hour)
    }

    /** Reflects the persisted preferences into state and tracks later writes. */
    private fun observePreferences() = intent {
        preferencesRepository.preferences.collect { prefs ->
            reduce {
                state.copy(
                    loaded = true,
                    quietHoursEnabled = prefs.quietHoursEnabled,
                    quietHoursStart = prefs.quietHoursStartHour,
                    quietHoursEnd = prefs.quietHoursEndHour,
                    remindersEnabled = prefs.remindersEnabled,
                    soundsEnabled = prefs.soundsEnabled,
                )
            }
        }
    }

    /** Persist-only: state updates flow back through [observePreferences], keeping
     *  DataStore the single source of truth (no optimistic local copy to drift). */
    private fun toggleQuietHours() = intent {
        preferencesRepository.setQuietHoursEnabled(!state.quietHoursEnabled)
    }

    private fun toggleBreakReminders() = intent {
        preferencesRepository.setRemindersEnabled(!state.remindersEnabled)
        // Re-arm (or cancel) the pending reminder chain so the switch takes
        // effect immediately, per the ReminderScheduler contract.
        reminderScheduler.onPreferencesChanged()
    }

    private fun toggleSounds() = intent {
        preferencesRepository.setSoundsEnabled(!state.soundsEnabled)
    }

    private fun openHourPicker(target: HourPickerTarget) = intent {
        reduce { state.copy(hourPicker = target) }
    }

    private fun dismissHourPicker() = intent {
        reduce { state.copy(hourPicker = null) }
    }

    private fun selectHour(hour: Int) = intent {
        when (state.hourPicker) {
            HourPickerTarget.QuietStart -> preferencesRepository.setQuietHoursStart(hour)
            HourPickerTarget.QuietEnd -> preferencesRepository.setQuietHoursEnd(hour)
            null -> Unit
        }
        reduce { state.copy(hourPicker = null) }
    }

    private fun close() = intent {
        navigator.pop()
    }
}
