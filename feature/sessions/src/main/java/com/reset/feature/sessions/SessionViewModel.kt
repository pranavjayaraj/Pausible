package com.reset.feature.sessions

import androidx.lifecycle.SavedStateHandle
import com.reset.core.mvi.BaseViewModel
import com.reset.feature.mood.api.MoodDestination
import com.reset.feature.sessions.api.SessionDestination
import com.reset.feature.sessions.navigation.SessionIntent
import com.reset.feature.sessions.navigation.SessionSideEffect
import com.reset.model.domain.CelebrationEvent
import com.reset.model.domain.CelebrationStore
import com.reset.model.domain.HomeRepository
import com.reset.model.domain.model.ChimeKind
import com.reset.navigation.Navigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import org.orbitmvi.orbit.syntax.simple.SimpleSyntax
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: HomeRepository,
    private val navigator: Navigator,
    private val celebrationStore: CelebrationStore,
) : BaseViewModel<SessionState, SessionSideEffect>(savedStateHandle) {

    // Typed destination args, surfaced via SavedStateHandle under their property names.
    private val mode: String by argument(SessionDestination.ARG_MODE)
    private val breakKind: String? by argumentNullable(SessionDestination.ARG_BREAK_KIND)
    private val durationMin: Int by argument(SessionDestination.ARG_DURATION_MIN)
    private val paceSec: Int by argument(SessionDestination.ARG_PACE_SEC)
    private val soundKey: String? by argumentNullable(SessionDestination.ARG_SOUND_KEY)
    private val warmup: Boolean by argument(SessionDestination.ARG_WARMUP)
    private val gong: Boolean by argument(SessionDestination.ARG_GONG)

    override fun initialState() = SessionState.getDefault()

    override fun initData() {
        start()
    }

    fun handleSessionIntent(intent: SessionIntent) = when (intent) {
        SessionIntent.ToggleRunning -> toggleRunning()
        SessionIntent.EndSession -> endSession()
        SessionIntent.FinishBreathingEarly -> finishBreathing()
        SessionIntent.HandleBackPress -> abandon()
    }

    /** Configures the experience from the destination args and kicks off its clock. */
    private fun start() = intent {
        reduce {
            state.copy(
                mode = mode,
                breakKind = breakKind,
                soundKey = if (mode == SessionDestination.MODE_FOCUS) soundKey else null,
                gong = gong,
            )
        }
        when {
            mode == SessionDestination.MODE_BREAK -> enterBreathing(isWarmup = false)
            warmup -> enterBreathing(isWarmup = true)
            else -> enterFocus()
        }
    }

    // ── Breathing ─────────────────────────────────────────────

    private suspend fun SimpleSyntax<SessionState, SessionSideEffect>.enterBreathing(
        isWarmup: Boolean,
    ) {
        reduce {
            state.copy(
                step = SessionStep.Breathing,
                breathing = BreathingState(
                    isWarmup = isWarmup,
                    totalTicks = if (isWarmup) {
                        SessionConstants.WARMUP_BREATH_TICKS
                    } else {
                        SessionConstants.BREAK_BREATH_TICKS
                    },
                    phaseDurationMs = if (isWarmup) {
                        paceSec * SessionConstants.MS_PER_SECOND
                    } else {
                        SessionConstants.BREATH_PHASE_MS
                    },
                ),
            )
        }
        runBreathingClock()
    }

    /** Alternates inhale/exhale on the phase clock until the tick budget is spent. */
    private suspend fun SimpleSyntax<SessionState, SessionSideEffect>.runBreathingClock() {
        while (state.step == SessionStep.Breathing && state.breathing.ticks < state.breathing.totalTicks) {
            delay(state.breathing.phaseDurationMs.toLong())
            reduce {
                if (state.step == SessionStep.Breathing) {
                    val ticks = state.breathing.ticks + 1
                    state.copy(breathing = state.breathing.copy(ticks = ticks, inhale = ticks % 2 == 0))
                } else {
                    state
                }
            }
        }
        if (state.step == SessionStep.Breathing && state.breathing.ticks >= state.breathing.totalTicks) {
            finishBreathing()
        }
    }

    /** A finished (or cut-short) breathing step: a warm-up flows into focus; a break is
     *  recorded, celebrated, and pops back to whichever screen launched the session. */
    private fun finishBreathing() = intent {
        if (state.step != SessionStep.Breathing) return@intent
        if (state.breathing.isWarmup) {
            enterFocus()
        } else {
            postSideEffect(SessionSideEffect.PlayChime(ChimeKind.End))
            repository.recordBreak()
            celebrationStore.dispatch(CelebrationEvent.BreakFinished)
            navigator.pop()
        }
    }

    // ── Focus ─────────────────────────────────────────────────

    private suspend fun SimpleSyntax<SessionState, SessionSideEffect>.enterFocus() {
        val totalSeconds = durationMin * SessionConstants.SECONDS_PER_MINUTE
        postSideEffect(SessionSideEffect.PlayChime(ChimeKind.Start))
        reduce {
            state.copy(
                step = SessionStep.Focus,
                focus = FocusState(
                    totalSeconds = totalSeconds,
                    remainingSeconds = totalSeconds,
                    running = true,
                ),
            )
        }
        runFocusCountdown()
    }

    /**
     * Ticks the focus countdown to zero, then completes the session. Guarded by the
     * current step so leaving stops the loop; pausing skips ticks without breaking it.
     */
    private suspend fun SimpleSyntax<SessionState, SessionSideEffect>.runFocusCountdown() {
        while (state.step == SessionStep.Focus && state.focus.remainingSeconds > 0) {
            delay(SessionConstants.SESSION_TICK_MS)
            reduce {
                if (state.step == SessionStep.Focus && state.focus.running) {
                    state.copy(
                        focus = state.focus.copy(
                            remainingSeconds = (state.focus.remainingSeconds - 1).coerceAtLeast(0),
                        ),
                    )
                } else {
                    state
                }
            }
        }
        if (state.step == SessionStep.Focus && state.focus.remainingSeconds == 0) {
            completeFocus()
        }
    }

    private fun toggleRunning() = intent {
        if (state.step != SessionStep.Focus) return@intent
        reduce { state.copy(focus = state.focus.copy(running = !state.focus.running)) }
    }

    /** "End Session" completes early, recording whatever was actually sat. */
    private fun endSession() = intent {
        if (state.step != SessionStep.Focus) return@intent
        completeFocus()
    }

    /**
     * Records the sit, then swaps the finished session for the Mood Log: [Navigator.pop]
     * clears the session off the back stack (back to whatever launched it) and the Mood Log
     * is pushed on top, matching the design's "log your mood after that session" flow. Both
     * events ride the ordered Navigator channel, so they apply in sequence.
     */
    private suspend fun SimpleSyntax<SessionState, SessionSideEffect>.completeFocus() {
        val focus = state.focus
        val elapsedMin =
            (focus.totalSeconds - focus.remainingSeconds) / SessionConstants.SECONDS_PER_MINUTE
        if (state.gong) postSideEffect(SessionSideEffect.PlayChime(ChimeKind.End))
        if (elapsedMin > 0) repository.recordFocusSession(elapsedMin)
        navigator.pop()
        navigator.navigate(MoodDestination)
    }

    /** System back abandons the experience without recording anything. */
    private fun abandon() = intent {
        navigator.pop()
    }
}
