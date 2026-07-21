package com.reset.feature.sessions.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppShapes
import com.reset.core.designsystem.AppType
import com.reset.feature.sessions.Act
import com.reset.feature.sessions.R
import com.reset.feature.sessions.SessionConstants
import com.reset.feature.sessions.SessionState
import com.reset.feature.sessions.content.ActionKind
import com.reset.feature.sessions.content.GuideStep
import com.reset.feature.sessions.content.isDimmed
import com.reset.feature.sessions.ui.components.SessionRing

/**
 * Act two: one exercise, fully led — the current [GuideStep] rendered natively per kind.
 * Breath steps run eyes-closed (audio/haptic-led, no pacing UI, per the product pivot);
 * Move/Prompt show their instruction with an optional dim; LaunchAction frames the hand-off
 * the ViewModel already fired as a side effect on entering the step.
 */
@Composable
fun GuideScreen(state: SessionState, modifier: Modifier = Modifier) {
    val bp = state.breakPlayer
    val script = bp.script ?: return
    val act = bp.act as? Act.Guide ?: return
    val step = script.guide.getOrNull(act.stepIndex) ?: return

    val dimAlpha by animateFloatAsState(
        targetValue = if (step.isDimmed) DIMMED_SCRIM_ALPHA else 0f,
        animationSpec = tween(durationMillis = BLOOM_MS),
        label = "guideDim",
    )

    Column(
        modifier
            .fillMaxSize()
            .background(SessionConstants.breakBackdrop(script))
            .background(Color.Black.copy(alpha = dimAlpha))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp)
            .padding(top = 48.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (step.isDimmed) {
            Text(
                text = stringResource(R.string.session_close_your_eyes),
                style = AppType.eyebrowWide,
                color = AppColors.textOnDarkFaint,
            )
        }

        Text(
            text = bp.currentLine,
            style = AppType.screenTitle,
            color = AppColors.textOnDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 28.dp),
        )

        if (step is GuideStep.LaunchAction) {
            LaunchActionRow(step.actionKind, Modifier.padding(top = 28.dp))
        }

        if (step is GuideStep.Move && step.poseAsset != null) {
            // Placeholder for the pose asset (Rive/Lottie key) until art ships.
            PoseAssetPlaceholder(Modifier.padding(top = 28.dp))
        }

        Spacer(Modifier.weight(1f))

        SessionRing(totalSeconds = bp.totalSec, remainingSeconds = bp.remainingSec)
    }
}

@Composable
private fun LaunchActionRow(actionKind: ActionKind, modifier: Modifier = Modifier) {
    val label = stringResource(
        if (actionKind == ActionKind.CALL) {
            R.string.session_launch_action_call
        } else {
            R.string.session_launch_action_message
        },
    )
    Row(
        modifier
            .clip(AppShapes.button)
            .background(AppColors.surfaceWhite.copy(alpha = 0.15f))
            .padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        Text(text = label, style = AppType.button, color = AppColors.textOnDark)
    }
}

@Composable
private fun PoseAssetPlaceholder(modifier: Modifier = Modifier) {
    Row(
        modifier
            .size(88.dp)
            .clip(CircleShape)
            .background(AppColors.surfaceWhite.copy(alpha = 0.12f)),
    ) {
        // Intentionally blank — a Rive/Lottie asset renders here once art ships.
    }
}

private const val DIMMED_SCRIM_ALPHA = 0.88f
private const val BLOOM_MS = 400
