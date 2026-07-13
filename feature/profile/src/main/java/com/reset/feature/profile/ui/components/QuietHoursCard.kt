package com.reset.feature.profile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppShapes
import com.reset.core.designsystem.AppType
import com.reset.feature.profile.ProfileConstants
import com.reset.feature.profile.R

/**
 * The Sense quiet-hours setting: two hour steppers over a short explanation.
 * Stateless — hours arrive from state, taps flow up as +/-1 hour deltas.
 */
@Composable
fun QuietHoursCard(
    startHour: Int,
    endHour: Int,
    onAdjustStart: (deltaHours: Int) -> Unit,
    onAdjustEnd: (deltaHours: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(AppColors.surfaceWarm)
            .padding(
                horizontal = ProfileConstants.statCardPaddingH,
                vertical = ProfileConstants.statCardPaddingV,
            ),
    ) {
        Text(
            text = stringResource(R.string.profile_quiet_hours_label),
            style = AppType.eyebrow,
            color = AppColors.inkMuted,
        )
        Text(
            text = stringResource(R.string.profile_quiet_hours_hint),
            style = AppType.caption,
            color = AppColors.inkBody,
            modifier = Modifier.padding(top = ProfileConstants.streakHintSpacing),
        )
        HourStepperRow(
            label = stringResource(R.string.profile_quiet_hours_start),
            hour = startHour,
            onAdjust = onAdjustStart,
            modifier = Modifier.padding(top = ProfileConstants.chipRowSpacing),
        )
        HourStepperRow(
            label = stringResource(R.string.profile_quiet_hours_end),
            hour = endHour,
            onAdjust = onAdjustEnd,
            modifier = Modifier.padding(top = ProfileConstants.streakHintSpacing),
        )
    }
}

@Composable
private fun HourStepperRow(
    label: String,
    hour: Int,
    onAdjust: (deltaHours: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AppType.toggleLabel,
            color = AppColors.ink,
            modifier = Modifier.weight(1f),
        )
        StepperButton(text = stringResource(R.string.profile_quiet_hours_minus)) { onAdjust(-1) }
        Text(
            text = formatHour(hour),
            style = AppType.paceLabel,
            color = AppColors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(ProfileConstants.quietHourValueWidth),
        )
        StepperButton(text = stringResource(R.string.profile_quiet_hours_plus)) { onAdjust(+1) }
    }
}

@Composable
private fun StepperButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(ProfileConstants.quietStepperSize)
            .clip(CircleShape)
            .background(AppColors.surfaceWhite)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = AppType.button, color = AppColors.teal)
    }
}

/** 24h → "10 PM" / "7 AM" / "12 AM"; the card is glanceable, not a time picker. */
private fun formatHour(hour: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val display = when (hour % 12) {
        0 -> 12
        else -> hour % 12
    }
    return "$display $amPm"
}
