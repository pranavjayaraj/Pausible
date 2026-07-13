package com.reset.feature.mood.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reset.core.compose.LifecycleAwareLaunchedEffect
import com.reset.feature.mood.MoodViewModel
import com.reset.feature.mood.navigation.MoodIntent
import com.reset.feature.mood.navigation.MoodSideEffect
import com.reset.model.domain.SoundController

/**
 * Composable entry point for the full-screen Mood Log. The whole screen renders from
 * [com.reset.feature.mood.MoodState]; saving/skipping leaves through the ViewModel's injected
 * [com.reset.navigation.Navigator]. The save chime is the feature's only one-shot effect,
 * played via the injected [soundController] (handed in by the app host, like the Session
 * route).
 */
@Composable
fun MoodRoute(soundController: SoundController) {
    val viewModel: MoodViewModel = hiltViewModel()
    val state by viewModel.stateFlow().collectAsStateWithLifecycle()

    LifecycleAwareLaunchedEffect(viewModel.sideFlow()) { effect ->
        when (effect) {
            is MoodSideEffect.PlayChime -> soundController.playChime(effect.kind)
        }
    }

    BackHandler { viewModel.handleMoodIntent(MoodIntent.Dismiss) }

    MoodScreen(
        state = state,
        onLevelChange = { viewModel.handleMoodIntent(MoodIntent.LevelChanged(it)) },
        onSave = { viewModel.handleMoodIntent(MoodIntent.SaveMood) },
        onSkip = { viewModel.handleMoodIntent(MoodIntent.Dismiss) },
        onClose = { viewModel.handleMoodIntent(MoodIntent.Dismiss) },
    )
}
