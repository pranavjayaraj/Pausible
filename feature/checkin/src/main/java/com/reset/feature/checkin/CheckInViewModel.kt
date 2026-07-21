package com.reset.feature.checkin

import androidx.lifecycle.SavedStateHandle
import com.reset.core.mvi.BaseViewModel
import com.reset.feature.checkin.navigation.CheckInIntent
import com.reset.feature.checkin.navigation.CheckInSideEffect
import com.reset.feature.sessions.api.SessionDestination
import com.reset.feature.sessions.api.SessionsDestination
import com.reset.model.domain.ReminderTimeCalculator
import com.reset.model.domain.TimeProvider
import com.reset.model.domain.breakprefs.BreakPreferencesRepository
import com.reset.model.domain.checkin.CheckInPropensityLog
import com.reset.model.domain.checkin.InputMethod
import com.reset.model.domain.checkin.NeedState
import com.reset.model.domain.checkin.SelectionContext
import com.reset.model.domain.checkin.SessionPreviewRepository
import com.reset.model.domain.checkin.SessionSelectorRepository
import com.reset.model.domain.closeperson.ClosePersonRepository
import com.reset.model.domain.preferences.PreferencesRepository
import com.reset.model.domain.sense.SenseSuggestionRepository
import com.reset.navigation.Navigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.syntax.simple.SimpleSyntax
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.reduce
import javax.inject.Inject

/**
 * Chip tap → [NeedState] (+ optional follow-up) → [SessionSelectorRepository] → offer.
 * Every selection reads only [NeedState] and [SelectionContext] — never chip identity — the
 * NeedState seam this whole feature exists to enforce (see `ChipId.kt`'s doc).
 */
