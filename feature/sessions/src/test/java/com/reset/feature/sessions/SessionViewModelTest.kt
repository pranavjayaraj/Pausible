package com.reset.feature.sessions

import androidx.lifecycle.SavedStateHandle
import com.reset.feature.mood.api.MoodDestination
import com.reset.feature.sessions.api.SessionDestination
import com.reset.feature.sessions.api.SessionScriptIds
import com.reset.feature.sessions.content.SessionScripts
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
        val repository: FakeStatsRepository = FakeStatsRepository(),
        val navigator: FakeNavigator = FakeNavigator(),
        val celebrationStore: FakeCelebrationStore = FakeCelebrationStore(),
        val hapticsDelegate: FakeHapticsDelegate = FakeHapticsDelegate(),
        val sessionAudioDelegate: FakeSessionAudioDelegate = FakeSessionAudioDelegate(),
        val checkInPropensityLog: FakeCheckInPropensityLog = FakeCheckInPropensityLog(),
        val timeProvider: FakeTimeProvider = FakeTimeProvider(),
    ) {
        val viewModel = SessionViewModel(
            SavedStateHandle(args),
            repository,
            navigator,
            celebrationStore,
            hapticsDelegate,
            sessionAudioDelegate,
            checkInPropensityLog,
            timeProvider,
        )
    }

    /** The typed-destination args as SavedStateHandle sees them (property-name keys). */
    private fun args(
        mode: String,
        scriptId: String? = null,
        senseTrigger: String? = null,
        durationMin: Int = 1,
        paceSec: Int = SessionDestination.DEFAULT_PACE_SEC,
        soundKey: String? = null,
        warmup: Boolean = false,
        gong: Boolean = true,
        checkInLogRowId: Long? = null,
    ) = mapOf(
        SessionDestination.ARG_MODE to mode,
        SessionDestination.ARG_SCRIPT_ID to scriptId,
        SessionDestination.ARG_SENSE_TRIGGER to senseTrigger,
        SessionDestination.ARG_DURATION_MIN to durationMin,
        SessionDestination.ARG_PACE_SEC to paceSec,
        SessionDestination.ARG_SOUND_KEY to soundKey,
        SessionDestination.ARG_WARMUP to warmup,
        SessionDestination.ARG_GONG to gong,
        SessionDestination.ARG_CHECKIN_LOG_ROW_ID to checkInLogRowId,
    )

    @Test
    fun `a break runs through Arrival, Guide, and Landing then records, celebrates, and pops back`() = runTest {
        val harness = Harness(
            args(mode = SessionDestination.MODE_BREAK, scriptId = SessionScriptIds.THE_SIGH),
        )

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()

            val arrival = awaitUntil { it.step == SessionStep.Break && it.breakPlayer.act == Act.Arrival }
            assertEquals(SessionScripts.THE_SIGH.totalSec, arrival.breakPlayer.totalSec)

            val guide = awaitUntil { it.step == SessionStep.Break && it.breakPlayer.act == Act.Guide(0) }
            assertTrue(guide.breakPlayer.currentLine.isNotBlank())

            val landing = awaitUntil { it.step == SessionStep.Break && it.breakPlayer.act == Act.Landing }
            assertTrue(landing.breakPlayer.landingBridgeLine.isNotBlank())

            // The clock spends its tick budget (virtual time), then the break completes.
            assertEquals(NavEvent.Pop, harness.navigator.events.first())
            assertEquals(1, harness.repository.recordedBreaks)
            assertEquals(
                listOf<Any>(CelebrationEvent.BreakFinished),
                harness.celebrationStore.dispatched,
            )
            assertEquals(1, harness.sessionAudioDelegate.arrivalChimes)
            assertEquals(1, harness.sessionAudioDelegate.landingChimes)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a break launched from Check In logs its real completion, not just the offer`() = runTest {
        val harness = Harness(
            args(mode = SessionDestination.MODE_BREAK, scriptId = SessionScriptIds.THE_SIGH, checkInLogRowId = 42L),
        )

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()
            awaitUntil { it.step == SessionStep.Break && it.breakPlayer.act == Act.Landing }
            harness.navigator.events.first() // Pop — the break finished

            assertEquals(
                listOf(Triple(42L, SessionScriptIds.THE_SIGH, harness.timeProvider.millis)),
                harness.checkInPropensityLog.completions,
            )

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a break launched outside Check In logs no completion`() = runTest {
        val harness = Harness(
            args(mode = SessionDestination.MODE_BREAK, scriptId = SessionScriptIds.THE_SIGH),
        )

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()
            awaitUntil { it.step == SessionStep.Break && it.breakPlayer.act == Act.Landing }
            harness.navigator.events.first()

            assertTrue(harness.checkInPropensityLog.completions.isEmpty())

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

            val warmup = awaitUntil { it.step == SessionStep.Warmup }
            assertEquals(SessionConstants.WARMUP_BREATH_TICKS, warmup.warmup.totalTicks)

            // The start chime is posted just before the step flips to Focus.
            assertEquals(SessionSideEffect.PlayChime(ChimeKind.Start), awaitNextSideEffect())
            val focus = awaitUntil { it.step == SessionStep.Focus }
            assertEquals(60, focus.focus.totalSeconds)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `a finished focus session is recorded, chimed, and hands off to the mood log`() = runTest {
        val harness = Harness(args(mode = SessionDestination.MODE_FOCUS, durationMin = 1))

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()

            assertEquals(SessionSideEffect.PlayChime(ChimeKind.Start), awaitNextSideEffect())
            awaitUntil { it.step == SessionStep.Focus && it.focus.running }

            // The countdown ticks to zero on virtual time, completing the session.
            assertEquals(SessionSideEffect.PlayChime(ChimeKind.End), awaitNextSideEffect())
            assertEquals(listOf(1), harness.repository.recordedFocusMinutes)
            assertEquals(NavEvent.Pop, harness.navigator.events.first())
            assertEquals(NavEvent.Navigate(MoodDestination), harness.navigator.events.first())

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
    fun `ending early with nothing elapsed records no sit but still hands off to the mood log`() = runTest {
        val harness = Harness(args(mode = SessionDestination.MODE_FOCUS, durationMin = 25))

        harness.viewModel.test(this) {
            expectInitialState()
            runOnCreate()
            awaitUntil { it.step == SessionStep.Focus && it.focus.running }

            containerHost.handleSessionIntent(SessionIntent.EndSession)

            assertEquals(NavEvent.Pop, harness.navigator.events.first())
            assertEquals(NavEvent.Navigate(MoodDestination), harness.navigator.events.first())
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
