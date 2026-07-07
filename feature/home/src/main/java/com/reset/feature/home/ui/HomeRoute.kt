package com.reset.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reset.feature.home.HomeStatus
import com.reset.feature.home.HomeViewModel

/**
 * Composable entry point for the Home feature, placed into the app's NavHost.
 * Cross-feature navigation (the session experience, tab changes, app-exit, builder)
 * goes through the ViewModel's injected [com.reset.navigation.Navigator].
 */
@Composable
fun HomeRoute() {
    val viewModel: HomeViewModel = hiltViewModel()
    val state by viewModel.stateFlow().collectAsStateWithLifecycle()

    when (state.status) {
        HomeStatus.Loading -> Unit
        is HomeStatus.Error -> ErrorScreen(onRetry = { viewModel.handleHomeIntent(com.reset.feature.home.navigation.HomeIntent.Retry) })
        HomeStatus.Content -> HomeScreen(state = state, onIntent = viewModel::handleHomeIntent)
    }
}
