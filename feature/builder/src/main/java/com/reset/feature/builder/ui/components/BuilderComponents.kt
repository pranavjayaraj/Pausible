package com.reset.feature.builder.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
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
import com.reset.feature.builder.BuilderSuggestion
import com.reset.feature.builder.BuilderConstants
import com.reset.feature.builder.R

// ── Key → label lookups (keys live in BuilderConstants; copy lives in strings.xml) ──

@Composable
fun intentLabel(key: String): String = stringResource(
    when (key) {
        BuilderConstants.INTENT_UNWIND -> R.string.intent_unwind
        BuilderConstants.INTENT_SLEEP -> R.string.intent_sleep
        BuilderConstants.INTENT_ANXIETY -> R.string.intent_anxiety
        BuilderConstants.INTENT_ENERGY -> R.string.intent_energy
        else -> R.string.intent_focus
    },
)

@Composable
fun soundLabel(key: String): String = stringResource(
    when (key) {
        BuilderConstants.SOUND_OCEAN -> R.string.sound_ocean
        BuilderConstants.SOUND_FOREST -> R.string.sound_forest
        BuilderConstants.SOUND_FIRE -> R.string.sound_fire
        BuilderConstants.SOUND_CHIMES -> R.string.sound_chimes
        BuilderConstants.SOUND_SILENCE -> R.string.sound_silence
        else -> R.string.sound_rain
    },
)

@Composable
fun paceLabel(seconds: Int): String = stringResource(
    when (seconds) {
        BuilderConstants.PACE_SLOW_SEC -> R.string.pace_slow
        BuilderConstants.PACE_QUICK_SEC -> R.string.pace_quick
        else -> R.string.pace_medium
    },
)

@Composable
fun remindLabel(key: String): String = stringResource(
    when (key) {
        BuilderConstants.REMIND_MORNING -> R.string.remind_morning
        BuilderConstants.REMIND_MIDDAY -> R.string.remind_midday
        BuilderConstants.REMIND_EVENING -> R.string.remind_evening
        else -> R.string.remind_off
    },
)

@Composable
fun suggestionLabel(suggestion: BuilderSuggestion): String = stringResource(
    when (suggestion.intentKey) {
        BuilderConstants.INTENT_ENERGY -> R.string.builder_suggest_morning
        BuilderConstants.INTENT_UNWIND -> R.string.builder_suggest_evening
        BuilderConstants.INTENT_SLEEP -> R.string.builder_suggest_night
        else -> R.string.builder_suggest_afternoon
    },
)

// ── Section + chips ───────────────────────────────────────────

@Composable
fun BuilderSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AppType.sectionTitle,
        color = AppColors.builderInk,
        modifier = modifier.padding(top = BuilderConstants.sectionSpacing, bottom = BuilderConstants.gridSpacing),
    )
}

/** One selectable option chip: accent when selected, warm grey otherwise. */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(AppShapes.chip)
            .background(if (selected) AppColors.accent else AppColors.chipUnselected)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 9.dp),
    ) {
        Text(
            text = label,
            style = AppType.chip,
            color = if (selected) AppColors.textOnDark else AppColors.builderInk,
        )
    }
}

/** Outlined preset chip under "My sessions". */
@Composable
fun PresetChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(AppShapes.chip)
            .background(AppColors.surfaceWhite)
            .border(2.dp, AppColors.builderInk, AppShapes.chip)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(text = label, style = AppType.soundName, color = AppColors.builderInk)
    }
}

// ── Sound tiles ───────────────────────────────────────────────

/** One soundscape tile: emoji + name, plus the MAIN/LAYER tag and EQ bars when selected. */
@Composable
fun SoundTile(
    option: BuilderConstants.SoundOption,
    selected: Boolean,
    tag: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(AppShapes.panel)
            .background(option.tile)
            .border(
                width = 2.5.dp,
                color = if (selected) AppColors.builderInk else Color.Transparent,
                shape = AppShapes.panel,
            )
            .clickable(onClick = onClick)
            .padding(top = 14.dp, bottom = 12.dp, start = 8.dp, end = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = option.emoji, style = AppType.cardTitle.copy(fontSize = AppType.screenTitle.fontSize))
        Text(text = soundLabel(option.key), style = AppType.soundName, color = option.foreground)
        if (selected && tag != null) {
            Text(
                text = tag,
                style = AppType.soundTag,
                color = option.foreground.copy(alpha = 0.7f),
            )
            EqBars(color = option.foreground)
        }
    }
}

/** The three bouncing equaliser bars on a selected sound tile. */
@Composable
fun EqBars(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "eq")
    val bars = List(3) { index ->
        transition.animateFloat(
            initialValue = 5f,
            targetValue = 15f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 350, delayMillis = index * 180),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "eqBar$index",
        )
    }
    Row(
        modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEach { animated ->
            val height by animated
            Box(
                Modifier
                    .width(3.5.dp)
                    .height(height.dp)
                    .clip(AppShapes.pill)
                    .background(color),
            )
        }
    }
}

// ── Extras panel pieces ───────────────────────────────────────

/** Small bell-option chip inside the extras panel. */
@Composable
fun BellChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(AppShapes.bell)
            .background(if (selected) AppColors.accent else AppColors.chipUnselected)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            style = AppType.chipSmall,
            color = if (selected) AppColors.textOnDark else AppColors.builderInk,
        )
    }
}

/** The design's pill toggle: sliding white knob over accent (on) / warm grey (off). */
@Composable
fun PillToggle(checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(width = BuilderConstants.toggleWidth, height = BuilderConstants.toggleHeight)
            .clip(AppShapes.bell)
            .background(if (checked) AppColors.accent else AppColors.toggleOff)
            .clickable(onClick = onToggle)
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .size(BuilderConstants.toggleKnob)
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .clip(CircleShape)
                .background(AppColors.surfaceWhite),
        )
    }
}

/** The "Name your session (optional)" input over the warm panel background. */
@Composable
fun SessionNameField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = AppType.input.copy(color = AppColors.builderInk),
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.input)
            .background(AppColors.panel)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.builder_name_hint),
                        style = AppType.input,
                        color = AppColors.inkFaint,
                    )
                }
                innerTextField()
            }
        },
    )
}
