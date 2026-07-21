package com.reset.feature.checkin.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reset.feature.checkin.CheckInStep
import com.reset.feature.checkin.CheckInViewModel
import com.reset.feature.checkin.navigation.CheckInIntent

/**
 * Composable entry point for the Check In feature: chip grid → optional follow-up → offer.
 * Intra-feature transitions are just state (rendered from [CheckInStep]); leaving goes
 * through the ViewModel's injected [com.reset.navigation.Navigator].
 */
@Composable
fun CheckInRoute() {
    val viewModel: CheckInViewModel = hiltViewModel()
    val state by viewModel.stateFlow().collectAsStateWithLifecycle()

    BackHandler { viewModel.handleCheckInIntent(CheckInIntent.HandleBackPress) }

    when (state.step) {
        CheckInStep.ChipGrid -> ChipGridScreen(state = state, onIntent = viewModel::handleCheckInIntent)
        is CheckInStep.FollowUp -> FollowUpScreen(state = state, onIntent = viewModel::handleCheckInIntent)
        is CheckInStep.Offer -> OfferScreen(state = state, onIntent = viewModel::handleCheckInIntent)
        CheckInStep.SupportResources -> SupportResourcesScreen(onIntent = viewModel::handleCheckInIntent)
    }
}
