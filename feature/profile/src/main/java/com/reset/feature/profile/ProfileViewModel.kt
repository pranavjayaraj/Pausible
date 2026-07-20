package com.reset.feature.profile

import androidx.lifecycle.SavedStateHandle
import com.reset.core.mvi.BaseViewModel
import com.reset.feature.profile.navigation.ProfileIntent
import com.reset.feature.profile.navigation.ProfileSideEffect
import com.reset.feature.settings.api.SettingsDestination
import com.reset.model.domain.stats.StatsRepository
import com.reset.navigation.Navigator
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.reduce
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val statsRepository: StatsRepository,
    private val navigator: Navigator,
) : BaseViewModel<ProfileState, ProfileSideEffect>(savedStateHandle) {

    override fun initialState() = ProfileState.getDefault()

    override fun initData() {
        observeStats()
        observeWeeklyFocus()
    }

    fun handleProfileIntent(intent: ProfileIntent) = when (intent) {
        ProfileIntent.HandleBackPress -> close()
        ProfileIntent.OpenSettings -> openSettings()
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

    private fun openSettings() = intent {
        navigator.navigate(SettingsDestination)
    }

    private fun close() = intent {
        navigator.pop()
    }
}
