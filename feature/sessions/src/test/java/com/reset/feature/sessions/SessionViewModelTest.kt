package com.reset.feature.sessions

import androidx.lifecycle.SavedStateHandle
import com.reset.feature.sessions.api.SessionDestination
import com.reset.feature.sessions.navigation.SessionIntent
import com.reset.feature.sessions.navigation.SessionSideEffect
import com.reset.model.domain.CelebrationEvent
import com.reset.model.domain.model.ChimeKind
import com.reset.navigation.NavEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.orbitmvi.orbit.test.test

class SessionViewModelTest {

    private class Harness(
        args: Map<String, Any?>,
        val repository: FakeHomeRepository = FakeHomeRepository(),
        val navigator: FakeNavigator = FakeNavigator(),
        val celebrationStore: FakeCelebrationStore = FakeCelebrationStore(),
    ) {
        val viewModel = SessionViewModel(
            SavedStateHandle(args),
            repository,
            navigator,
            celebrationStore,
        )
    }

    /** The typed-destination args as SavedStateHandle sees them (property-name keys). */
    private fun args(
        mode: String,
        breakKind: String? = null,
        durationMin: Int = 1,
        paceSec: Int = SessionDestination.DEFAULT_PACE_SEC,
        soundKey: String? = null,
        warmup: Boolean = false,
        gong: Boolean = true,
    ) = mapOf(
        SessionDestination.ARG_MODE to mode,
        SessionDestination.ARG_BREAK_KIND to breakKind,
        SessionDestination.ARG_DURATION_MIN to durationMin,
        SessionDestination.ARG_PACE_SEC to paceSec,
        SessionDestination.ARG_SOUND_KEY to soundKey,
        SessionDestination.ARG_WARMUP to warmup,
        SessionDestination.ARG_GONG to gong,
    )

    @Test
    fun `a break runs the breathing clock then records, celebrates and pops back`() = runTest {
        val harness = Harness(
            args(mode = SessionDestination.MODE_BREAK, breakKind = SessionDestination.KIND_BREATHING),
        )

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()

            val breathing = awaitUntil { it.step == SessionStep.Breathing }
            assertEquals(SessionConstants.BREAK_BREATH_TICKS, breathing.breathing.totalTicks)

            // The clock spends its tick budget (virtual time), then the break completes.
            assertEquals(
                NavEvent.Pop,
                harness.navigator.events.first(),
            )
            assertEquals(SessionSideEffect.PlayChime(ChimeKind.End), awaitNextSideEffect())
            assertEquals(1, harness.repository.recordedBreaks)
            assertEquals(
                listOf<Any>(CelebrationEvent.BreakFinished),
                harness.celebrationStore.dispatched,
            )

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a warm-up flows into the focus countdown`() = runTest {
        val harness = Harness(
            args(mode = SessionDestination.MODE_FOCUS, durationMin = 1, warmup = true, paceSec = 3),
        )

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()

            val warmup = awaitUntil { it.step == SessionStep.Breathing }
            assertTrue(warmup.breathing.isWarmup)
            assertEquals(SessionConstants.WARMUP_BREATH_TICKS, warmup.breathing.totalTicks)

            // The start chime is posted just before the step flips to Focus.
            assertEquals(SessionSideEffect.PlayChime(ChimeKind.Start), awaitNextSideEffect())
            val focus = awaitUntil { it.step == SessionStep.Focus }
            assertEquals(60, focus.focus.totalSeconds)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a finished focus session is recorded and pops back to its caller`() = runTest {
        val harness = Harness(args(mode = SessionDestination.MODE_FOCUS, durationMin = 1))

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()

            assertEquals(SessionSideEffect.PlayChime(ChimeKind.Start), awaitNextSideEffect())
            awaitUntil { it.step == SessionStep.Focus && it.focus.running }

            // The countdown ticks to zero on virtual time, completing the session.
            assertEquals(
                NavEvent.Pop,
                harness.navigator.events.first(),
            )
            assertEquals(SessionSideEffect.PlayChime(ChimeKind.End), awaitNextSideEffect())
            assertEquals(listOf(1), harness.repository.recordedFocusMinutes)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `toggling pauses and resumes the countdown`() = runTest {
        val harness = Harness(args(mode = SessionDestination.MODE_FOCUS, durationMin = 25))

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()
            awaitUntil { it.step == SessionStep.Focus && it.focus.running }

            containerHost.handleSessionIntent(SessionIntent.ToggleRunning)
            awaitUntil { !it.focus.running }

            containerHost.handleSessionIntent(SessionIntent.ToggleRunning)
            awaitUntil { it.focus.running }

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `ending early with nothing elapsed records no sit but still pops back`() = runTest {
        val harness = Harness(args(mode = SessionDestination.MODE_FOCUS, durationMin = 25))

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()
            awaitUntil { it.step == SessionStep.Focus && it.focus.running }

            containerHost.handleSessionIntent(SessionIntent.EndSession)

            assertEquals(
                NavEvent.Pop,
                harness.navigator.events.first(),
            )
            assertTrue(harness.repository.recordedFocusMinutes.isEmpty())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `back abandons the experience without recording anything`() = runTest {
        val harness = Harness(args(mode = SessionDestination.MODE_FOCUS, durationMin = 25))

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()
            awaitUntil { it.step == SessionStep.Focus }

            containerHost.handleSessionIntent(SessionIntent.HandleBackPress)

            assertEquals(NavEvent.Pop, harness.navigator.events.first())
            assertTrue(harness.repository.recordedFocusMinutes.isEmpty())
            assertEquals(0, harness.repository.recordedBreaks)

            cancelAndIgnoreRemainingItems()
        }
    }
}
