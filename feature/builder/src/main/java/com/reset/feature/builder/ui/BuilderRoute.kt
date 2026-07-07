package com.reset.feature.builder.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reset.feature.builder.BuilderViewModel

/**
 * Composable entry point for the Builder feature, placed into the app's root NavHost.
 */
@Composable
fun BuilderRoute() {
    val viewModel: BuilderViewModel = hiltViewModel()
    val state by viewModel.stateFlow().collectAsStateWithLifecycle()

    BuilderScreen(
        builder = state,
        suggestion = viewModel.currentSuggestion(),
        onIntent = viewModel::handleIntent,
    )
}
