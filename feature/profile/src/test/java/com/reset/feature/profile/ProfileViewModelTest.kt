package com.reset.feature.profile

import androidx.lifecycle.SavedStateHandle
import com.reset.feature.profile.navigation.ProfileIntent
import com.reset.model.domain.model.SessionStats
import com.reset.model.domain.model.WeeklyFocus
import com.reset.navigation.NavEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.orbitmvi.orbit.test.test

class ProfileViewModelTest {

    private fun viewModel(
        preferencesRepository: FakePreferencesRepository = FakePreferencesRepository(),
        statsRepository: FakeStatsRepository = FakeStatsRepository(),
        navigator: FakeNavigator = FakeNavigator(),
    ) = ProfileViewModel(SavedStateHandle(), preferencesRepository, statsRepository, navigator)

    @Test
    fun `loads persisted stats into state`() = runTest {
        val repo = FakeStatsRepository(
            stats = SessionStats(sessions = 3, totalMin = 75, streak = 12, breaksTaken = 45),
        )

        viewModel(statsRepository = repo).test(this) {
            expectInitialState()
            runOnCreate()

            val loaded = awaitUntil { it.loaded }
            assertEquals(45, loaded.breaksTaken)
            assertEquals(12, loaded.streakDays)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `loads the weekly focus buckets into state`() = runTest {
        val minutes = listOf(58, 84, 46, 96, 70, 30, 20)
        val repo = FakeStatsRepository(
            weeklyFocus = WeeklyFocus(minutesPerDay = minutes, todayIndex = 4),
        )

        viewModel(statsRepository = repo).test(this) {
            expectInitialState()
            runOnCreate()

            val loaded = awaitUntil { it.focusMinutesPerDay == minutes }
            assertEquals(4, loaded.todayIndex)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `later stat writes keep flowing into state`() = runTest {
        val repo = FakeStatsRepository(stats = SessionStats(breaksTaken = 1, streak = 1))

        viewModel(statsRepository = repo).test(this) {
            expectInitialState()
            runOnCreate()
            awaitUntil { it.loaded }

            repo.statsFlow.value = SessionStats(breaksTaken = 2, streak = 2)

            val updated = awaitUntil { it.breaksTaken == 2 }
            assertEquals(2, updated.streakDays)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `quiet hours load from preferences and steppers persist with wrap`() = runTest {
        val repo = FakePreferencesRepository()

        viewModel(preferencesRepository = repo).test(this) {
            expectInitialState()
            runOnCreate()

            // Defaults surface from preferences (22 → 7).
            awaitUntil { it.quietHoursStart == 22 && it.quietHoursEnd == 7 }

            // 22 + 3 wraps past midnight to 1.
            containerHost.handleProfileIntent(ProfileIntent.AdjustQuietHoursStart(+3))
            val wrapped = awaitUntil { it.quietHoursStart == 1 }
            assertEquals("persisted, not just local state", 1, repo.preferencesFlow.value.quietHoursStartHour)
            assertEquals(7, wrapped.quietHoursEnd)

            // 7 − 8 wraps backwards to 23.
            containerHost.handleProfileIntent(ProfileIntent.AdjustQuietHoursEnd(-8))
            awaitUntil { it.quietHoursEnd == 23 }
            assertEquals(23, repo.preferencesFlow.value.quietHoursEndHour)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `back press pops via the navigator`() = runTest {
        val navigator = FakeNavigator()

        viewModel(navigator = navigator).test(this) {
            expectInitialState()
            containerHost.handleProfileIntent(ProfileIntent.HandleBackPress)

            assertEquals(NavEvent.Pop, navigator.events.first())

            cancelAndIgnoreRemainingItems()
        }
    }
}
