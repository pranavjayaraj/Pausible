package com.reset.feature.checkin

import androidx.lifecycle.SavedStateHandle
import com.reset.core.mvi.BaseViewModel
import com.reset.feature.checkin.classify.NeedStateClassifier
import com.reset.feature.checkin.navigation.CheckInIntent
import com.reset.feature.checkin.navigation.CheckInSideEffect
import com.reset.feature.sessions.api.SessionDestination
import com.reset.feature.sessions.api.SessionsDestination
import com.reset.model.domain.ReminderTimeCalculator
import com.reset.model.domain.TimeProvider
import com.reset.model.domain.breakprefs.BreakPreferencesRepository
import com.reset.model.domain.checkin.CheckInPropensityLog
import com.reset.model.domain.checkin.EmbedModelManager
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.syntax.simple.SimpleSyntax
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
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
    private val needStateClassifier: NeedStateClassifier,
    private val embedModelManager: EmbedModelManager,
) : BaseViewModel<CheckInState, CheckInSideEffect>(savedStateHandle) {

    override fun initialState() = CheckInState.getDefault()

    override fun initData() {
        load()
    }

    fun handleCheckInIntent(intent: CheckInIntent) = when (intent) {
        is CheckInIntent.SelectChip -> selectChip(intent.chipId)
        is CheckInIntent.SetInputMode -> setInputMode(intent.mode)
        is CheckInIntent.ChatTextChanged -> chatTextChanged(intent.text)
        CheckInIntent.SendChat -> sendChat()
        CheckInIntent.MicTap -> micTap()
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
        reduce { state.copy(originInputMethod = InputMethod.CHIP) }
        if (chipId.hasFollowUp) {
            reduce { state.copy(step = CheckInStep.FollowUp(chipId.needState)) }
        } else {
            resolveAndOffer(chipId.needState)
        }
    }

    private fun setInputMode(mode: InputMode) = intent {
        reduce { state.copy(inputMode = mode) }
        if (mode == InputMode.Voice) postSideEffect(CheckInSideEffect.VoiceComingSoon)
    }

    private fun chatTextChanged(text: String) = intent {
        reduce { state.copy(chatText = text) }
    }

    private fun micTap() = intent { postSideEffect(CheckInSideEffect.VoiceComingSoon) }

    /**
     * Text → need, the second writer to the NeedState seam. The classifier's closed output
     * maps cleanly onto the existing flow: a confident need routes exactly like a chip; an
     * ambiguity that matches a binary follow-up pair routes to that clarifier; crisis and
     * no-match take their escapes. Raw text never leaves this method — only the derived need.
     */
    private fun sendChat() = intent {
        val text = state.chatText
        if (text.isBlank()) return@intent

        when (val result = needStateClassifier.classify(text)) {
            // Crisis and NoMatch take their escapes without marking the input method (NoMatch
            // deliberately emits no state, so its nudge side effect stays first in the stream).
            NeedStateClassifier.Result.Crisis ->
                reduce { state.copy(step = CheckInStep.SupportResources) }

            NeedStateClassifier.Result.NoMatch -> {
                postSideEffect(CheckInSideEffect.UnrecognizedText)
                maybeStartEmbedModelFetch()
            }

            is NeedStateClassifier.Result.Confident -> {
                reduce { state.copy(originInputMethod = InputMethod.TEXT) }
                routeNeed(result.needState)
            }

            is NeedStateClassifier.Result.Ambiguous -> {
                reduce { state.copy(originInputMethod = InputMethod.TEXT) }
                val followUp = followUpFor(result.top, result.second)
                if (followUp != null) {
                    reduce { state.copy(step = CheckInStep.FollowUp(followUp)) }
                } else {
                    routeNeed(result.top)
                }
            }
        }
    }

    /**
     * Kicks off the Tier-2 model's background download the first time text classification
     * needs it and it isn't ready — idempotent (a second unresolved Send while one is
     * already in flight is a no-op) and never blocks Send: this input already fell back to
     * Tier-1 + the always-visible chips via [CheckInSideEffect.UnrecognizedText]. A declined
     * or failed download is silent — [state.embedModelFetching] just clears — since chips
     * already served this input; there is nothing left to retry for *this* Send.
     */
    private fun maybeStartEmbedModelFetch() = intent {
        if (state.embedModelFetching) return@intent
        if (embedModelManager.isReady()) return@intent
        reduce { state.copy(embedModelFetching = true) }
        embedModelManager.fetch().collect { fetchState ->
            when (fetchState) {
                is EmbedModelManager.FetchState.Downloading -> Unit
                EmbedModelManager.FetchState.WaitingForWifi -> Unit
                EmbedModelManager.FetchState.RequiresConfirmation ->
                    postSideEffect(CheckInSideEffect.RequestCellularConfirmation)
                EmbedModelManager.FetchState.Downloaded ->
                    reduce { state.copy(embedModelFetching = false) }
                is EmbedModelManager.FetchState.Failed ->
                    reduce { state.copy(embedModelFetching = false) }
            }
        }
    }

    private suspend fun SimpleSyntax<CheckInState, CheckInSideEffect>.routeNeed(needState: NeedState) {
        val hasFollowUp = needState == NeedState.WOUND_UP || needState == NeedState.BODY_TENSION
        if (hasFollowUp) {
            reduce { state.copy(step = CheckInStep.FollowUp(needState)) }
        } else {
            resolveAndOffer(needState)
        }
    }

    /** The two known binary clarifiers map onto exactly these ambiguous pairs, so a typed
     *  "stressed and wiped" or "stiff wrists" lands on the same follow-up a chip would. */
    private fun followUpFor(a: NeedState, b: NeedState): NeedState? {
        val pair = setOf(a, b)
        return when {
            pair == setOf(NeedState.WOUND_UP, NeedState.DRAINED) -> NeedState.WOUND_UP
            pair == setOf(NeedState.BODY_TENSION, NeedState.HAND_STRAIN) -> NeedState.BODY_TENSION
            else -> null
        }
    }

    private fun answerWoundUp() = intent { resolveAndOffer(NeedState.WOUND_UP) }

    /** Worn-out re-routes to DRAINED — the clarifier's whole point. */
    private fun answerWornOut() = intent { resolveAndOffer(NeedState.DRAINED) }

    private fun answerNeckShoulders() = intent { resolveAndOffer(NeedState.BODY_TENSION) }

    private fun answerWristsHands() = intent { resolveAndOffer(NeedState.HAND_STRAIN) }

    /** Input → NeedState is the input layer's whole job (chips or text); this is the seam
     *  boundary — nothing past here ever sees a [ChipId] or raw text again, only [needState].
     *  A brief "finding" beat covers the selection so it reads as considered, not instant. */
    private suspend fun SimpleSyntax<CheckInState, CheckInSideEffect>.resolveAndOffer(needState: NeedState) {
        reduce { state.copy(step = CheckInStep.Finding) }
        val startMs = timeProvider.nowMillis()

        val nowMillis = timeProvider.nowMillis()
        val context = selectionContext(nowMillis)
        val selection = sessionSelectorRepository.select(needState, context)
        val rowId = checkInPropensityLog.logSelection(needState, state.originInputMethod, selection, nowMillis)
        breakPreferencesRepository.recordNeedStateEcho(needState)

        val primary = offeredSessionFor(selection.primaryScriptId)
        val alternate = offeredSessionFor(selection.alternateScriptId)

        val elapsed = timeProvider.nowMillis() - startMs
        if (elapsed < CheckInConstants.FINDING_MIN_DWELL_MS) {
            delay(CheckInConstants.FINDING_MIN_DWELL_MS - elapsed)
        }
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

    /** No progress indicators, no stack to unwind — back always returns to the input screen
     *  from anywhere; from the input screen itself it leaves the feature. */
    private fun handleBackPress() = intent {
        if (state.step == CheckInStep.Input) {
            navigator.pop()
        } else {
            reduce { state.copy(step = CheckInStep.Input) }
        }
    }
}
