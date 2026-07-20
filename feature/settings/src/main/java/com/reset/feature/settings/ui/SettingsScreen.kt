package com.reset.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppDimens
import com.reset.core.designsystem.AppType
import com.reset.core.designsystem.BackIcon
import com.reset.feature.settings.HourPickerTarget
import com.reset.feature.settings.R
import com.reset.feature.settings.SettingsConstants
import com.reset.feature.settings.SettingsState
import com.reset.feature.settings.navigation.SettingsIntent
import com.reset.feature.settings.ui.components.BellIcon
import com.reset.feature.settings.ui.components.HourPickerDialog
import com.reset.feature.settings.ui.components.MoonIcon
import com.reset.feature.settings.ui.components.QuietClockIcon
import com.reset.feature.settings.ui.components.SettingsCard
import com.reset.feature.settings.ui.components.SettingsRow
import com.reset.feature.settings.ui.components.SettingsRowDivider
import com.reset.feature.settings.ui.components.SettingsToggle
import com.reset.feature.settings.ui.components.SoundWavesIcon
import com.reset.feature.settings.ui.components.TimeChip
import com.reset.feature.settings.utils.SettingsUtils

/**
 * Stateless Settings surface — the BloomNow design's Settings screen. Renders purely from
 * [SettingsState]; every event flows up through [onIntent].
 */
@Composable
fun SettingsScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(AppColors.surfaceBreak)
            .statusBarsPadding(),
    ) {
        SettingsHeader(onBack = { onIntent(SettingsIntent.HandleBackPress) })

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = AppDimens.screenPaddingH,
                    end = AppDimens.screenPaddingH,
                    top = SettingsConstants.contentPaddingTop,
                    bottom = SettingsConstants.contentPaddingBottom,
                ),
        ) {
            SectionLabel(
                text = stringResource(R.string.settings_section_quiet_hours),
                topSpacing = SettingsConstants.sectionFirstSpacing,
            )
            QuietHoursCard(state = state, onIntent = onIntent)
            QuietHoursSummary(state = state)

            SectionLabel(
                text = stringResource(R.string.settings_section_notifications),
                topSpacing = SettingsConstants.sectionSpacing,
            )
            NotificationsCard(state = state, onIntent = onIntent)
        }
    }

    state.hourPicker?.let { target ->
        HourPickerDialog(
            title = stringResource(
                when (target) {
                    HourPickerTarget.QuietStart -> R.string.settings_quiet_starts
                    HourPickerTarget.QuietEnd -> R.string.settings_quiet_ends
                },
            ),
            selectedHour = when (target) {
                HourPickerTarget.QuietStart -> state.quietHoursStart
                HourPickerTarget.QuietEnd -> state.quietHoursEnd
            },
            onSelect = { onIntent(SettingsIntent.SelectHour(it)) },
            onDismiss = { onIntent(SettingsIntent.DismissHourPicker) },
        )
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Row(
        Modifier.padding(
            start = AppDimens.screenPaddingH,
            end = AppDimens.screenPaddingH,
            top = SettingsConstants.headerPaddingTop,
            bottom = SettingsConstants.headerPaddingBottom,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(SettingsConstants.backButtonSize)
                .clip(CircleShape)
                .background(AppColors.surfaceWhite)
                .border(SettingsConstants.timeChipBorder, SettingsTone.chipBorder, CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            BackIcon(
                modifier = Modifier.size(SettingsConstants.backIconSize),
                tint = AppColors.inkSoft,
            )
        }
        Text(
            text = stringResource(R.string.settings_title),
            style = AppType.cardTitle,
            color = AppColors.ink,
            modifier = Modifier.padding(start = SettingsConstants.headerGap),
        )
    }
}

@Composable
private fun SectionLabel(text: String, topSpacing: Dp) {
    Text(
        text = text,
        style = AppType.eyebrowWide,
        color = SettingsTone.sectionLabel,
        modifier = Modifier.padding(
            start = SettingsConstants.sectionLabelInset,
            top = topSpacing,
            bottom = SettingsConstants.sectionLabelSpacing,
        ),
    )
}

@Composable
private fun QuietHoursCard(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    SettingsCard {
        SettingsRow(
            icon = { MoonIcon(Modifier.size(SettingsConstants.iconSize)) },
            iconBox = SettingsTone.moonBox,
            title = stringResource(R.string.settings_quiet_toggle_title),
            subtitle = stringResource(R.string.settings_quiet_toggle_subtitle),
        ) {
            SettingsToggle(
                checked = state.quietHoursEnabled,
                onToggle = { onIntent(SettingsIntent.ToggleQuietHours) },
                contentDescription = stringResource(R.string.settings_quiet_toggle_a11y),
            )
        }
        SettingsRowDivider()
        SettingsRow(
            icon = { QuietClockIcon(handsForward = true, modifier = Modifier.size(SettingsConstants.iconSize)) },
            iconBox = SettingsTone.clockBox,
            title = stringResource(R.string.settings_quiet_starts),
            enabled = state.quietHoursEnabled,
        ) {
            TimeChip(
                hour = state.quietHoursStart,
                onClick = { onIntent(SettingsIntent.OpenHourPicker(HourPickerTarget.QuietStart)) },
                enabled = state.quietHoursEnabled,
            )
        }
        SettingsRowDivider()
        SettingsRow(
            icon = { QuietClockIcon(handsForward = false, modifier = Modifier.size(SettingsConstants.iconSize)) },
            iconBox = SettingsTone.clockBox,
            title = stringResource(R.string.settings_quiet_ends),
            enabled = state.quietHoursEnabled,
        ) {
            TimeChip(
                hour = state.quietHoursEnd,
                onClick = { onIntent(SettingsIntent.OpenHourPicker(HourPickerTarget.QuietEnd)) },
                enabled = state.quietHoursEnabled,
            )
        }
    }
}

@Composable
private fun QuietHoursSummary(state: SettingsState) {
    val text = if (state.quietHoursEnabled) {
        stringResource(
            R.string.settings_quiet_summary_on,
            SettingsUtils.formatHour(state.quietHoursStart),
            SettingsUtils.formatHour(state.quietHoursEnd),
        )
    } else {
        stringResource(R.string.settings_quiet_summary_off)
    }
    Text(
        text = text,
        style = AppType.suggestSub,
        color = SettingsTone.sectionLabel,
        modifier = Modifier.padding(
            start = SettingsConstants.summaryInset,
            end = SettingsConstants.summaryInset,
            top = SettingsConstants.summarySpacing,
        ),
    )
}

@Composable
private fun NotificationsCard(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    SettingsCard {
        SettingsRow(
            icon = { BellIcon(Modifier.size(SettingsConstants.iconSize)) },
            iconBox = SettingsTone.bellBox,
            title = stringResource(R.string.settings_break_reminders),
        ) {
            SettingsToggle(
                checked = state.remindersEnabled,
                onToggle = { onIntent(SettingsIntent.ToggleBreakReminders) },
                contentDescription = stringResource(R.string.settings_break_reminders_a11y),
            )
        }
        SettingsRowDivider()
        SettingsRow(
            icon = { SoundWavesIcon(Modifier.size(SettingsConstants.iconSize)) },
            iconBox = SettingsTone.soundBox,
            title = stringResource(R.string.settings_sounds_haptics),
        ) {
            SettingsToggle(
                checked = state.soundsEnabled,
                onToggle = { onIntent(SettingsIntent.ToggleSounds) },
                contentDescription = stringResource(R.string.settings_sounds_haptics_a11y),
            )
        }
    }
}
