package com.reset.feature.home

import androidx.lifecycle.SavedStateHandle
import com.reset.feature.builder.api.BuilderDestination
import com.reset.feature.home.navigation.HomeIntent
import com.reset.feature.sessions.api.SessionDestination
import com.reset.feature.sessions.api.SessionsDestination
import com.reset.model.domain.CelebrationEvent
import com.reset.model.domain.StartupState
import com.reset.model.domain.model.HomePreferences
import com.reset.model.domain.model.SessionPreset
import com.reset.model.domain.model.SessionStats
import com.reset.navigation.NavEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.test

class HomeViewModelTest {

    private class Harness(
        val repository: FakeHomeRepository = FakeHomeRepository(),
        val navigator: FakeNavigator = FakeNavigator(),
        val celebrationStore: FakeCelebrationStore = FakeCelebrationStore(),
        val startupState: StartupState = StartupState(),
        val timeProvider: FakeTimeProvider = FakeTimeProvider(),
    ) {
        val viewModel = HomeViewModel(
            SavedStateHandle(),
            repository,
            navigator,
            celebrationStore,
            startupState,
            timeProvider,
        )

        /** Suspends until the next navigation event — the assertion point for nav intents. */
        suspend fun awaitNavEvent(): NavEvent = navigator.events.first()
    }

    @Test
    fun `loads preferences, stats into content state`() = runTest {
        val harness = Harness(
            repository = FakeHomeRepository(
                preferences = HomePreferences(durationMin = 10),
                stats = SessionStats(sessions = 4, streak = 3, breaksTaken = 7),
            ),
        )

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()

            val loaded = awaitUntil { it.status == HomeStatus.Content }
            assertEquals(10, loaded.durationMin)
            assertEquals(3, loaded.stats.streak)
            assertTrue(harness.startupState.contentReady.value)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a failed load surfaces the error state and still releases the splash`() = runTest {
        val harness = Harness(repository = FakeHomeRepository(failPreferences = true))

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()

            val failed = awaitUntil { it.status is HomeStatus.Error }
            assertEquals("boom", (failed.status as HomeStatus.Error).message)
            assertTrue(harness.startupState.contentReady.value)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `the meditate hero starts a focus session with the preset interval`() = runTest {
        val harness = Harness(
            repository = FakeHomeRepository(preferences = HomePreferences(durationMin = 30)),
        )

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()
            awaitUntil { it.status == HomeStatus.Content }

            containerHost.handleHomeIntent(HomeIntent.StartMeditate)

            val event = harness.awaitNavEvent() as NavEvent.Navigate
            val destination = event.screen as SessionDestination
            assertEquals(SessionDestination.MODE_FOCUS, destination.mode)
            assertEquals(30, destination.durationMin)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `the deep breathing tile starts a breathing break`() = runTest {
        val harness = Harness()

        harness.viewModel.test(this) {
            expectInitialState()
            containerHost.handleHomeIntent(HomeIntent.StartDeepBreathing)

            val event = harness.awaitNavEvent() as NavEvent.Navigate
            val destination = event.screen as SessionDestination
            assertEquals(SessionDestination.MODE_BREAK, destination.mode)
            assertEquals(SessionDestination.KIND_BREATHING, destination.breakKind)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `the builder step opens BuilderDestination`() = runTest {
        val harness = Harness()

        harness.viewModel.test(this) {
            expectInitialState()
            containerHost.handleHomeIntent(HomeIntent.OpenBuilder)

            val event = harness.awaitNavEvent() as NavEvent.Navigate
            assertEquals(BuilderDestination, event.screen)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `explore more switches to the sessions tab`() = runTest {
        val harness = Harness()

        harness.viewModel.test(this) {
            expectInitialState()
            containerHost.handleHomeIntent(HomeIntent.ExploreMore)

            assertEquals(NavEvent.SwitchTab(SessionsDestination), harness.awaitNavEvent())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a finished break pops the celebration banner with the current streak`() = runTest {
        val harness = Harness(
            repository = FakeHomeRepository(stats = SessionStats(streak = 5)),
        )

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()
            awaitUntil { it.status == HomeStatus.Content }

            harness.celebrationStore.dispatch(CelebrationEvent.BreakFinished)

            val celebrating = awaitUntil { it.celebration != null }
            assertEquals(5, celebrating.celebration?.streakDays)
            assertEquals(
                listOf<Any>(CelebrationEvent.BreakFinished),
                harness.celebrationStore.consumed,
            )

            cancelAndIgnoreRemainingItems()
        }
    }
}
