package com.reset.feature.settings.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reset.feature.settings.SettingsViewModel
import com.reset.feature.settings.navigation.SettingsIntent

/**
 * Composable entry point for the full-screen Settings page. The whole screen renders from
 * [com.reset.feature.settings.SettingsState]; leaving goes through the ViewModel's injected
 * [com.reset.navigation.Navigator]. The page has no feature-local one-shot effects today,
 * so nothing collects the side-effect flow.
 */
@Composable
fun SettingsRoute() {
    val viewModel: SettingsViewModel = hiltViewModel()
    val state by viewModel.stateFlow().collectAsStateWithLifecycle()

    BackHandler { viewModel.handleSettingsIntent(SettingsIntent.HandleBackPress) }

    SettingsScreen(
        state = state,
        onIntent = viewModel::handleSettingsIntent,
    )
}
