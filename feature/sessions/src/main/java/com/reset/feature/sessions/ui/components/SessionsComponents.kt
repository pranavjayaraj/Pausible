package com.reset.feature.sessions.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppDimens
import com.reset.core.designsystem.AppShapes
import com.reset.core.designsystem.AppType
import com.reset.core.designsystem.ClockIcon
import com.reset.feature.sessions.R
import com.reset.feature.sessions.SessionConstants
import com.reset.feature.sessions.SessionsConstants

private const val FULL_SWEEP_DEGREES = 360f
private const val SWEEP_START_DEGREES = -90f

/**
 * Track + sweep ring with the m:ss countdown centred inside — the honest-duration ring
 * shared by the focus countdown and the catalog player's Arrival/Guide/Landing acts.
 */
@Composable
fun SessionRing(totalSeconds: Int, remainingSeconds: Int, modifier: Modifier = Modifier) {
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
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )
            drawArc(
                color = AppColors.ringProgress,
                startAngle = SWEEP_START_DEGREES,
                sweepAngle = FULL_SWEEP_DEGREES * fraction,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )
        }
        Text(text = countdown, style = AppType.timer, color = AppColors.textOnDark)
    }
}

/**
 * One break card: tinted circle icon, title, and the little clock + duration tag. The
 * [background] brush covers both the design's gradient (stretch) and solid cards.
 */
@Composable
fun BreakOptionCard(
    title: String,
    duration: String,
    background: Brush,
    contentColor: Color,
    metaColor: Color,
    icon: @Composable (Modifier) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        Box(
            Modifier
                .size(SessionsConstants.cardIconSize)
                .clip(CircleShape)
                .background(AppColors.surfaceWhite.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center,
        ) {
            icon(Modifier.size(SessionsConstants.cardIconGlyph))
        }
        Column {
            Text(text = title, style = AppType.breakCardTitle, color = contentColor)
            Row(
                Modifier.padding(top = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                ClockIcon(Modifier.size(SessionsConstants.metaIconSize), tint = metaColor)
                Text(text = duration, style = AppType.cardMeta, color = metaColor)
            }
        }
    }
}
