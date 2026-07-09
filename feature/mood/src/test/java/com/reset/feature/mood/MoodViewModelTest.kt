package com.reset.feature.mood

import androidx.lifecycle.SavedStateHandle
import com.reset.feature.mood.navigation.MoodIntent
import com.reset.feature.mood.navigation.MoodSideEffect
import com.reset.feature.mood.ui.MoodTone
import com.reset.model.domain.model.ChimeKind
import com.reset.navigation.NavEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.test

class MoodViewModelTest {

    private fun viewModel(
        repository: FakeHomeRepository = FakeHomeRepository(),
        navigator: FakeNavigator = FakeNavigator(),
    ) = MoodViewModel(SavedStateHandle(), repository, navigator)

    @Test
    fun `opens centred on the neutral band`() = runTest {
        viewModel().test(this) {
            val initial = awaitState()
            assertEquals(MoodState.DEFAULT_LEVEL, initial.level)
            assertEquals(MoodTone.Okay, initial.tone)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `moving the slider updates the level and derived tone`() = runTest {
        viewModel().test(this) {
            expectInitialState()

            containerHost.handleMoodIntent(MoodIntent.LevelChanged(90))
            val great = awaitUntil { it.level == 90 }
            assertEquals(MoodTone.Great, great.tone)

            containerHost.handleMoodIntent(MoodIntent.LevelChanged(10))
            val rough = awaitUntil { it.level == 10 }
            assertEquals(MoodTone.Rough, rough.tone)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `out-of-range slider values are clamped`() = runTest {
        viewModel().test(this) {
            expectInitialState()

            containerHost.handleMoodIntent(MoodIntent.LevelChanged(250))
            val clamped = awaitUntil { it.level == MoodConstants.LEVEL_MAX }
            assertEquals(MoodConstants.LEVEL_MAX, clamped.level)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `saving records the mood, chimes and pops back`() = runTest {
        val repo = FakeHomeRepository()
        val navigator = FakeNavigator()

        viewModel(repo, navigator).test(this) {
            expectInitialState()

            containerHost.handleMoodIntent(MoodIntent.LevelChanged(72))
            awaitUntil { it.level == 72 }

            containerHost.handleMoodIntent(MoodIntent.SaveMood)

            assertEquals(MoodSideEffect.PlayChime(ChimeKind.End), awaitSideEffect())
            assertEquals(NavEvent.Pop, navigator.events.first())
            assertEquals(listOf(72), repo.recordedMoods)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `dismissing pops back without recording a mood`() = runTest {
        val repo = FakeHomeRepository()
        val navigator = FakeNavigator()

        viewModel(repo, navigator).test(this) {
            expectInitialState()

            containerHost.handleMoodIntent(MoodIntent.Dismiss)

            assertEquals(NavEvent.Pop, navigator.events.first())
            assertTrue(repo.recordedMoods.isEmpty())

            cancelAndIgnoreRemainingItems()
        }
    }
}
