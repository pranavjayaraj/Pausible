package com.reset.feature.home.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppShapes
import com.reset.core.designsystem.AppType
import com.reset.core.designsystem.BackIcon
import com.reset.core.designsystem.SproutExpression
import com.reset.core.designsystem.SproutMascot
import com.reset.feature.home.CheckInCardState
import com.reset.feature.home.HomeConstants
import com.reset.feature.home.R
import com.reset.feature.home.ui.HomeTone

private val mascotOffsetY = (-30).dp
private val mascotOffsetX = 8.dp
private val dotSize = 7.dp

/**
 * The single check-in entry card — the design's three palette variants (resting / Sense
 * pre-lit / night) sharing one layout. Stateless: renders from [checkIn], events flow up
 * through [onClickCta] / [onClickSkip].
 */
@Composable
fun CheckInCard(
    checkIn: CheckInCardState,
    onClickCta: () -> Unit,
    onClickSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val copy = checkInCopy(checkIn)
    val palette = checkInPalette(checkIn)

    Box(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(AppShapes.card)
                .background(palette.cardBackground)
                .let {
                    if (palette.glowBorder != null) {
                        it.border(1.5.dp, palette.glowBorder, AppShapes.card)
                    } else {
                        it
                    }
                }
                .padding(horizontal = 22.dp, vertical = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                if (checkIn is CheckInCardState.SensePreLit) {
                    Box(Modifier.size(dotSize).clip(CircleShape).background(AppColors.accent))
                }
                Text(text = copy.eyebrow, style = AppType.eyebrowWide, color = palette.eyebrowColor)
            }
            Text(
                text = copy.title,
                style = AppType.sectionTitle,
                color = palette.titleColor,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = copy.subtitle,
                style = AppType.body,
                color = palette.subtitleColor,
                modifier = Modifier.padding(top = 7.dp),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(AppShapes.button)
                    .background(palette.ctaBackground)
                    .clickable(onClick = onClickCta)
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = copy.cta, style = AppType.button, color = palette.ctaInk)
            }
            Text(
                text = copy.skip,
                style = AppType.caption,
                color = palette.skipColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clickable(onClick = onClickSkip),
            )
        }

        SproutMascot(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = -mascotOffsetX, y = mascotOffsetY)
                .size(HomeConstants.checkInMascotSize),
            expression = if (checkIn is CheckInCardState.SensePreLit) SproutExpression.Happy else SproutExpression.Calm,
        )
    }
}

private data class CheckInCopy(
    val eyebrow: String,
    val title: String,
    val subtitle: String,
    val cta: String,
    val skip: String,
)

private data class CheckInPalette(
    val cardBackground: Color,
    val eyebrowColor: Color,
    val titleColor: Color,
    val subtitleColor: Color,
    val ctaBackground: Color,
    val ctaInk: Color,
    val skipColor: Color,
    val glowBorder: Color? = null,
)

@Composable
private fun checkInCopy(checkIn: CheckInCardState): CheckInCopy = when (checkIn) {
    is CheckInCardState.Resting -> CheckInCopy(
        eyebrow = stringResource(R.string.home_checkin_eyebrow),
        title = stringResource(R.string.home_checkin_title),
        subtitle = stringResource(R.string.home_checkin_subtitle),
        cta = stringResource(R.string.home_checkin_cta),
        skip = stringResource(R.string.home_checkin_skip),
    )
    is CheckInCardState.SensePreLit -> CheckInCopy(
        eyebrow = stringResource(R.string.home_checkin_sense_eyebrow),
        title = stringResource(R.string.home_checkin_sense_title),
        subtitle = stringResource(R.string.home_checkin_sense_subtitle),
        cta = stringResource(R.string.home_checkin_cta),
        skip = stringResource(R.string.home_checkin_sense_skip),
    )
    is CheckInCardState.Night -> CheckInCopy(
        eyebrow = stringResource(R.string.home_checkin_eyebrow),
        title = stringResource(R.string.home_checkin_night_title),
        subtitle = stringResource(R.string.home_checkin_night_subtitle),
        cta = stringResource(R.string.home_checkin_cta),
        skip = stringResource(R.string.home_checkin_night_skip),
    )
}

private fun checkInPalette(checkIn: CheckInCardState): CheckInPalette = when (checkIn) {
    is CheckInCardState.Resting -> CheckInPalette(
        cardBackground = AppColors.surfaceWhite,
        eyebrowColor = AppColors.inkMuted,
        titleColor = AppColors.ink,
        subtitleColor = AppColors.inkBody,
        ctaBackground = AppColors.teal,
        ctaInk = AppColors.textOnDark,
        skipColor = AppColors.inkMuted,
    )
    is CheckInCardState.SensePreLit -> CheckInPalette(
        cardBackground = AppColors.surfaceWhite,
        eyebrowColor = AppColors.accentDark,
        titleColor = AppColors.ink,
        subtitleColor = AppColors.inkBody,
        ctaBackground = AppColors.accent,
        ctaInk = AppColors.textOnDark,
        skipColor = AppColors.inkMuted,
        glowBorder = AppColors.accent.copy(alpha = 0.5f),
    )
    is CheckInCardState.Night -> CheckInPalette(
        cardBackground = HomeTone.nightCard,
        eyebrowColor = HomeTone.nightInkMuted,
        titleColor = HomeTone.nightInk,
        subtitleColor = HomeTone.nightInkMuted,
        ctaBackground = HomeTone.nightCta,
        ctaInk = HomeTone.nightInk,
        skipColor = HomeTone.nightInkMuted,
    )
}

/** One small stat tile ("TODAY" / "NEXT NUDGE"): eyebrow label, big value, small sub-line. */
@Composable
fun HomeStatTile(
    eyebrow: String,
    value: String,
    sub: String,
    eyebrowColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(AppShapes.card)
            .background(AppColors.surfaceWarm)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(text = eyebrow, style = AppType.eyebrow, color = eyebrowColor)
        Text(
            text = value,
            style = AppType.sectionTitle,
            color = AppColors.ink,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(text = sub, style = AppType.caption, color = AppColors.inkMuted)
    }
}

/** A tappable quick-access row: icon chip, title + sub, trailing chevron. */
@Composable
fun HomeQuickActionRow(
    title: String,
    sub: String,
    icon: @Composable (Modifier) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(AppColors.surfaceWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HomeConstants.cardSpacing),
    ) {
        Box(
            Modifier
                .size(HomeConstants.quickRowIconSize)
                .clip(AppShapes.input)
                .background(AppColors.surfaceWarm),
            contentAlignment = Alignment.Center,
        ) {
            icon(Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(text = title, style = AppType.headerTitle, color = AppColors.ink)
            Text(text = sub, style = AppType.caption, color = AppColors.inkMuted)
        }
        BackIcon(
            modifier = Modifier.size(16.dp).rotate(180f),
            tint = AppColors.inkMuted,
        )
    }
}

/** The teal "Nice breather!" banner popped over Home after a finished break. */
@Composable
fun CelebrationBannerCard(title: String, message: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(AppShapes.panel)
            .background(AppColors.teal)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HomeConstants.cardSpacing),
    ) {
        SproutMascot(
            modifier = Modifier.size(HomeConstants.bannerMascotSize),
            expression = SproutExpression.Excited,
        )
        Column {
            Text(text = title, style = AppType.bannerTitle, color = AppColors.textOnDark)
            Text(
                text = message,
                style = AppType.bannerBody,
                color = AppColors.textOnDark.copy(alpha = 0.8f),
            )
        }
    }
}
