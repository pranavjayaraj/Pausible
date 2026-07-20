package com.reset.feature.profile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppShapes
import com.reset.core.designsystem.AppType
import com.reset.core.designsystem.GearIcon
import com.reset.core.designsystem.SproutExpression
import com.reset.core.designsystem.SproutMascot
import com.reset.feature.profile.ProfileConstants
import com.reset.feature.profile.R

private val chipShape = RoundedCornerShape(12.dp)
private val chipPaddingH = 12.dp
private val chipPaddingV = 4.dp
private val barShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp)

/** The circular bordered gear button that opens Settings, per the design's header. */
@Composable
fun SettingsGearButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.profile_settings_content_description)
    Box(
        modifier
            .size(ProfileConstants.gearButtonSize)
            .clip(CircleShape)
            .background(AppColors.surfaceWhite)
            .border(ProfileConstants.gearButtonBorder, AppColors.exploreBorder, CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        GearIcon(
            modifier = Modifier.size(ProfileConstants.gearIconSize),
            tint = AppColors.inkSoft,
        )
    }
}

/** The teal "breaks taken" stat card with its break-kind chips. */
@Composable
fun BreaksTakenCard(breaksTaken: Int, modifier: Modifier = Modifier) {
    StatCard(
        background = SolidColor(AppColors.statsBreaksCard),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.profile_breaks_label),
            style = AppType.eyebrow,
            color = AppColors.statsBreaksLabel,
        )
        Text(
            text = breaksTaken.toString(),
            style = AppType.statValue,
            color = AppColors.statsBreaksInk,
            modifier = Modifier.padding(top = ProfileConstants.statValueSpacing),
        )
        Row(
            Modifier.padding(top = ProfileConstants.chipRowSpacing),
            horizontalArrangement = Arrangement.spacedBy(ProfileConstants.chipGap),
        ) {
            BreakKindChip(stringResource(R.string.profile_chip_stretch))
            BreakKindChip(stringResource(R.string.profile_chip_hydrate))
        }
    }
}

/** The gradient streak card with Sprout peeking over its bottom-right corner. */
@Composable
fun StreakCard(streakDays: Int, modifier: Modifier = Modifier) {
    StatCard(
        background = AppColors.streakCardGradient,
        modifier = modifier,
        overlay = {
            SproutMascot(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp)
                    .offset(y = ProfileConstants.streakMascotOverhang)
                    .size(ProfileConstants.streakMascotSize),
                expression = SproutExpression.Excited,
            )
        },
    ) {
        Text(
            text = stringResource(R.string.profile_streak_label),
            style = AppType.eyebrow,
            color = AppColors.streakLabel,
        )
        Row(
            Modifier.padding(top = ProfileConstants.statValueSpacing),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = streakDays.toString(),
                style = AppType.statValue,
                color = AppColors.streakInk,
            )
            Text(
                text = stringResource(R.string.profile_streak_days),
                style = AppType.statUnit,
                color = AppColors.streakInk,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Text(
            text = stringResource(R.string.profile_streak_hint),
            style = AppType.bannerBody,
            color = AppColors.streakLabel,
            modifier = Modifier.padding(top = ProfileConstants.streakHintSpacing),
        )
    }
}

/**
 * The trailing-week focus bar chart. Bars scale against the week's best day; today's bar
 * is the highlighted one. [dayInitials] is Monday-first, matching [minutesPerDay].
 */
@Composable
fun FocusBarChart(
    minutesPerDay: List<Int>,
    todayIndex: Int,
    dayInitials: List<String>,
    modifier: Modifier = Modifier,
) {
    val maxMinutes = (minutesPerDay.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = ProfileConstants.chartPaddingH),
        horizontalArrangement = Arrangement.spacedBy(ProfileConstants.chartBarGap),
        verticalAlignment = Alignment.Bottom,
    ) {
        minutesPerDay.forEachIndexed { index, minutes ->
            val fraction = (minutes.toFloat() / maxMinutes)
                .coerceAtLeast(ProfileConstants.CHART_MIN_BAR_FRACTION)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(ProfileConstants.chartBarAreaHeight * fraction)
                        .clip(barShape)
                        .background(if (index == todayIndex) AppColors.barActive else AppColors.barIdle),
                )
                Text(
                    text = dayInitials.getOrElse(index) { "" },
                    style = AppType.barDay,
                    color = AppColors.inkMuted,
                    modifier = Modifier.padding(top = ProfileConstants.chartLabelSpacing),
                )
            }
        }
    }
}

/** Shared rounded stat-card scaffold; [overlay] draws over the padded content box. */
@Composable
private fun StatCard(
    background: Brush,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(background),
    ) {
        Column(
            Modifier.padding(
                horizontal = ProfileConstants.statCardPaddingH,
                vertical = ProfileConstants.statCardPaddingV,
            ),
        ) {
            content()
        }
        overlay()
    }
}

@Composable
private fun BreakKindChip(label: String) {
    Text(
        text = label,
        style = AppType.chipSmall,
        color = Color.White,
        modifier = Modifier
            .clip(chipShape)
            .background(AppColors.statsBreaksLabel)
            .padding(horizontal = chipPaddingH, vertical = chipPaddingV),
    )
}
