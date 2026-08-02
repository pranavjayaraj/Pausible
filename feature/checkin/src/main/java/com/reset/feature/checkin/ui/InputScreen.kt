package com.reset.feature.checkin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppDimens
import com.reset.core.designsystem.AppType
import com.reset.core.designsystem.BackIcon
import com.reset.core.designsystem.SproutExpression
import com.reset.core.designsystem.SproutMascot
import com.reset.feature.checkin.ChipId
import com.reset.feature.checkin.CheckInState
import com.reset.feature.checkin.InputMode
import com.reset.feature.checkin.R
import com.reset.feature.checkin.navigation.CheckInIntent

/**
 * Check In's conversational hero screen (design "Check In Chat"): breathing Sprout + a speech
 * bubble, a Text/Voice toggle, and — in Text mode — quick-chip pills over a free-text box.
 * Quick chips dispatch [CheckInIntent.SelectChip] (the existing NeedState path); the text box
 * dispatches [CheckInIntent.SendChat] (the classifier path). Voice is deferred.
 */
@Composable
fun InputScreen(
    state: CheckInState,
    onIntent: (CheckInIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(AppColors.surface)
            .statusBarsPadding(),
    ) {
        // Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = AppDimens.screenPaddingH, end = AppDimens.screenPaddingH, top = 22.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(AppColors.surfaceWhite)
                    .border(1.5.dp, AppColors.hairlineCard, CircleShape)
                    .clickable { onIntent(CheckInIntent.HandleBackPress) },
                contentAlignment = Alignment.Center,
            ) { BackIcon(modifier = Modifier.size(18.dp), tint = AppColors.inkSoft) }
            Text(text = stringResource(R.string.checkin_title), style = AppType.screenTitle, color = AppColors.ink)
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SproutMascot(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(76.dp),
                expression = SproutExpression.Happy,
            )

            // Speech bubble
            Box(
                Modifier
                    .padding(top = 14.dp)
                    .widthIn(max = 280.dp)
                    .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                    .background(AppColors.surfaceWhite)
                    .border(1.5.dp, AppColors.hairlineCard, RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.checkin_prompt),
                    style = AppType.bodySmall,
                    color = AppColors.ink,
                    textAlign = TextAlign.Center,
                )
            }

            // Text / Voice toggle
            ModeToggle(
                mode = state.inputMode,
                onSelect = { onIntent(CheckInIntent.SetInputMode(it)) },
                modifier = Modifier.padding(top = 22.dp),
            )

            when (state.inputMode) {
                InputMode.Text -> TextInput(state = state, onIntent = onIntent)
                InputMode.Voice -> VoiceComingSoon(onIntent = onIntent)
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.checkin_just_browsing),
                style = AppType.skipLabel,
                color = AppColors.inkMuted,
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .clickable { onIntent(CheckInIntent.JustBrowsing) },
            )
        }
    }
}

@Composable
private fun ModeToggle(mode: InputMode, onSelect: (InputMode) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.surfaceWhite)
            .border(1.5.dp, AppColors.hairlineCard, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ModeButton(stringResource(R.string.checkin_mode_text), mode == InputMode.Text) { onSelect(InputMode.Text) }
        ModeButton(stringResource(R.string.checkin_mode_voice), mode == InputMode.Voice) { onSelect(InputMode.Voice) }
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) AppColors.teal else AppColors.surfaceWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = AppType.chip,
            color = if (selected) AppColors.textOnDark else AppColors.inkMuted,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TextInput(state: CheckInState, onIntent: (CheckInIntent) -> Unit) {
    // Quick chips
    FlowRow(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 340.dp)
            .padding(top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (chipId in state.grid) {
            QuickChip(
                chipId = chipId,
                suggested = chipId == state.suggestedChip,
                onClick = { onIntent(CheckInIntent.SelectChip(chipId)) },
            )
        }
    }

    // Free-text box
    Box(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 340.dp)
            .padding(top = 16.dp)
            .height(84.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.surfaceWhite)
            .border(1.5.dp, AppColors.hairlineCard, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (state.chatText.isEmpty()) {
            Text(stringResource(R.string.checkin_text_placeholder), style = AppType.body, color = AppColors.inkFaint)
        }
        BasicTextField(
            value = state.chatText,
            onValueChange = { onIntent(CheckInIntent.ChatTextChanged(it)) },
            textStyle = AppType.body.copy(color = AppColors.ink),
            cursorBrush = SolidColor(AppColors.teal),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    // Send
    val enabled = state.sendEnabled
    Box(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 340.dp)
            .padding(top = 10.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) AppColors.teal else AppColors.teal.copy(alpha = 0.4f))
            .clickable(enabled = enabled) { onIntent(CheckInIntent.SendChat) },
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.checkin_send), style = AppType.button, color = AppColors.textOnDark)
    }

    // Subtle, non-blocking — this Send's input already fell back to the chips above; this
    // just says the model for *next* time is on its way.
    if (state.embedModelFetching) {
        Text(
            text = stringResource(R.string.checkin_embed_model_fetching),
            style = AppType.legal,
            color = AppColors.inkFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 340.dp)
                .padding(top = 10.dp),
        )
    }
}

@Composable
private fun VoiceComingSoon(onIntent: (CheckInIntent) -> Unit) {
    Column(
        Modifier.padding(top = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(
            Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(AppColors.teal.copy(alpha = 0.5f))
                .clickable { onIntent(CheckInIntent.MicTap) },
            contentAlignment = Alignment.Center,
        ) { Text("🎙", fontSize = 34.sp) }
        Text(
            text = stringResource(R.string.checkin_voice_tap),
            style = AppType.paceLabel,
            color = AppColors.inkMuted,
        )
    }
}

@Composable
private fun QuickChip(chipId: ChipId, suggested: Boolean, onClick: () -> Unit) {
    val border = if (suggested) AppColors.accent else AppColors.hairlineCard
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.surfaceWhite)
            .border(1.5.dp, border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(chipId.emoji(), fontSize = 13.sp)
        Text(stringResource(chipId.labelRes()), style = AppType.chip, color = AppColors.inkSoft)
    }
}
