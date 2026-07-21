package com.reset.feature.sessions

import androidx.lifecycle.SavedStateHandle
import com.reset.core.mvi.BaseViewModel
import com.reset.feature.mood.api.MoodDestination
import com.reset.feature.sessions.api.SessionDestination
import com.reset.feature.sessions.content.GuideStep
import com.reset.feature.sessions.content.SessionScript
import com.reset.feature.sessions.content.SessionScripts
import com.reset.feature.sessions.content.copyLine
import com.reset.feature.sessions.navigation.SessionIntent
import com.reset.feature.sessions.navigation.SessionSideEffect
import com.reset.feature.sessions.utils.HapticsDelegate
import com.reset.feature.sessions.utils.SessionAudioDelegate
import com.reset.feature.sessions.utils.SessionCopySeed
import com.reset.model.domain.CelebrationEvent
import com.reset.model.domain.CelebrationStore
import com.reset.model.domain.checkin.CheckInPropensityLog
import com.reset.model.domain.stats.StatsRepository
import com.reset.model.domain.model.ChimeKind
import com.reset.model.domain.TimeProvider
import com.reset.navigation.Navigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.syntax.simple.SimpleSyntax
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import javax.inject.Inject

/**
 * Two unrelated experiences share this ViewModel because they share one route/screen shell
 * (see [SessionDestination]):
 *  - MODE_FOCUS: the builder's user-timed sit, with an optional warm-up breathing pulse.
 *    Untouched by the catalog rewrite — its duration is a user choice, not a script.
 *  - MODE_BREAK: the generic three-act player, driving any [SessionScript] from the
 *    catalog end to end (Arrival → Guide → Landing) on one honest, script-summed clock.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val statsRepository: StatsRepository,
    private val navigator: Navigator,
    private val celebrationStore: CelebrationStore,
    private val hapticsDelegate: HapticsDelegate,
    private val sessionAudioDelegate: SessionAudioDelegate,
    private val checkInPropensityLog: CheckInPropensityLog,
    private val timeProvider: TimeProvider,
) : BaseViewModel<SessionState, SessionSideEffect>(savedStateHandle) {

    // Typed destination args, surfaced via SavedStateHandle under their property names.
    private val mode: String by argument(SessionDestination.ARG_MODE)
    private val scriptId: String? by argumentNullable(SessionDestination.ARG_SCRIPT_ID)
    private val senseTrigger: String? by argumentNullable(SessionDestination.ARG_SENSE_TRIGGER)
    private val durationMin: Int by argument(SessionDestination.ARG_DURATION_MIN)
    private val paceSec: Int by argument(SessionDestination.ARG_PACE_SEC)
    private val soundKey: String? by argumentNullable(SessionDestination.ARG_SOUND_KEY)
    private val warmup: Boolean by argument(SessionDestination.ARG_WARMUP)
    private val gong: Boolean by argument(SessionDestination.ARG_GONG)
    private val checkInLogRowId: Long? by argumentNullable(SessionDestination.ARG_CHECKIN_LOG_ROW_ID)

    override fun initialState() = SessionState.getDefault()

    override fun initData() {
        start()
    }

    fun handleSessionIntent(intent: SessionIntent) = when (intent) {
        SessionIntent.ToggleRunning -> toggleRunning()
        SessionIntent.EndSession -> endSession()
        SessionIntent.FinishWarmupEarly -> finishWarmup()
        SessionIntent.HandleBackPress -> abandon()
    }

    /** Configures the experience from the destination args and kicks off its clock. */
    private fun start() = intent {
        reduce {
            state.copy(
                mode = mode,
                soundKey = if (mode == SessionDestination.MODE_FOCUS) soundKey else null,
                gong = gong,
            )
        }
        when {
            mode == SessionDestination.MODE_BREAK -> {
                val script = scriptId?.let(SessionScripts::byId)
                if (script != null) runBreakScript(script, senseTrigger) else navigator.pop()
            }
            warmup -> enterWarmup()
            else -> enterFocus()
        }
    }

    // ── Break: the generic three-act player ──────────────────────

    /** Plays [script] end to end on one honest clock — Arrival → every Guide step → Landing
     *  — then records the break and pops back to whichever screen launched it. */
    private suspend fun SimpleSyntax<SessionState, SessionSideEffect>.runBreakScript(
        script: SessionScript,
        senseTrigger: String?,
    ) {
        val completedCount = statsRepository.stats.first().breaksTaken
        val seed = SessionCopySeed.today()
        reduce {
            state.copy(
                step = SessionStep.Break,
                breakPlayer = BreakPlayerState(
                    script = script,
                    act = Act.Arrival,
                    totalSec = script.totalSec,
                    remainingSec = script.totalSec,
                    copySeed = seed,
                    copyCompletedCount = completedCount,
                    currentLine = script.arrivalFor(senseTrigger).pick(seed, completedCount),
                ),
            )
        }
        sessionAudioDelegate.playArrivalChime()
        tickBreakSeconds(SessionScript.ARRIVAL_SEC)

        for ((index, step) in script.guide.withIndex()) {
            if (state.step != SessionStep.Break) return
            runGuideStep(index, step)
        }
        if (state.step != SessionStep.Break) return

        reduce {
            val bp = state.breakPlayer
            state.copy(
                breakPlayer = bp.copy(
                    act = Act.Landing,
                    currentLine = script.landingClose.pick(bp.copySeed, bp.copyCompletedCount),
                    landingBridgeLine = script.landingBridge.pick(bp.copySeed, bp.copyCompletedCount),
                ),
            )
        }
        sessionAudioDelegate.playLandingChime()
        tickBreakSeconds(SessionScript.LANDING_SEC)
        if (state.step != SessionStep.Break) return

        completeBreak(script.id)
    }

    /** Shows [step]'s line, then spends its duration — a breath step spends it cycle by
     *  cycle so haptics/audio stay synced to each cycle's real-time swell. */
    private suspend fun SimpleSyntax<SessionState, SessionSideEffect>.runGuideStep(
        index: Int,
        step: GuideStep,
    ) {
        val bp = state.breakPlayer
        reduce {
            state.copy(
                breakPlayer = state.breakPlayer.copy(
                    act = Act.Guide(index),
                    currentLine = step.copyLine(bp.copySeed, bp.copyCompletedCount),
                ),
            )
        }
        when (step) {
            is GuideStep.Breath -> repeat(step.cycles) {
                if (state.step != SessionStep.Break) return
                hapticsDelegate.pulseBreathCycle(step.pattern)
                sessionAudioDelegate.playBreathTick()
                tickBreakSeconds(step.pattern.cycleMs / SessionConstants.MS_PER_SECOND)
            }
            is GuideStep.LaunchAction -> {
                postSideEffect(SessionSideEffect.LaunchActionRequested(step.actionKind))
                tickBreakSeconds(step.durationSec)
            }
            is GuideStep.Move, is GuideStep.Prompt -> tickBreakSeconds(step.durationSec)
        }
    }

    /** Spends [seconds] of the break player's honest countdown, one reduce per second. */
    private suspend fun SimpleSyntax<SessionState, SessionSideEffect>.tickBreakSeconds(seconds: Int) {
        repeat(seconds.coerceAtLeast(0)) {
            if (state.step != SessionStep.Break) return
            delay(SessionConstants.SESSION_TICK_MS)
            reduce {
                if (state.step == SessionStep.Break) {
                    state.copy(
                        breakPlayer = state.breakPlayer.copy(
                            remainingSec = (state.breakPlayer.remainingSec - 1).coerceAtLeast(0),
                        ),
                    )
                } else {
                    state
                }
            }
        }
    }

    private suspend fun SimpleSyntax<SessionState, SessionSideEffect>.completeBreak(scriptId: String) {
        statsRepository.recordBreak()
        celebrationStore.dispatch(CelebrationEvent.BreakFinished)
        checkInLogRowId?.let { rowId ->
            checkInPropensityLog.recordCompleted(rowId, scriptId, timeProvider.nowMillis())
        }
        navigator.pop()
    }

    // ── Focus (untouched by the catalog rewrite) ─────────────────

    private suspend fun SimpleSyntax<SessionState, SessionSideEffect>.enterWarmup() {
        reduce {
            state.copy(
                step = SessionStep.Warmup,
                warmup = WarmupState(
                    totalTicks = SessionConstants.WARMUP_BREATH_TICKS,
                    phaseDurationMs = paceSec * SessionConstants.MS_PER_SECOND,
                ),
            )
        }
        runWarmupClock()
    }

    /** Alternates inhale/exhale on the phase clock until the tick budget is spent. */
    private suspend fun SimpleSyntax<SessionState, SessionSideEffect>.runWarmupClock() {
        while (state.step == SessionStep.Warmup && state.warmup.ticks < state.warmup.totalTicks) {
            delay(state.warmup.phaseDurationMs.toLong())
            reduce {
                if (state.step == SessionStep.Warmup) {
                    val ticks = state.warmup.ticks + 1
                    state.copy(warmup = state.warmup.copy(ticks = ticks, inhale = ticks % 2 == 0))
                } else {
                    state
                }
            }
        }
        if (state.step == SessionStep.Warmup && state.warmup.ticks >= state.warmup.totalTicks) {
            finishWarmup()
        }
    }

    /** A finished (or cut-short) warm-up flows straight into the focus countdown. */
    private fun finishWarmup() = intent {
        if (state.step != SessionStep.Warmup) return@intent
        enterFocus()
    }

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
        if (elapsedMin > 0) statsRepository.recordFocusSession(elapsedMin)
        navigator.pop()
        navigator.navigate(MoodDestination)
    }

    /** System back abandons the experience without recording anything. */
    private fun abandon() = intent {
        navigator.pop()
    }
}
