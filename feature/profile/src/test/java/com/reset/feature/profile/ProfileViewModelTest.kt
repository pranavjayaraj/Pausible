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
        repository: FakeHomeRepository = FakeHomeRepository(),
        navigator: FakeNavigator = FakeNavigator(),
    ) = ProfileViewModel(SavedStateHandle(), repository, navigator)

    @Test
    fun `loads persisted stats into state`() = runTest {
        val repo = FakeHomeRepository(
            stats = SessionStats(sessions = 3, totalMin = 75, streak = 12, breaksTaken = 45),
        )

        viewModel(repo).test(this) {
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
        val repo = FakeHomeRepository(
            weeklyFocus = WeeklyFocus(minutesPerDay = minutes, todayIndex = 4),
        )

        viewModel(repo).test(this) {
            expectInitialState()
            runOnCreate()

            val loaded = awaitUntil { it.focusMinutesPerDay == minutes }
            assertEquals(4, loaded.todayIndex)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `later stat writes keep flowing into state`() = runTest {
        val repo = FakeHomeRepository(stats = SessionStats(breaksTaken = 1, streak = 1))

        viewModel(repo).test(this) {
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
