package com.reset.feature.home

import androidx.lifecycle.SavedStateHandle
import com.reset.core.mvi.BaseViewModel
import com.reset.feature.builder.api.BuilderDestination
import com.reset.feature.checkin.api.CheckInDestination
import com.reset.feature.home.navigation.HomeIntent
import com.reset.feature.home.navigation.HomeSideEffect
import com.reset.feature.sessions.api.SessionDestination
import com.reset.feature.sessions.api.SessionScriptIds
import com.reset.model.domain.CelebrationEvent
import com.reset.model.domain.CelebrationStore
import com.reset.model.domain.ReminderTimeCalculator
import com.reset.model.domain.StartupState
import com.reset.model.domain.TimeProvider
import com.reset.model.domain.model.HomePreferences
import com.reset.model.domain.model.SessionStats
import com.reset.model.domain.model.WeeklyFocus
import com.reset.model.domain.preferences.PreferencesRepository
import com.reset.model.domain.sense.SenseSuggestionRepository
import com.reset.model.domain.stats.StatsRepository
import com.reset.navigation.Navigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.syntax.simple.SimpleSyntax
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.reduce
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val preferencesRepository: PreferencesRepository,
    private val statsRepository: StatsRepository,
    private val navigator: Navigator,
    private val celebrationStore: CelebrationStore,
    private val startupState: StartupState,
    private val timeProvider: TimeProvider,
    private val senseSuggestionRepository: SenseSuggestionRepository,
) : BaseViewModel<HomeState, HomeSideEffect>(savedStateHandle) {

    override fun initialState() = HomeState.getDefault()

    override fun initData() {
        load()
        observeCelebrations()
    }

    fun handleHomeIntent(intent: HomeIntent) = when (intent) {
        HomeIntent.Load -> load()
        HomeIntent.Retry -> load()
        HomeIntent.OpenCheckIn -> openCheckIn()
        HomeIntent.DismissCheckIn -> dismissCheckIn()
        HomeIntent.StartDeepBreathing -> startDeepBreathing()
        HomeIntent.OpenBuilder -> openBuilder()
    }

    private fun load() = intent {
        val dayPeriod = HomeConstants.dayPeriodFor(timeProvider.hourOfDay())
        reduce {
            state.copy(
                status = HomeStatus.Loading,
                dayPeriod = dayPeriod,
                dayOfWeekIndex = timeProvider.dayOfWeekIndex(),
            )
        }

        // A single fetch for this load — good enough for "does Home open pre-lit"; it does
        // not push live updates while Home stays open (no in-app Sense event bus exists).
        val pendingStretchId = senseSuggestionRepository.pendingSittingStretchDecisionId()

        combine(
            preferencesRepository.preferences,
            statsRepository.stats,
            statsRepository.weeklyFocus,
            statsRepository.todayBreaksTaken,
        ) { prefs, stats, weeklyFocus, todayBreaks ->
            HomeLoad(prefs, stats, weeklyFocus, todayBreaks)
        }
            .catch { error ->
                reduce { state.copy(status = HomeStatus.Error(error.message)) }
                startupState.markContentReady()
            }.collect { load ->
                reduce {
                    state.copy(
                        status = HomeStatus.Content,
                        checkIn = checkInStateFor(dayPeriod, pendingStretchId),
                        todayPauses = load.todayBreaks,
                        todayQuietMin = load.weeklyFocus.minutesPerDay.getOrElse(load.weeklyFocus.todayIndex) { 0 },
                        nextNudgeAt = nextNudgeAt(load.prefs),
                        streak = load.stats.streak,
                    )
                }
                startupState.markContentReady()
            }
    }

    private fun checkInStateFor(dayPeriod: DayPeriod, pendingStretchId: Long?): CheckInCardState = when {
        dayPeriod == DayPeriod.NIGHT -> CheckInCardState.Night
        pendingStretchId != null -> CheckInCardState.SensePreLit(pendingStretchId)
        else -> CheckInCardState.Resting
    }

    private fun nextNudgeAt(prefs: HomePreferences): String? {
        if (!prefs.remindersEnabled) return null
        val next = ReminderTimeCalculator.nextTriggerMillis(
            nowMillis = timeProvider.nowMillis(),
            everyMin = prefs.remindersEveryMin,
            startHour = prefs.remindersStartHour,
            endHour = prefs.remindersEndHour,
        )
        return HomeConstants.formatClockTime(next)
    }

    private fun openCheckIn() = intent {
        navigator.navigate(CheckInDestination)
    }

    /** The card's secondary text; only the Sense pre-lit variant is tied to a real prompt
     *  to acknowledge back to the decision log — Resting/Night dismissals are inert. */
    private fun dismissCheckIn() = intent {
        val pending = state.checkIn as? CheckInCardState.SensePreLit ?: return@intent
        reduce { state.copy(checkIn = CheckInCardState.Resting) }
        senseSuggestionRepository.dismissSittingStretchPrompt(pending.decisionId)
    }

    private fun startDeepBreathing() = intent {
        navigator.navigate(
            SessionDestination(
                mode = SessionDestination.MODE_BREAK,
                scriptId = SessionScriptIds.THE_SIGH,
            ),
        )
    }

    private fun openBuilder() = intent {
        navigator.navigate(BuilderDestination)
    }

    private fun observeCelebrations() = intent {
        celebrationStore.pending.collect { event ->
            when (event) {
                CelebrationEvent.BreakFinished -> {
                    celebrationStore.consume(CelebrationEvent.BreakFinished)
                    showCelebration()
                }
                null -> Unit
            }
        }
    }

    private suspend fun SimpleSyntax<HomeState, HomeSideEffect>.showCelebration() {
        val banner = CelebrationBanner(streakDays = state.streak)
        reduce { state.copy(celebration = banner) }
        delay(HomeConstants.CELEBRATION_VISIBLE_MS)
        reduce { if (state.celebration == banner) state.copy(celebration = null) else state }
    }

    private data class HomeLoad(
        val prefs: HomePreferences,
        val stats: SessionStats,
        val weeklyFocus: WeeklyFocus,
        val todayBreaks: Int,
    )
}
