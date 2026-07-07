package com.reset.feature.sessions.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppShapes
import com.reset.core.designsystem.AppType
import com.reset.core.designsystem.ClockIcon
import com.reset.feature.sessions.SessionsConstants

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