@HiltViewModel
class CheckInViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigator: Navigator,
    private val sessionSelectorRepository: SessionSelectorRepository,
    private val sessionPreviewRepository: SessionPreviewRepository,
    private val checkInPropensityLog: CheckInPropensityLog,
    private val breakPreferencesRepository: BreakPreferencesRepository,
    private val closePersonRepository: ClosePersonRepository,
    private val preferencesRepository: PreferencesRepository,
    private val senseSuggestionRepository: SenseSuggestionRepository,
    private val timeProvider: TimeProvider,
) : BaseViewModel<CheckInState, CheckInSideEffect>(savedStateHandle) {

    override fun initialState() = CheckInState.getDefault()

    override fun initData() {
        load()
    }

    fun handleCheckInIntent(intent: CheckInIntent) = when (intent) {
        is CheckInIntent.SelectChip -> selectChip(intent.chipId)
        CheckInIntent.AnswerWoundUp -> answerWoundUp()
        CheckInIntent.AnswerWornOut -> answerWornOut()
        CheckInIntent.AnswerNeckShoulders -> answerNeckShoulders()
        CheckInIntent.AnswerWristsHands -> answerWristsHands()
        CheckInIntent.StartOffered -> startOffered()
        CheckInIntent.TryAlternate -> tryAlternate()
        CheckInIntent.OpenSupportResources -> openSupportResources()
        CheckInIntent.JustBrowsing -> justBrowsing()
        CheckInIntent.HandleBackPress -> handleBackPress()
    }

    /** Night (real quiet-hours prefs) and the same "long sitting stretch" signal Home's
     *  pre-lit card reads decide which contextual chip shows, and glow the surfaced one. */
    private fun load() = intent {
        val prefs = preferencesRepository.preferences.first()
        val nowMillis = timeProvider.nowMillis()
        val isNight = prefs.quietHoursEnabled &&
            ReminderTimeCalculator.isWithinWindow(nowMillis, prefs.quietHoursStartHour, prefs.quietHoursEndHour)
        val longStillness = senseSuggestionRepository.pendingSittingStretchDecisionId() != null

        reduce {
            state.copy(
                isNight = isNight,
                grid = ChipId.gridFor(isNight = isNight, longStillness = longStillness),
                suggestedChip = if (longStillness) ChipId.BEEN_SITTING_FOREVER else null,
            )
        }
    }

    private fun selectChip(chipId: ChipId) = intent {
        if (chipId.hasFollowUp) {
            reduce { state.copy(step = CheckInStep.FollowUp(chipId.needState)) }
        } else {
            resolveAndOffer(chipId.needState)
        }
    }

    private fun answerWoundUp() = intent { resolveAndOffer(NeedState.WOUND_UP) }

    /** Worn-out re-routes to DRAINED — the clarifier's whole point. */
    private fun answerWornOut() = intent { resolveAndOffer(NeedState.DRAINED) }

    private fun answerNeckShoulders() = intent { resolveAndOffer(NeedState.BODY_TENSION) }

    private fun answerWristsHands() = intent { resolveAndOffer(NeedState.HAND_STRAIN) }

    /** Chips → NeedState is the input layer's whole job; this is the seam boundary — nothing
     *  past this point ever sees a [ChipId] again, only [needState]. */
    private suspend fun SimpleSyntax<CheckInState, CheckInSideEffect>.resolveAndOffer(needState: NeedState) {
        val nowMillis = timeProvider.nowMillis()
        val context = selectionContext(nowMillis)
        val selection = sessionSelectorRepository.select(needState, context)
        val rowId = checkInPropensityLog.logSelection(needState, InputMethod.CHIP, selection, nowMillis)
        breakPreferencesRepository.recordNeedStateEcho(needState)

        val primary = offeredSessionFor(selection.primaryScriptId)
        val alternate = offeredSessionFor(selection.alternateScriptId)
        reduce {
            state.copy(
                step = CheckInStep.Offer(
                    needState = needState,
                    primary = primary,
                    alternate = alternate,
                    logRowId = rowId,
                ),
            )
        }
    }

    private suspend fun selectionContext(nowMillis: Long): SelectionContext {
        val breakPrefs = breakPreferencesRepository.preferences.first()
        val closePerson = closePersonRepository.closePerson.first()
        val prefs = preferencesRepository.preferences.first()
        val isNight = prefs.quietHoursEnabled &&
            ReminderTimeCalculator.isWithinWindow(nowMillis, prefs.quietHoursStartHour, prefs.quietHoursEndHour)
        return SelectionContext(
            audioAvailable = breakPrefs.audioAvailable,
            stairsAvailable = breakPrefs.stairsAvailable,
            moveSpaceAvailable = breakPrefs.moveSpaceAvailable,
            waterAccessAvailable = breakPrefs.waterAccessAvailable,
            hasClosePerson = closePerson != null,
            isNight = isNight,
            recentCompletions = checkInPropensityLog.recentCompletions(
                nowMillis - CheckInConstants.RECENT_COMPLETIONS_LOOKBACK_MS,
            ),
            nowMillis = nowMillis,
        )
    }

    private suspend fun offeredSessionFor(scriptId: String): OfferedSession {
        val preview = sessionPreviewRepository.preview(scriptId)
        checkNotNull(preview) { "Selector returned an unknown catalog id: $scriptId" }
        return OfferedSession(
            scriptId = preview.scriptId,
            displayName = preview.displayName,
            totalSec = preview.totalSec,
            whyThisOne = preview.whyThisOne,
            evidenceTag = preview.evidenceTag,
        )
    }

    /** "Not it?" swaps to the alternate and logs the swap as a revealed-preference label —
     *  it never re-queries the selector, since chips already answered the NeedState once. */
    private fun tryAlternate() = intent {
        val offer = state.step as? CheckInStep.Offer ?: return@intent
        if (offer.showingAlternate) return@intent
        checkInPropensityLog.recordAlternateTaken(offer.logRowId)
        reduce { state.copy(step = offer.copy(showingAlternate = true)) }
    }

    /** Completion itself is logged by the session runtime once the break actually finishes
     *  (see [SessionDestination.checkInLogRowId]) — not here, on the tap, which would blur
     *  ACCEPTED into COMPLETED the same way the Sense decision log deliberately keeps apart. */
    private fun startOffered() = intent {
        val offer = state.step as? CheckInStep.Offer ?: return@intent
        navigator.navigate(
            SessionDestination(
                mode = SessionDestination.MODE_BREAK,
                scriptId = offer.current.scriptId,
                checkInLogRowId = offer.logRowId,
            ),
        )
    }

    private fun openSupportResources() = intent {
        reduce { state.copy(step = CheckInStep.SupportResources) }
    }

    private fun justBrowsing() = intent {
        navigator.switchTab(SessionsDestination)
    }

    /** No progress indicators, no stack to unwind — back always returns to the grid from
     *  anywhere; from the grid itself it leaves the feature. */
    private fun handleBackPress() = intent {
        if (state.step == CheckInStep.ChipGrid) {
            navigator.pop()
        } else {
            reduce { state.copy(step = CheckInStep.ChipGrid) }
        }
    }
}
