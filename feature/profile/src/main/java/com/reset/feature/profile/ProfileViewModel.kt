package com.reset.feature.profile

import androidx.lifecycle.SavedStateHandle
import com.reset.core.mvi.BaseViewModel
import com.reset.feature.profile.navigation.ProfileIntent
import com.reset.feature.profile.navigation.ProfileSideEffect
import com.reset.model.domain.preferences.PreferencesRepository
import com.reset.model.domain.stats.StatsRepository
import com.reset.navigation.Navigator
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.reduce
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val preferencesRepository: PreferencesRepository,
    private val statsRepository: StatsRepository,
    private val navigator: Navigator,
) : BaseViewModel<ProfileState, ProfileSideEffect>(savedStateHandle) {

    override fun initialState() = ProfileState.getDefault()

    override fun initData() {
        observeStats()
        observeWeeklyFocus()
        observePreferences()
    }

    fun handleProfileIntent(intent: ProfileIntent) = when (intent) {
        ProfileIntent.HandleBackPress -> close()
        is ProfileIntent.AdjustQuietHoursStart -> adjustQuietHoursStart(intent.deltaHours)
        is ProfileIntent.AdjustQuietHoursEnd -> adjustQuietHoursEnd(intent.deltaHours)
    }

    /** Reflects the persisted session history into state and tracks later writes. */
    private fun observeStats() = intent {
        statsRepository.stats.collect { stats ->
            reduce {
                state.copy(
                    loaded = true,
                    breaksTaken = stats.breaksTaken,
                    streakDays = stats.streak,
                )
            }
        }
    }

    private fun observeWeeklyFocus() = intent {
        statsRepository.weeklyFocus.collect { weekly ->
            reduce {
                state.copy(
                    focusMinutesPerDay = weekly.minutesPerDay,
                    todayIndex = weekly.todayIndex,
                )
            }
        }
    }

    /** Reflects the persisted quiet-hours bounds; the steppers write through here. */
    private fun observePreferences() = intent {
        preferencesRepository.preferences.collect { prefs ->
            reduce {
                state.copy(
                    quietHoursStart = prefs.quietHoursStartHour,
                    quietHoursEnd = prefs.quietHoursEndHour,
                )
            }
        }
    }

    /** Persist-only: state updates flow back through [observePreferences], keeping
     *  DataStore the single source of truth (no optimistic local copy to drift). */
    private fun adjustQuietHoursStart(deltaHours: Int) = intent {
        preferencesRepository.setQuietHoursStart(wrapHour(state.quietHoursStart + deltaHours))
    }

    private fun adjustQuietHoursEnd(deltaHours: Int) = intent {
        preferencesRepository.setQuietHoursEnd(wrapHour(state.quietHoursEnd + deltaHours))
    }

    private fun wrapHour(hour: Int): Int = ((hour % HOURS_PER_DAY) + HOURS_PER_DAY) % HOURS_PER_DAY

    private fun close() = intent {
        navigator.pop()
    }

    private companion object {
        const val HOURS_PER_DAY = 24
    }
}
