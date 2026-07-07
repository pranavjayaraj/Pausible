package com.reset.feature.builder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppDimens
import com.reset.core.designsystem.AppShapes
import com.reset.core.designsystem.AppType
import com.reset.core.designsystem.BackIcon
import com.reset.core.designsystem.SproutExpression
import com.reset.core.designsystem.SproutMascot
import com.reset.feature.builder.BuilderState
import com.reset.feature.builder.BuilderSuggestion
import com.reset.feature.builder.BuilderConstants
import com.reset.feature.builder.R
import com.reset.feature.builder.navigation.BuilderIntent
import com.reset.feature.builder.ui.components.BellChip
import com.reset.feature.builder.ui.components.BuilderSectionTitle
import com.reset.feature.builder.ui.components.ChoiceChip
import com.reset.feature.builder.ui.components.EqBars
import com.reset.feature.builder.ui.components.PillToggle
import com.reset.feature.builder.ui.components.PresetChip
import com.reset.feature.builder.ui.components.SessionNameField
import com.reset.feature.builder.ui.components.SoundTile
import com.reset.feature.builder.ui.components.intentLabel
import com.reset.feature.builder.ui.components.paceLabel
import com.reset.feature.builder.ui.components.remindLabel
import com.reset.feature.builder.ui.components.soundLabel
import com.reset.feature.builder.ui.components.suggestionLabel

private val ctaGradientHeight = 110.dp
private val soundColumns = 3

/**
 * The "Craft your session" builder — stateless, rendered from [BuilderState]; every event
 * flows up through [onIntent]. "Begin" hands the configuration to the Sessions feature
 * through the ViewModel's Navigator.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BuilderScreen(
    builder: BuilderState,
    suggestion: BuilderSuggestion,
    onIntent: (BuilderIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(AppColors.surfaceWhite)) {
        Column(Modifier.fillMaxSize()) {
            BuilderHeader(paceSec = builder.paceSec, onBack = { onIntent(BuilderIntent.HandleBackPress) })

            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = AppDimens.screenPaddingH)
                    .padding(top = 8.dp, bottom = ctaGradientHeight),
            ) {
                SuggestionCard(
                    label = suggestionLabel(suggestion),
                    onApply = { onIntent(BuilderIntent.ApplySuggestion) },
                    modifier = Modifier.padding(top = BuilderConstants.cardSpacing),
                )

                if (builder.presets.isNotEmpty()) {
                    BuilderSectionTitle(stringResource(R.string.builder_my_sessions))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(BuilderConstants.chipSpacing),
                        verticalArrangement = Arrangement.spacedBy(BuilderConstants.chipSpacing),
                    ) {
                        builder.presets.forEach { preset ->
                            PresetChip(
                                label = preset.name,
                                onClick = { onIntent(BuilderIntent.ApplyPreset(preset.name)) },
                            )
                        }
                    }
                }

                BuilderSectionTitle(stringResource(R.string.builder_whats_this_for))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(BuilderConstants.chipSpacing),
                    verticalArrangement = Arrangement.spacedBy(BuilderConstants.chipSpacing),
                ) {
                    BuilderConstants.intentKeys.forEach { key ->
                        ChoiceChip(
                            label = intentLabel(key),
                            selected = key == builder.intentKey,
                            onClick = { onIntent(BuilderIntent.SelectIntentTag(key)) },
                        )
                    }
                }

                BuilderSectionTitle(stringResource(R.string.builder_how_long))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(BuilderConstants.chipSpacing),
                    verticalArrangement = Arrangement.spacedBy(BuilderConstants.chipSpacing),
                ) {
                    BuilderConstants.DURATION_OPTIONS.forEach { minutes ->
                        ChoiceChip(
                            label = stringResource(R.string.builder_duration_chip, minutes),
                            selected = minutes == builder.durationMin,
                            onClick = { onIntent(BuilderIntent.SelectDuration(minutes)) },
                        )
                    }
                }

                BuilderSectionTitle(stringResource(R.string.builder_pick_soundscape))
                SoundGrid(builder = builder, onIntent = onIntent)

                if (builder.layerSoundKey != null) {
                    MixPanel(builder = builder, onIntent = onIntent)
                }

                BuilderSectionTitle(stringResource(R.string.builder_breathing_pace))
                Row(horizontalArrangement = Arrangement.spacedBy(BuilderConstants.gridSpacing)) {
                    BuilderConstants.paceOptions.forEach { seconds ->
                        PaceOption(
                            seconds = seconds,
                            selected = seconds == builder.paceSec,
                            onClick = { onIntent(BuilderIntent.SelectPace(seconds)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                BuilderSectionTitle(stringResource(R.string.builder_daily_reminder))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(BuilderConstants.chipSpacing),
                    verticalArrangement = Arrangement.spacedBy(BuilderConstants.chipSpacing),
                ) {
                    BuilderConstants.remindKeys.forEach { key ->
                        ChoiceChip(
                            label = remindLabel(key),
                            selected = key == builder.remindKey,
                            onClick = { onIntent(BuilderIntent.SelectReminder(key)) },
                        )
                    }
                }

                BuilderSectionTitle(stringResource(R.string.builder_extras))
                ExtrasPanel(builder = builder, onIntent = onIntent)

                SessionNameField(
                    value = builder.sessionName,
                    onValueChange = { onIntent(BuilderIntent.SetSessionName(it)) },
                    modifier = Modifier.padding(top = BuilderConstants.sectionSpacing),
                )

                SaveToMySessionsButton(
                    onClick = { onIntent(BuilderIntent.SavePreset) },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = BuilderConstants.gridSpacing),
                )
            }
        }

        BeginCta(
            builder = builder,
            onClick = { onIntent(BuilderIntent.BeginCustomSession) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** The curved accent header: back, title, and Sprout breathing at the chosen pace. */
