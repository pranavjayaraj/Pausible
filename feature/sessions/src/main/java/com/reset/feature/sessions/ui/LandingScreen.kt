package com.reset.feature.sessions.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppType
import com.reset.feature.sessions.SessionConstants
import com.reset.feature.sessions.SessionState
import com.reset.feature.sessions.ui.components.SessionRing

/**
 * Act three: closure, then the bridge back to the day — [BreakPlayerState.currentLine] is
 * the physical-closure line, [BreakPlayerState.landingBridgeLine] is the last thing read.
 */
@Composable
fun LandingScreen(state: SessionState, modifier: Modifier = Modifier) {
    val bp = state.breakPlayer
    val script = bp.script ?: return

    Column(
        modifier
            .fillMaxSize()
            .background(SessionConstants.breakBackdrop(script))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp)
            .padding(top = 48.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = bp.currentLine,
            style = AppType.screenTitle,
            color = AppColors.textOnDark,
            textAlign = TextAlign.Center,
        )

        Text(
            text = bp.landingBridgeLine,
            style = AppType.eyebrowWide,
            color = AppColors.textOnDarkFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp),
        )

        Spacer(Modifier.weight(1f))

        SessionRing(totalSeconds = bp.totalSec, remainingSeconds = bp.remainingSec)
    }
}
