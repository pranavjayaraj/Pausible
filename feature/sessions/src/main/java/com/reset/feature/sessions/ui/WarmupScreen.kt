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
import com.reset.feature.sessions.navigation.SessionIntent

/**
 * The warm-up breathing pulse ahead of a custom focus sit (MODE_FOCUS only — the catalog's
 * break sessions run eyes-closed through the three-act player, not this pacing UI).
 * Stateless — the breath clock is owned by the ViewModel.
 */
@Composable
fun WarmupScreen(
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
            text = stringResource(R.string.session_break_title_warmup),
            style = AppType.eyebrowWide,
            color = AppColors.textOnDarkFaint,
        )

        BreathingGuide(
            inhale = state.warmup.inhale,
            phaseDurationMs = state.warmup.phaseDurationMs,
            phaseLabel = if (state.warmup.inhale) {
                stringResource(R.string.session_breath_in)
            } else {
                stringResource(R.string.session_breath_out)
            },
            totalCycles = state.warmup.totalCycles,
            completedCycles = state.warmup.completedCycles,
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
                .clickable { onIntent(SessionIntent.FinishWarmupEarly) }
                .padding(horizontal = 28.dp, vertical = 12.dp),
        )
    }
}