@Composable
private fun BuilderHeader(paceSec: Int, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(AppShapes.headerCurve)
            .background(AppColors.accent)
            .statusBarsPadding()
            .padding(horizontal = AppDimens.screenPaddingH)
            .padding(top = 18.dp, bottom = 20.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val backLabel = stringResource(R.string.builder_back)
            Box(
                Modifier
                    .size(AppDimens.touchTargetMin)
                    .clip(CircleShape)
                    .clickable(onClick = onBack)
                    .semantics { contentDescription = backLabel },
                contentAlignment = Alignment.Center,
            ) {
                BackIcon(Modifier.size(22.dp), tint = AppColors.textOnDark)
            }
            Text(
                text = stringResource(R.string.builder_title),
                style = AppType.headerTitle,
                color = AppColors.textOnDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.size(AppDimens.touchTargetMin))
        }
        Row(
            Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier
                    .size(BuilderConstants.builderMascotHalo)
                    .clip(CircleShape)
                    .background(AppColors.textOnDark.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                SproutMascot(
                    modifier = Modifier.size(BuilderConstants.builderMascotSize),
                    expression = SproutExpression.Calm,
                    bodyColor = AppColors.sproutBodyPeach,
                    showCheeks = false,
                    breathePeriodMs = paceSec * 2_000,
                )
            }
            Text(
                text = stringResource(R.string.builder_pace_intro, paceLabel(paceSec).lowercase()),
                style = AppType.heroPrompt,
                color = AppColors.textOnDark,
            )
        }
    }
}

@Composable
private fun SuggestionCard(label: String, onApply: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(AppShapes.panel)
            .background(AppColors.suggestCard)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SproutMascot(
            modifier = Modifier.size(BuilderConstants.suggestMascotSize),
            expression = SproutExpression.Happy,
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.builder_suggests_title),
                style = AppType.suggestTitle,
                color = AppColors.builderInk,
            )
            Text(text = label, style = AppType.suggestSub, color = AppColors.builderSub)
        }
        Box(
            Modifier
                .clip(AppShapes.input)
                .background(AppColors.builderInk)
                .clickable(onClick = onApply)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.builder_try_it),
                style = AppType.buttonTiny,
                color = AppColors.textOnDark,
            )
        }
    }
}

@Composable
private fun SoundGrid(builder: BuilderState, onIntent: (BuilderIntent) -> Unit) {
    BuilderConstants.soundOptions.chunked(soundColumns).forEach { rowOptions ->
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = BuilderConstants.gridSpacing),
            horizontalArrangement = Arrangement.spacedBy(BuilderConstants.gridSpacing),
        ) {
            rowOptions.forEach { option ->
                val isMain = option.key == builder.soundKey
                val isLayer = option.key == builder.layerSoundKey
                SoundTile(
                    option = option,
                    selected = isMain || isLayer,
                    tag = when {
                        isMain -> stringResource(R.string.builder_sound_tag_main)
                        isLayer -> stringResource(R.string.builder_sound_tag_layer)
                        else -> null
                    },
                    onClick = { onIntent(BuilderIntent.ToggleSound(option.key)) },
                    modifier = Modifier.weight(1f),
                )
            }
            repeat(soundColumns - rowOptions.size) { Box(Modifier.weight(1f)) }
        }
    }
}

