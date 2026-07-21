package com.reset.feature.sessions.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reset.core.compose.LifecycleAwareLaunchedEffect
import com.reset.feature.sessions.Act
import com.reset.feature.sessions.SessionStep
import com.reset.feature.sessions.SessionViewModel
import com.reset.feature.sessions.navigation.SessionIntent
import com.reset.feature.sessions.navigation.SessionSideEffect
import com.reset.model.domain.SoundController

/**
 * Composable entry point for the full-screen session experience. Focus↔Warmup and the
 * catalog player's Arrival→Guide→Landing are intra-feature state transitions (rendered
 * from [SessionStep]/[Act]); leaving goes through the ViewModel's injected
 * [com.reset.navigation.Navigator]. The chime is the feature's only one-shot effect
 * routed here, played via the injected [soundController].
 */
@Composable
fun SessionRoute(soundController: SoundController) {
    val viewModel: SessionViewModel = hiltViewModel()
    val state by viewModel.stateFlow().collectAsStateWithLifecycle()

    LifecycleAwareLaunchedEffect(viewModel.sideFlow()) { effect ->
        when (effect) {
            is SessionSideEffect.PlayChime -> soundController.playChime(effect.kind)
            // Attach point for Phase 6: resolve the close-person target and hand the built
            // ACTION_SENDTO/ACTION_DIAL Intent to the app host — never built here directly
            // held or launched from the ViewModel, per the leak-safe activity-launching rule.
            is SessionSideEffect.LaunchActionRequested -> Unit
        }
    }

    BackHandler { viewModel.handleSessionIntent(SessionIntent.HandleBackPress) }

    when (state.step) {
        SessionStep.Focus -> FocusScreen(state = state, onIntent = viewModel::handleSessionIntent)
        SessionStep.Warmup -> WarmupScreen(state = state, onIntent = viewModel::handleSessionIntent)
        SessionStep.Break -> when (state.breakPlayer.act) {
            Act.Arrival -> ArrivalScreen(state = state)
            is Act.Guide -> GuideScreen(state = state)
            Act.Landing -> LandingScreen(state = state)
        }
    }
}
