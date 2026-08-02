package com.reset.feature.checkin.ui

import android.app.Activity
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
import com.reset.model.domain.checkin.EmbedModelManager

/**
 * Composable entry point for the Check In feature: conversational input → optional follow-up
 * → "finding" beat → offer. Intra-feature transitions are just state (rendered from
 * [CheckInStep]); leaving goes through the ViewModel's injected [com.reset.navigation.Navigator].
 *
 * [embedModelManager] is handed down from the host (like [com.reset.model.domain.SoundController]
 * is to `SessionRoute`) purely so [CheckInSideEffect.RequestCellularConfirmation] can resolve
 * a live [Activity] transiently, here — never stored, and never on the ViewModel.
 */
@Composable
fun CheckInRoute(embedModelManager: EmbedModelManager) {
    val viewModel: CheckInViewModel = hiltViewModel()
    val state by viewModel.stateFlow().collectAsStateWithLifecycle()
    val context = LocalContext.current

    val unrecognized = stringResource(R.string.checkin_unrecognized)
    val voiceSoon = stringResource(R.string.checkin_voice_coming_soon)
    LifecycleAwareLaunchedEffect(viewModel.sideFlow()) { effect ->
        when (effect) {
            CheckInSideEffect.UnrecognizedText -> Toast.makeText(context, unrecognized, Toast.LENGTH_SHORT).show()
            CheckInSideEffect.VoiceComingSoon -> Toast.makeText(context, voiceSoon, Toast.LENGTH_SHORT).show()
            CheckInSideEffect.RequestCellularConfirmation -> {
                (context as? Activity)?.let { activity -> embedModelManager.confirmCellularDownload(activity) }
            }
        }
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
