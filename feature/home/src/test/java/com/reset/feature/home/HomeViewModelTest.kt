package com.reset.feature.home

import androidx.lifecycle.SavedStateHandle
import com.reset.feature.builder.api.BuilderDestination
import com.reset.feature.home.navigation.HomeIntent
import com.reset.feature.sessions.api.SessionDestination
import com.reset.feature.sessions.api.SessionScriptIds
import com.reset.model.domain.CelebrationEvent
import com.reset.model.domain.StartupState
import com.reset.model.domain.model.HomePreferences
import com.reset.model.domain.model.SessionStats
import com.reset.navigation.NavEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.test

class HomeViewModelTest {

    private class Harness(
        val preferencesRepository: FakePreferencesRepository = FakePreferencesRepository(),
        val statsRepository: FakeStatsRepository = FakeStatsRepository(),
        val navigator: FakeNavigator = FakeNavigator(),
        val celebrationStore: FakeCelebrationStore = FakeCelebrationStore(),
        val startupState: StartupState = StartupState(),
        val timeProvider: FakeTimeProvider = FakeTimeProvider(),
        val senseSuggestionRepository: FakeSenseSuggestionRepository = FakeSenseSuggestionRepository(),
    ) {
        val viewModel = HomeViewModel(
            SavedStateHandle(),
            preferencesRepository,
            statsRepository,
            navigator,
            celebrationStore,
            startupState,
            timeProvider,
            senseSuggestionRepository,
        )

        /** Suspends until the next navigation event — the assertion point for nav intents. */
        suspend fun awaitNavEvent(): NavEvent = navigator.events.first()
    }

    @Test
    fun `loads preferences, stats into a resting check-in card`() = runTest {
        val harness = Harness(
            statsRepository = FakeStatsRepository(
                stats = SessionStats(streak = 3),
                todayBreaksTaken = 2,
            ),
        )

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()

            val loaded = awaitUntil { it.status == HomeStatus.Content }
            assertEquals(3, loaded.streak)
            assertEquals(2, loaded.todayPauses)
            assertEquals(CheckInCardState.Resting, loaded.checkIn)
            assertTrue(harness.startupState.contentReady.value)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a failed load surfaces the error state and still releases the splash`() = runTest {
        val harness = Harness(
            preferencesRepository = FakePreferencesRepository(failPreferences = true),
        )

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
    fun `a pending sitting-stretch prompt shows the Sense pre-lit check-in card`() = runTest {
        val harness = Harness(
            timeProvider = FakeTimeProvider(hour = 14),
            senseSuggestionRepository = FakeSenseSuggestionRepository(pendingDecisionId = 42L),
        )

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()

            val loaded = awaitUntil { it.status == HomeStatus.Content }
            assertEquals(CheckInCardState.SensePreLit(42L), loaded.checkIn)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `night hours show the night card, overriding any pending prompt`() = runTest {
        val harness = Harness(
            timeProvider = FakeTimeProvider(hour = 23),
            senseSuggestionRepository = FakeSenseSuggestionRepository(pendingDecisionId = 42L),
        )

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()

            val loaded = awaitUntil { it.status == HomeStatus.Content }
            assertEquals(CheckInCardState.Night, loaded.checkIn)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `dismissing the pre-lit card acknowledges it and returns to resting`() = runTest {
        val harness = Harness(
            timeProvider = FakeTimeProvider(hour = 14),
            senseSuggestionRepository = FakeSenseSuggestionRepository(pendingDecisionId = 42L),
        )

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()
            awaitUntil { it.checkIn == CheckInCardState.SensePreLit(42L) }

            containerHost.handleHomeIntent(HomeIntent.DismissCheckIn)

            awaitUntil { it.checkIn == CheckInCardState.Resting }
            assertEquals(listOf(42L), harness.senseSuggestionRepository.dismissed)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `dismissing the resting card is inert`() = runTest {
        val harness = Harness()

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()
            awaitUntil { it.status == HomeStatus.Content }

            containerHost.handleHomeIntent(HomeIntent.DismissCheckIn)

            assertTrue(harness.senseSuggestionRepository.dismissed.isEmpty())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a next-nudge estimate is present while reminders are enabled`() = runTest {
        val harness = Harness(
            preferencesRepository = FakePreferencesRepository(HomePreferences(remindersEnabled = true)),
        )
        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()
            val loaded = awaitUntil { it.status == HomeStatus.Content }
            assertTrue(loaded.nextNudgeAt != null)
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `no next-nudge estimate while reminders are disabled`() = runTest {
        val harness = Harness(
            preferencesRepository = FakePreferencesRepository(HomePreferences(remindersEnabled = false)),
        )
        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()
            val loaded = awaitUntil { it.status == HomeStatus.Content }
            assertNull(loaded.nextNudgeAt)
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `the deep breathing row starts a breathing break`() = runTest {
        val harness = Harness()

        harness.viewModel.test(this) {
            expectInitialState()
            containerHost.handleHomeIntent(HomeIntent.StartDeepBreathing)

            val event = harness.awaitNavEvent() as NavEvent.Navigate
            val destination = event.screen as SessionDestination
            assertEquals(SessionDestination.MODE_BREAK, destination.mode)
            assertEquals(SessionScriptIds.THE_SIGH, destination.scriptId)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `the build-your-own row opens BuilderDestination`() = runTest {
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
    fun `a finished break pops the celebration banner with the current streak`() = runTest {
        val harness = Harness(
            statsRepository = FakeStatsRepository(SessionStats(streak = 5)),
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
