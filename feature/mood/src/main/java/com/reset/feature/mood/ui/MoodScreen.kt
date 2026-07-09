package com.reset.feature.mood.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reset.core.designsystem.AppShapes
import com.reset.core.designsystem.AppType
import com.reset.core.designsystem.CloseIcon
import com.reset.feature.mood.MoodConstants
import com.reset.feature.mood.MoodState
import com.reset.feature.mood.R
import com.reset.feature.mood.ui.components.HeartIcon
import com.reset.feature.mood.ui.components.MoodFace
import com.reset.feature.mood.ui.components.MoodSlider

private const val COLOR_TWEEN_MS = 500
private const val PULSE_MS = 1500
private const val PULSE_SCALE = 1.02f
private val translucentWhite = Color.White.copy(alpha = 0.2f)
private val subtitleWhite = Color.White.copy(alpha = 0.85f)
private val scaleLabelWhite = Color.White.copy(alpha = 0.8f)
private val skipWhite = Color.White.copy(alpha = 0.7f)

/**
 * Stateless "Mood Log" surface — the mood-reactive gradient backdrop, the morphing face, the
 * slider and the save/skip actions, per the design. Everything is derived from
 * [MoodState.tone]; all events flow up through the passed lambdas.
 */
@Composable
fun MoodScreen(
    state: MoodState,
    onLevelChange: (Int) -> Unit,
    onSave: () -> Unit,
    onSkip: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tone = state.tone
    val topColor by animateColorAsState(tone.backgroundTop, tween(COLOR_TWEEN_MS), label = "bgTop")
    val bottomColor by animateColorAsState(tone.backgroundBottom, tween(COLOR_TWEEN_MS), label = "bgBottom")
    val accent by animateColorAsState(tone.accent, tween(COLOR_TWEEN_MS), label = "accent")
    val sliderDesc = stringResource(R.string.mood_slider_content_description)

    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(topColor, bottomColor))),
    ) {
        CloseButton(
            onClose = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .systemBarsPadding()
                .padding(MoodConstants.closeButtonInset),
        )

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.mood_title),
                style = AppType.displayTitle,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = MoodConstants.titleSpacing),
            )
            Text(
                text = stringResource(R.string.mood_subtitle),
                style = AppType.body,
                color = subtitleWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = MoodConstants.subtitleSpacing),
            )

            MoodFace(
                tone = tone,
                modifier = Modifier
                    .padding(top = MoodConstants.faceTopSpacing)
                    .size(MoodConstants.faceSize),
            )

            Column(
                Modifier
                    .padding(top = MoodConstants.sliderTopSpacing)
                    .widthIn(max = MoodConstants.sliderWidthMax)
                    .fillMaxWidth(),
            ) {
                MoodSlider(
                    level = state.level,
                    onLevelChange = onLevelChange,
                    accent = accent,
                    modifier = Modifier.semantics { contentDescription = sliderDesc },
                )
                ScaleLabels(
                    modifier = Modifier.padding(top = MoodConstants.scaleLabelSpacing),
                )
            }

            Column(
                Modifier
                    .padding(top = MoodConstants.actionsTopSpacing)
                    .widthIn(max = MoodConstants.sliderWidthMax)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SaveButton(accent = accent, onSave = onSave)
                Text(
                    text = stringResource(R.string.mood_skip),
                    style = AppType.button,
                    color = skipWhite,
                    modifier = Modifier
                        .padding(top = MoodConstants.skipSpacing)
                        .clip(AppShapes.pill)
                        .clickable(onClick = onSkip)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CloseButton(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val closeDesc = stringResource(R.string.mood_close_content_description)
    Box(
        modifier
            .size(MoodConstants.closeButtonSize)
            .clip(CircleShape)
            .background(translucentWhite)
            .clickable(onClick = onClose)
            .semantics { contentDescription = closeDesc },
        contentAlignment = Alignment.Center,
    ) {
        CloseIcon(Modifier.size(MoodConstants.closeIconSize), tint = Color.White)
    }
}

@Composable
private fun ScaleLabels(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(R.string.mood_scale_rough), style = AppType.eyebrowWide, color = scaleLabelWhite)
        Text(stringResource(R.string.mood_scale_okay), style = AppType.eyebrowWide, color = scaleLabelWhite)
        Text(stringResource(R.string.mood_scale_great), style = AppType.eyebrowWide, color = scaleLabelWhite)
    }
}

/** White pill with the accent label + heart, gently pulsing like the design's CTA. */
@Composable
private fun SaveButton(accent: Color, onSave: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = PULSE_SCALE,
        animationSpec = infiniteRepeatable(tween(PULSE_MS), RepeatMode.Reverse),
        label = "pulseScale",
    )
    Row(
        Modifier
            .scale(scale)
            .fillMaxWidth()
            .height(MoodConstants.saveButtonHeight)
            .clip(AppShapes.pill)
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSave,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.mood_save),
            style = AppType.cta,
            color = accent,
        )
        HeartIcon(
            tint = accent,
            modifier = Modifier
                .padding(start = MoodConstants.saveIconSpacing)
                .size(MoodConstants.saveIconSize),
        )
    }
}
