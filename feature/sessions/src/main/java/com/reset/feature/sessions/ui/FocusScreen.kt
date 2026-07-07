package com.reset.feature.sessions.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppDimens
import com.reset.core.designsystem.AppShapes
import com.reset.core.designsystem.AppType
import com.reset.core.designsystem.SproutExpression
import com.reset.core.designsystem.SproutMascot
import com.reset.feature.sessions.R
import com.reset.feature.sessions.SessionConstants
import com.reset.feature.sessions.SessionState
import com.reset.feature.sessions.navigation.SessionIntent

private const val FULL_SWEEP_DEGREES = 360f
private const val SWEEP_START_DEGREES = -90f

/**
 * The active focus countdown: Sprout with closed eyes, the progress ring, and the
 * pause/end controls, over the soundscape-themed backdrop. Stateless — the countdown is
 * owned by the ViewModel; user actions flow up through [onIntent].
 */
@Composable
fun FocusScreen(
    state: SessionState,
    onIntent: (SessionIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backdrop = SessionConstants.focusTheme(state.soundKey) ?: AppColors.teal

    Column(
        modifier
            .fillMaxSize()
            .background(backdrop)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp)
            .padding(top = 34.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SproutMascot(
            modifier = Modifier.size(SessionConstants.focusMascotSize),
            expression = SproutExpression.Calm,
        )
        Text(
            text = stringResource(R.string.session_keep_eyes_closed),
            style = AppType.screenTitle,
            color = AppColors.textOnDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = SessionConstants.focusTitleSpacing),
        )

        CountdownRing(
            totalSeconds = state.focus.totalSeconds,
            remainingSeconds = state.focus.remainingSeconds,
            modifier = Modifier.padding(top = SessionConstants.ringSpacing),
        )

        Row(
            Modifier
                .weight(1f)
                .padding(top = SessionConstants.controlSpacing),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(SessionConstants.controlSpacing),
        ) {
            SessionControlButton(
                label = if (state.focus.running) {
                    stringResource(R.string.session_pause)
                } else {
                    stringResource(R.string.session_resume)
                },
                background = AppColors.surfaceWhite,
                contentColor = AppColors.ink,
                onClick = { onIntent(SessionIntent.ToggleRunning) },
            )
            SessionControlButton(
                label = stringResource(R.string.session_end),
                background = AppColors.tealDeep,
                contentColor = AppColors.textOnDark,
                onClick = { onIntent(SessionIntent.EndSession) },
            )
        }
    }
}

/** Track + sweep ring with the m:ss countdown centred inside. */
@Composable
private fun CountdownRing(totalSeconds: Int, remainingSeconds: Int, modifier: Modifier = Modifier) {
    val fraction = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f
    val countdown = SessionConstants.formatMmSs(remainingSeconds)
    val countdownLabel = stringResource(R.string.session_countdown_content_description, countdown)

    Box(
        modifier
            .size(AppDimens.ringSize)
            .semantics { contentDescription = countdownLabel },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = AppDimens.ringStroke.toPx(), cap = StrokeCap.Round)
            val inset = AppDimens.ringStroke.toPx() / 2
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(
                color = AppColors.ringTrack,
                startAngle = SWEEP_START_DEGREES,
                sweepAngle = FULL_SWEEP_DEGREES,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )
            drawArc(
                color = AppColors.ringProgress,
                startAngle = SWEEP_START_DEGREES,
                sweepAngle = FULL_SWEEP_DEGREES * fraction,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )
        }
        Text(text = countdown, style = AppType.timer, color = AppColors.textOnDark)
    }
}

@Composable
private fun SessionControlButton(
    label: String,
    background: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .widthIn(min = SessionConstants.controlMinWidth)
            .height(AppDimens.buttonHeight)
            .clip(AppShapes.button)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = AppType.button, color = contentColor)
    }
}
