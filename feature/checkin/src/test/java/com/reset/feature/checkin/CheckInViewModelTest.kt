package com.reset.feature.checkin

import androidx.lifecycle.SavedStateHandle
import com.reset.feature.checkin.classify.LexiconNeedStateClassifier
import com.reset.feature.checkin.navigation.CheckInIntent
import com.reset.feature.checkin.navigation.CheckInSideEffect
import com.reset.feature.sessions.api.SessionDestination
import com.reset.feature.sessions.api.SessionsDestination
import com.reset.model.domain.checkin.InputMethod
import com.reset.model.domain.checkin.NeedState
import com.reset.model.domain.model.HomePreferences
import com.reset.navigation.NavEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.test

class CheckInViewModelTest {

    private class Harness(
        val navigator: FakeNavigator = FakeNavigator(),
        val sessionSelectorRepository: FakeSessionSelectorRepository = FakeSessionSelectorRepository(),
        val sessionPreviewRepository: FakeSessionPreviewRepository = FakeSessionPreviewRepository(),
        val checkInPropensityLog: FakeCheckInPropensityLog = FakeCheckInPropensityLog(),
        val breakPreferencesRepository: FakeBreakPreferencesRepository = FakeBreakPreferencesRepository(),
        val closePersonRepository: FakeClosePersonRepository = FakeClosePersonRepository(),
        val preferencesRepository: FakePreferencesRepository = FakePreferencesRepository(),
        val senseSuggestionRepository: FakeSenseSuggestionRepository = FakeSenseSuggestionRepository(),
        val timeProvider: FakeTimeProvider = FakeTimeProvider(),
    ) {
        val viewModel = CheckInViewModel(
            SavedStateHandle(),
            navigator,
            sessionSelectorRepository,
            sessionPreviewRepository,
            checkInPropensityLog,
            breakPreferencesRepository,
            closePersonRepository,
            preferencesRepository,
            senseSuggestionRepository,
            timeProvider,
            LexiconNeedStateClassifier(),
        )

        /** Suspends until the next navigation event — the race-free assertion point for
         *  navigation, since a plain post-intent list read can run before the launched
         *  intent coroutine does. */
        suspend fun awaitNavEvent(): NavEvent = navigator.events.first()
    }

    // ------------------------------------------------------------ load: grid/context
    //
    // The chip grid starts (and, absent night/stillness, stays) equal to
    // CheckInState.getDefault() — StateFlow never re-emits an equal value, so these tests
    // only await a state change in scenarios where load() genuinely produces one. The
    // "nothing special" default itself is covered by ChipIdTest's pure gridFor() checks.

