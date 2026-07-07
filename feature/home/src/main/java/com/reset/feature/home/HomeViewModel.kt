package com.reset.feature.home

import androidx.lifecycle.SavedStateHandle
import com.reset.core.mvi.BaseViewModel
import com.reset.feature.builder.api.BuilderDestination
import com.reset.feature.home.navigation.HomeIntent
import com.reset.feature.home.navigation.HomeSideEffect
import com.reset.feature.sessions.api.SessionDestination
import com.reset.feature.sessions.api.SessionsDestination
import com.reset.model.domain.CelebrationEvent
import com.reset.model.domain.CelebrationStore
import com.reset.model.domain.HomeRepository
import com.reset.model.domain.StartupState
import com.reset.model.domain.TimeProvider
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
    private val repository: HomeRepository,
    private val navigator: Navigator,
    private val celebrationStore: CelebrationStore,
    private val startupState: StartupState,
    private val timeProvider: TimeProvider,
) : BaseViewModel<HomeState, HomeSideEffect>(savedStateHandle) {

    override fun initialState() = HomeState.getDefault()

    override fun initData() {
        load()
        observeCelebrations()
    }

    fun handleHomeIntent(intent: HomeIntent) = when (intent) {
        HomeIntent.Load -> load()
        HomeIntent.Retry -> load()
        HomeIntent.StartMeditate -> startMeditate()
        HomeIntent.StartDeepBreathing -> startDeepBreathing()
        HomeIntent.OpenBuilder -> openBuilder()
        HomeIntent.ExploreMore -> exploreMore()
    }

    private fun load() = intent {
        reduce {
            state.copy(
                status = HomeStatus.Loading,
                factIndex = timeProvider.dayOfMonth() % HomeConstants.FACT_COUNT,
            )
        }
        
        combine(repository.preferences, repository.stats) { prefs, stats ->
            Pair(prefs, stats)
        }
            .catch { error ->
                reduce { state.copy(status = HomeStatus.Error(error.message)) }
                startupState.markContentReady()
            }.collect { (prefs, stats) ->
                reduce {
                    state.copy(
                        status = HomeStatus.Content,
                        durationMin = prefs.durationMin,
                        stats = stats,
                    )
                }
                startupState.markContentReady()
            }
    }

    private fun startMeditate() = intent {
        navigator.navigate(
            SessionDestination(
                mode = SessionDestination.MODE_FOCUS,
                durationMin = state.durationMin,
            ),
        )
    }

    private fun startDeepBreathing() = intent {
        navigator.navigate(
            SessionDestination(
                mode = SessionDestination.MODE_BREAK,
                breakKind = SessionDestination.KIND_BREATHING,
            ),
        )
    }

    private fun openBuilder() = intent {
        navigator.navigate(BuilderDestination)
    }

    private fun exploreMore() = intent {
        navigator.switchTab(SessionsDestination)
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
        val banner = CelebrationBanner(streakDays = state.stats.streak)
        reduce { state.copy(celebration = banner) }
        delay(HomeConstants.CELEBRATION_VISIBLE_MS)
        reduce { if (state.celebration == banner) state.copy(celebration = null) else state }
    }
}
