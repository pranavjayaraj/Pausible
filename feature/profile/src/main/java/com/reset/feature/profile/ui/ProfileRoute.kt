package com.reset.feature.profile.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reset.feature.profile.ProfileViewModel
import com.reset.feature.profile.navigation.ProfileIntent

/**
 * Composable entry point for the Profile tab — the "Weekly Progress" stats summary, placed
 * into the app's NavHost as the dashboard's last tab. Back pops via the ViewModel's
 * injected [com.reset.navigation.Navigator].
 */
@Composable
fun ProfileRoute() {
    val viewModel: ProfileViewModel = hiltViewModel()
    val state by viewModel.stateFlow().collectAsStateWithLifecycle()

    BackHandler { viewModel.handleProfileIntent(ProfileIntent.HandleBackPress) }

    ProfileScreen(state = state)
}
