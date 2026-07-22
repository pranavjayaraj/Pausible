package com.reset.feature.checkin.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reset.core.compose.LifecycleAwareLaunchedEffect
import com.reset.feature.checkin.CheckInStep
import com.reset.feature.checkin.CheckInViewModel
import com.reset.feature.checkin.R
import com.reset.feature.checkin.navigation.CheckInIntent
import com.reset.feature.checkin.navigation.CheckInSideEffect

/**
 * Composable entry point for the Check In feature: conversational input → optional follow-up
 * → "finding" beat → offer. Intra-feature transitions are just state (rendered from
 * [CheckInStep]); leaving goes through the ViewModel's injected [com.reset.navigation.Navigator].
 */
@Composable
fun CheckInRoute() {
    val viewModel: CheckInViewModel = hiltViewModel()
    val state by viewModel.stateFlow().collectAsStateWithLifecycle()
    val context = LocalContext.current

    val unrecognized = stringResource(R.string.checkin_unrecognized)
    val voiceSoon = stringResource(R.string.checkin_voice_coming_soon)
    LifecycleAwareLaunchedEffect(viewModel.sideFlow()) { effect ->
        val message = when (effect) {
            CheckInSideEffect.UnrecognizedText -> unrecognized
            CheckInSideEffect.VoiceComingSoon -> voiceSoon
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    BackHandler { viewModel.handleCheckInIntent(CheckInIntent.HandleBackPress) }

    when (state.step) {
        CheckInStep.Input -> InputScreen(state = state, onIntent = viewModel::handleCheckInIntent)
        is CheckInStep.FollowUp -> FollowUpScreen(state = state, onIntent = viewModel::handleCheckInIntent)
        CheckInStep.Finding -> FindingScreen()
        is CheckInStep.Offer -> OfferScreen(state = state, onIntent = viewModel::handleCheckInIntent)
        CheckInStep.SupportResources -> SupportResourcesScreen(onIntent = viewModel::handleCheckInIntent)
    }
}
