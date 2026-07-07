package com.reset.feature.sessions.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reset.core.compose.LifecycleAwareLaunchedEffect
import com.reset.feature.sessions.SessionStep
import com.reset.feature.sessions.SessionViewModel
import com.reset.feature.sessions.navigation.SessionIntent
import com.reset.feature.sessions.navigation.SessionSideEffect
import com.reset.model.domain.SoundController

/**
 * Composable entry point for the full-screen session experience. Focus↔Breathing is an
 * intra-feature state transition (rendered from [SessionStep]); leaving goes through the
 * ViewModel's injected [com.reset.navigation.Navigator]. The chime is the feature's only
 * one-shot effect, played via the injected [soundController].
 */
@Composable
fun SessionRoute(soundController: SoundController) {
    val viewModel: SessionViewModel = hiltViewModel()
    val state by viewModel.stateFlow().collectAsStateWithLifecycle()

    LifecycleAwareLaunchedEffect(viewModel.sideFlow()) { effect ->
        when (effect) {
            is SessionSideEffect.PlayChime -> soundController.playChime(effect.kind)
        }
    }

    BackHandler { viewModel.handleSessionIntent(SessionIntent.HandleBackPress) }

    when (state.step) {
        SessionStep.Focus -> FocusScreen(state = state, onIntent = viewModel::handleSessionIntent)
        SessionStep.Breathing -> BreathingScreen(state = state, onIntent = viewModel::handleSessionIntent)
    }
}
