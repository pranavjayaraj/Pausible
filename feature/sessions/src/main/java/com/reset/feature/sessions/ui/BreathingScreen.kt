package com.reset.feature.sessions.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppShapes
import com.reset.core.designsystem.AppType
import com.reset.core.designsystem.BreathingGuide
import com.reset.feature.sessions.R
import com.reset.feature.sessions.SessionState
import com.reset.feature.sessions.api.SessionDestination
import com.reset.feature.sessions.navigation.SessionIntent

/**
 * The guided-breathing pulse over the deep-teal backdrop: kind title up top, the shared
 * [BreathingGuide] in the middle, "Finish early" at the bottom. Stateless — the breath
 * clock is owned by the ViewModel.
 */
@Composable
fun BreathingScreen(
    state: SessionState,
    onIntent: (SessionIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(AppColors.breathingBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp)
            .padding(top = 30.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = breathTitle(state),
            style = AppType.eyebrowWide,
            color = AppColors.textOnDarkFaint,
        )

        BreathingGuide(
            inhale = state.breathing.inhale,
            phaseDurationMs = state.breathing.phaseDurationMs,
            phaseLabel = if (state.breathing.inhale) {
                stringResource(R.string.session_breath_in)
            } else {
                stringResource(R.string.session_breath_out)
            },
            totalCycles = state.breathing.totalCycles,
            completedCycles = state.breathing.completedCycles,
            modifier = Modifier.padding(top = 60.dp),
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = stringResource(R.string.session_finish_early),
            style = AppType.buttonSmall,
            color = AppColors.textOnDark,
            modifier = Modifier
                .clip(AppShapes.button)
                .border(1.5.dp, AppColors.finishEarlyBorder, AppShapes.button)
                .clickable { onIntent(SessionIntent.FinishBreathingEarly) }
                .padding(horizontal = 28.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun breathTitle(state: SessionState): String = stringResource(
    when {
        state.breathing.isWarmup -> R.string.session_break_title_warmup
        state.breakKind == SessionDestination.KIND_STRETCH -> R.string.session_break_title_stretch
        state.breakKind == SessionDestination.KIND_MEDITATE -> R.string.session_break_title_meditate
        else -> R.string.session_break_title_breathing
    },
)