    @Test
    fun `quiet hours swap in the night grid`() = runTest {
        val harness = Harness(
            preferencesRepository = FakePreferencesRepository(
                HomePreferences(quietHoursEnabled = true, quietHoursStartHour = 22, quietHoursEndHour = 7),
            ),
            timeProvider = FakeTimeProvider(hour = 23),
        )

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()

            val loaded = awaitUntil { it.isNight }
            assertTrue(ChipId.CANT_SWITCH_OFF in loaded.grid)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a pending long-stillness signal surfaces and glows Been sitting forever`() = runTest {
        val harness = Harness(senseSuggestionRepository = FakeSenseSuggestionRepository(pendingDecisionId = 7L))

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()

            val loaded = awaitUntil { it.suggestedChip == ChipId.BEEN_SITTING_FOREVER }
            assertTrue(ChipId.BEEN_SITTING_FOREVER in loaded.grid)

            cancelAndIgnoreRemainingItems()
        }
    }

    // ------------------------------------------------------------ chip -> NeedState -> offer

    @Test
    fun `a chip with no follow-up resolves straight to an offer`() = runTest {
        val harness = Harness()

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()

            containerHost.handleCheckInIntent(CheckInIntent.SelectChip(ChipId.CANT_FOCUS))

            val offer = awaitUntil { it.step is CheckInStep.Offer }.step as CheckInStep.Offer
            assertEquals(NeedState.SCATTERED, offer.needState)
            assertEquals("primary_scattered", offer.primary.scriptId)
            assertEquals("alternate_scattered", offer.alternate.scriptId)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `resolving a need never passes chip identity to the selector`() = runTest {
        val harness = Harness()

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()

            containerHost.handleCheckInIntent(CheckInIntent.SelectChip(ChipId.EYES_TIRED))
            awaitUntil { it.step is CheckInStep.Offer }

            // The seam rule: the selector's own call log only ever holds (NeedState,
            // SelectionContext) pairs — that's a static type guarantee, not just a runtime
            // check, but confirming the actual call landed proves the ViewModel uses it.
            assertEquals(1, harness.sessionSelectorRepository.calls.size)
            assertEquals(NeedState.EYE_STRAIN, harness.sessionSelectorRepository.calls.single().first)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `resolving a need logs it, echoes it, and stops on the offer`() = runTest {
        val harness = Harness()

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()

            containerHost.handleCheckInIntent(CheckInIntent.SelectChip(ChipId.OVERWHELMED))
            awaitUntil { it.step is CheckInStep.Offer }

            val logged = harness.checkInPropensityLog.logged.single()
            assertEquals(NeedState.OVERWHELMED, logged.needState)
            assertEquals(InputMethod.CHIP, logged.inputMethod)
            assertEquals(listOf(NeedState.OVERWHELMED), harness.breakPreferencesRepository.recordedEchoes)

            cancelAndIgnoreRemainingItems()
        }
    }

    // ------------------------------------------------------------ follow-ups

    @Test
    fun `Stressed opens the wound-up or worn-out follow-up`() = runTest {
        val harness = Harness()

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()

            containerHost.handleCheckInIntent(CheckInIntent.SelectChip(ChipId.STRESSED))

            val followUp = awaitUntil { it.step is CheckInStep.FollowUp }.step as CheckInStep.FollowUp
            assertEquals(NeedState.WOUND_UP, followUp.originNeedState)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `worn-out re-routes WOUND_UP to DRAINED`() = runTest {
        val harness = Harness()

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()
            containerHost.handleCheckInIntent(CheckInIntent.SelectChip(ChipId.STRESSED))
            awaitUntil { it.step is CheckInStep.FollowUp }

            containerHost.handleCheckInIntent(CheckInIntent.AnswerWornOut)

            val offer = awaitUntil { it.step is CheckInStep.Offer }.step as CheckInStep.Offer
            assertEquals(NeedState.DRAINED, offer.needState)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `wound-up stays WOUND_UP`() = runTest {
        val harness = Harness()

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()
            containerHost.handleCheckInIntent(CheckInIntent.SelectChip(ChipId.STRESSED))
            awaitUntil { it.step is CheckInStep.FollowUp }

            containerHost.handleCheckInIntent(CheckInIntent.AnswerWoundUp)

            val offer = awaitUntil { it.step is CheckInStep.Offer }.step as CheckInStep.Offer
            assertEquals(NeedState.WOUND_UP, offer.needState)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `Stiff achy opens the neck-shoulders or wrists-hands follow-up`() = runTest {
        val harness = Harness()

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()

            containerHost.handleCheckInIntent(CheckInIntent.SelectChip(ChipId.STIFF_ACHY))

            val followUp = awaitUntil { it.step is CheckInStep.FollowUp }.step as CheckInStep.FollowUp
            assertEquals(NeedState.BODY_TENSION, followUp.originNeedState)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `wrists and hands routes BODY_TENSION to HAND_STRAIN`() = runTest {
        val harness = Harness()

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()
            containerHost.handleCheckInIntent(CheckInIntent.SelectChip(ChipId.STIFF_ACHY))
            awaitUntil { it.step is CheckInStep.FollowUp }

            containerHost.handleCheckInIntent(CheckInIntent.AnswerWristsHands)

            val offer = awaitUntil { it.step is CheckInStep.Offer }.step as CheckInStep.Offer
            assertEquals(NeedState.HAND_STRAIN, offer.needState)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `neck and shoulders stays BODY_TENSION`() = runTest {
        val harness = Harness()

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()
            containerHost.handleCheckInIntent(CheckInIntent.SelectChip(ChipId.STIFF_ACHY))
            awaitUntil { it.step is CheckInStep.FollowUp }

            containerHost.handleCheckInIntent(CheckInIntent.AnswerNeckShoulders)

            val offer = awaitUntil { it.step is CheckInStep.Offer }.step as CheckInStep.Offer
            assertEquals(NeedState.BODY_TENSION, offer.needState)

            cancelAndIgnoreRemainingItems()
        }
    }

    // ------------------------------------------------------------ offer: alternate / start

    @Test
    fun `Not it swaps to the alternate and logs the swap, once`() = runTest {
        val harness = Harness()

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()
            containerHost.handleCheckInIntent(CheckInIntent.SelectChip(ChipId.CANT_FOCUS))
            awaitUntil { it.step is CheckInStep.Offer }

            containerHost.handleCheckInIntent(CheckInIntent.TryAlternate)
            val swapped = awaitUntil { (it.step as CheckInStep.Offer).showingAlternate }.step as CheckInStep.Offer
            assertEquals("alternate_scattered", swapped.current.scriptId)
            assertEquals(1, harness.checkInPropensityLog.alternateTaken.size)

            // A second tap is inert — never double-logs the swap. Nothing here awaits a
            // state change (there isn't one), but the assertion can only ever read the
            // count as unchanged, whether or not the no-op intent has run yet.
            containerHost.handleCheckInIntent(CheckInIntent.TryAlternate)
            assertEquals(1, harness.checkInPropensityLog.alternateTaken.size)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `starting the primary navigates with its scriptId and the log row id`() = runTest {
        val harness = Harness()

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()
            containerHost.handleCheckInIntent(CheckInIntent.SelectChip(ChipId.CANT_FOCUS))
            awaitUntil { it.step is CheckInStep.Offer }

            containerHost.handleCheckInIntent(CheckInIntent.StartOffered)

            val destination = harness.awaitNavEvent() as NavEvent.Navigate
            val sessionDestination = destination.screen as SessionDestination
            assertEquals(SessionDestination.MODE_BREAK, sessionDestination.mode)
            assertEquals("primary_scattered", sessionDestination.scriptId)
            assertEquals(100L, sessionDestination.checkInLogRowId)
            // Completion is the session runtime's job, not the offer tap's.
            assertTrue(harness.checkInPropensityLog.completed.isEmpty())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `starting after swapping navigates with the alternate's scriptId`() = runTest {
        val harness = Harness()

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()
            containerHost.handleCheckInIntent(CheckInIntent.SelectChip(ChipId.CANT_FOCUS))
            awaitUntil { it.step is CheckInStep.Offer }
            containerHost.handleCheckInIntent(CheckInIntent.TryAlternate)
            awaitUntil { (it.step as CheckInStep.Offer).showingAlternate }

            containerHost.handleCheckInIntent(CheckInIntent.StartOffered)

            val destination = harness.awaitNavEvent() as NavEvent.Navigate
            assertEquals("alternate_scattered", (destination.screen as SessionDestination).scriptId)

            cancelAndIgnoreRemainingItems()
        }
    }

    // ------------------------------------------------------------ browsing / back / support

    @Test
    fun `just browsing switches to the Sessions tab`() = runTest {
        val harness = Harness()

        harness.viewModel.test(this) {
            expectInitialState()
            containerHost.handleCheckInIntent(CheckInIntent.JustBrowsing)

            assertEquals(NavEvent.SwitchTab(SessionsDestination), harness.awaitNavEvent())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `back from the grid pops, but back from anywhere else returns to the grid`() = runTest {
        val harness = Harness()

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()
            containerHost.handleCheckInIntent(CheckInIntent.SelectChip(ChipId.CANT_FOCUS))
            awaitUntil { it.step is CheckInStep.Offer }

            containerHost.handleCheckInIntent(CheckInIntent.HandleBackPress)
            awaitUntil { it.step == CheckInStep.Input }

            containerHost.handleCheckInIntent(CheckInIntent.HandleBackPress)
            assertEquals(NavEvent.Pop, harness.awaitNavEvent())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `Need support now opens the resources screen`() = runTest {
        val harness = Harness()

        harness.viewModel.test(this) {
            expectInitialState()
            containerHost.handleCheckInIntent(CheckInIntent.OpenSupportResources)

            awaitUntil { it.step == CheckInStep.SupportResources }

            cancelAndIgnoreRemainingItems()
        }
    }

    // ------------------------------------------------------------ text input (classifier path)

    @Test
    fun `confident text resolves to an offer and logs InputMethod TEXT`() = runTest {
        val harness = Harness()

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()

            containerHost.handleCheckInIntent(CheckInIntent.ChatTextChanged("my eyes are so tired and blurry"))
            containerHost.handleCheckInIntent(CheckInIntent.SendChat)

            val offer = awaitUntil { it.step is CheckInStep.Offer }.step as CheckInStep.Offer
            assertEquals(NeedState.EYE_STRAIN, offer.needState)
            assertEquals(InputMethod.TEXT, harness.checkInPropensityLog.logged.single().inputMethod)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `confident text for a need with a follow-up routes to the clarifier`() = runTest {
        val harness = Harness()

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()

            containerHost.handleCheckInIntent(CheckInIntent.ChatTextChanged("my neck is killing me"))
            containerHost.handleCheckInIntent(CheckInIntent.SendChat)

            val followUp = awaitUntil { it.step is CheckInStep.FollowUp }.step as CheckInStep.FollowUp
            assertEquals(NeedState.BODY_TENSION, followUp.originNeedState)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `ambiguous text maps onto the matching binary follow-up`() = runTest {
        val harness = Harness()

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()

            // "stressed and wiped out" ties WOUND_UP↔DRAINED → the wound-up follow-up.
            containerHost.handleCheckInIntent(CheckInIntent.ChatTextChanged("stressed and wiped out"))
            containerHost.handleCheckInIntent(CheckInIntent.SendChat)

            val followUp = awaitUntil { it.step is CheckInStep.FollowUp }.step as CheckInStep.FollowUp
            assertEquals(NeedState.WOUND_UP, followUp.originNeedState)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `crisis text short-circuits to support resources`() = runTest {
        val harness = Harness()

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()

            containerHost.handleCheckInIntent(CheckInIntent.ChatTextChanged("I want to disappear"))
            containerHost.handleCheckInIntent(CheckInIntent.SendChat)

            awaitUntil { it.step == CheckInStep.SupportResources }

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `unrecognized text stays on input and nudges toward the chips`() = runTest {
        // Daytime, quiet hours off → load() reduces to the (equal) default and never emits,
        // so the only state item is ChatTextChanged; SendChat's NoMatch emits no state, so
        // the nudge side effect is cleanly next in the unified stream.
        val harness = Harness(
            preferencesRepository = FakePreferencesRepository(HomePreferences(quietHoursEnabled = false)),
            timeProvider = FakeTimeProvider(hour = 15),
        )

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()

            containerHost.handleCheckInIntent(CheckInIntent.ChatTextChanged("my stomach hurts"))
            awaitUntil { it.chatText == "my stomach hurts" }
            containerHost.handleCheckInIntent(CheckInIntent.SendChat)

            assertEquals(CheckInSideEffect.UnrecognizedText, awaitSideEffect())
            assertTrue(harness.checkInPropensityLog.logged.isEmpty())

            cancelAndIgnoreRemainingItems()
        }
    }
}