/** Main/layer balance slider, shown only while a layer sound is active. */
@Composable
private fun MixPanel(builder: BuilderState, onIntent: (BuilderIntent) -> Unit) {
    val layerKey = builder.layerSoundKey ?: return
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
            .clip(AppShapes.input)
            .background(AppColors.panel)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.builder_mix_side, soundLabel(builder.soundKey), builder.mixPercent),
                style = AppType.soundName,
                color = AppColors.builderInk,
            )
            Text(
                text = stringResource(
                    R.string.builder_mix_side,
                    soundLabel(layerKey),
                    BuilderConstants.MIX_MAX_PERCENT - builder.mixPercent,
                ),
                style = AppType.soundName,
                color = AppColors.builderInk,
            )
        }
        Slider(
            value = builder.mixPercent.toFloat(),
            onValueChange = { onIntent(BuilderIntent.ChangeMix(it.toInt())) },
            valueRange = 0f..BuilderConstants.MIX_MAX_PERCENT.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = AppColors.accent,
                activeTrackColor = AppColors.accent,
                inactiveTrackColor = AppColors.toggleOff,
            ),
        )
        Text(
            text = stringResource(R.string.builder_mix_hint),
            style = AppType.panelHint,
            color = AppColors.panelSub,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PaceOption(
    seconds: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(AppShapes.chip)
            .background(if (selected) AppColors.accent else AppColors.chipUnselected)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = paceLabel(seconds),
            style = AppType.paceLabel,
            color = if (selected) AppColors.textOnDark else AppColors.builderInk,
        )
        Text(
            text = stringResource(R.string.builder_pace_breath, seconds),
            style = AppType.chipSub,
            color = if (selected) AppColors.textOnDark.copy(alpha = 0.85f) else AppColors.panelSub,
        )
    }
}

/** Interval bell / guided voice / warm-up / gong rows over the warm panel. */
@Composable
private fun ExtrasPanel(builder: BuilderState, onIntent: (BuilderIntent) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(AppShapes.panel)
            .background(AppColors.panel)
            .padding(horizontal = 16.dp),
    ) {
        ExtrasRow(divider = true) {
            Text(
                text = stringResource(R.string.builder_interval_bell),
                style = AppType.toggleLabel,
                color = AppColors.builderInk,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BuilderConstants.BELL_OPTIONS.forEach { minutes ->
                    BellChip(
                        label = if (minutes == BuilderConstants.BELL_OFF) {
                            stringResource(R.string.builder_bell_off)
                        } else {
                            stringResource(R.string.builder_bell_min, minutes)
                        },
                        selected = minutes == builder.bellMin,
                        onClick = { onIntent(BuilderIntent.SelectBell(minutes)) },
                    )
                }
            }
        }
        ExtrasRow(divider = true) {
            Text(
                text = stringResource(R.string.builder_guided_voice),
                style = AppType.toggleLabel,
                color = AppColors.builderInk,
            )
            PillToggle(checked = builder.guidedVoice, onToggle = { onIntent(BuilderIntent.ToggleGuidedVoice) })
        }
        ExtrasRow(divider = true) {
            Column {
                Text(
                    text = stringResource(R.string.builder_warmup_title),
                    style = AppType.toggleLabel,
                    color = AppColors.builderInk,
                )
                Text(
                    text = stringResource(R.string.builder_warmup_sub),
                    style = AppType.panelHint,
                    color = AppColors.panelSub,
                )
            }
            PillToggle(checked = builder.warmup, onToggle = { onIntent(BuilderIntent.ToggleWarmup) })
        }
        ExtrasRow(divider = false) {
            Text(
                text = stringResource(R.string.builder_end_gong),
                style = AppType.toggleLabel,
                color = AppColors.builderInk,
            )
            PillToggle(checked = builder.gong, onToggle = { onIntent(BuilderIntent.ToggleGong) })
        }
    }
}

@Composable
private fun ExtrasRow(divider: Boolean, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        content()
    }
    if (divider) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(AppColors.panelDivider),
        )
    }
}

@Composable
private fun SaveToMySessionsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(AppShapes.card)
            .background(AppColors.surfaceWhite)
            .border(2.dp, AppColors.builderInk, AppShapes.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 9.dp),
    ) {
        Text(
            text = stringResource(R.string.builder_save_preset),
            style = AppType.saveButton,
            color = AppColors.builderInk,
        )
    }
}

/** The gradient-backed full-width "Begin · …" call to action pinned to the bottom. */
@Composable
private fun BeginCta(
    builder: BuilderState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = builder.sessionName.trim().ifEmpty {
        val mainName = soundLabel(builder.soundKey)
        val layerKey = builder.layerSoundKey
        if (layerKey == null) {
            stringResource(R.string.builder_begin_summary, builder.durationMin, mainName)
        } else {
            stringResource(
                R.string.builder_begin_summary_layered,
                builder.durationMin,
                mainName,
                soundLabel(layerKey),
            )
        }
    }
    Box(
        modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(AppColors.surfaceWhite.copy(alpha = 0f), AppColors.surfaceWhite),
                ),
            )
            .navigationBarsPadding()
            .padding(horizontal = AppDimens.screenPaddingH, vertical = 14.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(AppDimens.ctaHeight)
                .clip(AppShapes.cta)
                .background(AppColors.accent)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.builder_begin, summary),
                style = AppType.cta,
                color = AppColors.textOnDark,
            )
        }
    }
}
