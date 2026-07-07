package com.reset.feature.home.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppDimens
import com.reset.core.designsystem.AppShapes
import com.reset.core.designsystem.AppType
import com.reset.core.designsystem.BreathIcon
import com.reset.core.designsystem.PlayIcon
import com.reset.core.designsystem.PlusIcon
import com.reset.core.designsystem.SproutExpression
import com.reset.core.designsystem.SproutMascot
import com.reset.feature.home.HomeConstants
import com.reset.feature.home.R

private val heroInnerRadius = 16.dp
private val heroShadow = 10.dp
private val tileWaveWidth = 34.dp
private val tileWaveHeight = 24.dp
private val dashedCircleSize = 44.dp
private val bannerShadow = 14.dp

/** The right-aligned outlined "EXPLORE MORE" pill above the hero card. */
@Composable
fun ExploreMoreButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(AppShapes.input)
            .background(AppColors.surfaceWhite)
            .border(1.5.dp, AppColors.exploreBorder, AppShapes.input)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = stringResource(R.string.home_explore_more),
            style = AppType.exploreLabel,
            color = AppColors.inkSoft,
        )
    }
}

/** The orange Meditate hero card: title + preset tag, play button, "Find your Zen" strip. */
@Composable
fun MeditateHeroCard(durationMin: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cardLabel = stringResource(R.string.home_meditate_content_description, durationMin)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(heroShadow, AppShapes.card, ambientColor = AppColors.accent, spotColor = AppColors.accent)
            .clip(AppShapes.card)
            .background(AppColors.accent)
            .clickable(onClick = onClick)
            .semantics { contentDescription = cardLabel }
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(
                    text = stringResource(R.string.home_meditate_title),
                    style = AppType.cardTitle,
                    color = AppColors.textOnDark,
                )
                Text(
                    text = stringResource(R.string.home_meditate_tag),
                    style = AppType.eyebrow,
                    color = AppColors.textOnDarkSoft,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Box(
                Modifier
                    .size(HomeConstants.heroIconSize)
                    .clip(CircleShape)
                    .background(AppColors.surfaceWhite),
                contentAlignment = Alignment.Center,
            ) {
                PlayIcon(Modifier.size(16.dp), tint = AppColors.accent)
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clip(AppShapes.input)
                .background(AppColors.textOnDark.copy(alpha = 0.22f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SproutMascot(
                modifier = Modifier.size(HomeConstants.heroMascotSize),
                expression = SproutExpression.Calm,
                bodyColor = AppColors.surfaceWhite,
                showCheeks = false,
            )
            Text(
                text = stringResource(R.string.home_meditate_prompt),
                style = AppType.heroPrompt,
                color = AppColors.textOnDark,
            )
        }
    }
}

/** The sand-yellow Deep Breathing tile with the little breath waves. */
@Composable
fun BreathingTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .height(AppDimens.tileHeight)
            .clip(AppShapes.card)
            .background(AppColors.breathTile)
            .clickable(onClick = onClick)
            .padding(AppDimens.tilePadding),
    ) {
        Text(
            text = stringResource(R.string.home_deep_breathing),
            style = AppType.eyebrow,
            color = AppColors.breathTileInk,
        )
        BreathIcon(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(width = tileWaveWidth, height = tileWaveHeight),
            tint = AppColors.breathTileWave,
        )
    }
}

/** The teal Custom tile: dashed "+" circle over the label. */
@Composable
fun CustomTile(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .height(AppDimens.tileHeight)
            .clip(AppShapes.card)
            .background(AppColors.customTile)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(dashedCircleSize), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    color = AppColors.textOnDark.copy(alpha = 0.7f),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(6.dp.toPx(), 6.dp.toPx()),
                        ),
                    ),
                )
            }
            PlusIcon(Modifier.size(20.dp))
        }
        Text(
            text = stringResource(R.string.home_custom),
            style = AppType.paceLabel,
            color = AppColors.textOnDark,
        )
    }
}

/** The white Mindful Tip card: Sprout beside the tip copy. */
@Composable
fun MindfulTipCard(body: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(AppShapes.tipCard)
            .background(AppColors.surfaceWhite)
            .border(1.5.dp, AppColors.hairlineCard, AppShapes.tipCard)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HomeConstants.gridSpacing),
    ) {
        SproutMascot(
            modifier = Modifier.size(HomeConstants.tipMascotSize),
            expression = SproutExpression.Happy,
        )
        Text(text = body, style = AppType.tipBody, color = AppColors.inkSoft)
    }
}

/** The teal "Nice breather!" banner popped over Home after a finished break. */
@Composable
fun CelebrationBannerCard(title: String, message: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .shadow(bannerShadow, AppShapes.panel, ambientColor = AppColors.teal, spotColor = AppColors.teal)
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
