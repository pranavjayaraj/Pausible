package com.reset.feature.sessions.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppType
import com.reset.feature.sessions.SessionConstants
import com.reset.feature.sessions.SessionState
import com.reset.feature.sessions.ui.components.SessionRing

/**
 * Act one: why this break, now — Sense's context, spoken. The script's display name sits
 * over the honest-duration ring; [BreakPlayerState.currentLine] is the context-aware
 * Arrival line, already resolved by the ViewModel.
 */
@Composable
fun ArrivalScreen(state: SessionState, modifier: Modifier = Modifier) {
    val bp = state.breakPlayer
    val script = bp.script ?: return

    Column(
        modifier
            .fillMaxSize()
            .background(SessionConstants.breakBackdrop(script))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp)
            .padding(top = 40.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = script.displayName,
            style = AppType.eyebrowWide,
            color = AppColors.textOnDarkFaint,
        )

        Text(
            text = bp.currentLine,
            style = AppType.screenTitle,
            color = AppColors.textOnDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp),
        )

        SessionRing(
            totalSeconds = bp.totalSec,
            remainingSeconds = bp.remainingSec,
            modifier = Modifier.padding(top = 44.dp),
        )
    }
}
