package com.reset.feature.checkin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppDimens
import com.reset.core.designsystem.AppType
import com.reset.core.designsystem.BackIcon
import com.reset.feature.checkin.ChipId
import com.reset.feature.checkin.CheckInConstants
import com.reset.feature.checkin.CheckInState
import com.reset.feature.checkin.R
import com.reset.feature.checkin.navigation.CheckInIntent
import com.reset.feature.checkin.ui.components.ChipCard

/**
 * The chip grid — Check In's hero screen (design "2a"–"2d"). Ten chips, zero scroll: a
 * fixed base grid plus night/stillness swaps already resolved into [CheckInState.grid] by
 * the ViewModel, never read from Sense/clock here.
 */
@Composable
fun ChipGridScreen(
    state: CheckInState,
    onIntent: (CheckInIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isNight = state.isNight
    val backdrop = if (isNight) CheckInTone.nightBackground else AppColors.surface
    val ink = if (isNight) CheckInTone.nightInk else AppColors.ink
    val inkMuted = if (isNight) CheckInTone.nightInkMuted else AppColors.inkMuted

    Column(
        modifier
            .fillMaxSize()
            .background(backdrop)
            .statusBarsPadding()
            .padding(horizontal = AppDimens.screenPaddingH)
            .padding(top = 38.dp, bottom = 30.dp),
    ) {
        BackIcon(
            modifier = Modifier
                .size(20.dp)
                .clickable { onIntent(CheckInIntent.HandleBackPress) },
            tint = inkMuted,
        )

        Text(
            text = stringResource(R.string.checkin_title),
            style = AppType.welcomeTitle,
            color = ink,
            modifier = Modifier.padding(top = 22.dp),
        )
        Text(
            text = stringResource(R.string.checkin_subtitle),
            style = AppType.body,
            color = inkMuted,
            modifier = Modifier.padding(top = 8.dp),
        )

        val rows = state.grid.chunked(2)
        Column(
            Modifier.padding(top = CheckInConstants.sectionSpacing),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(CheckInConstants.chipGap),
        ) {
            for (row in rows) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(CheckInConstants.chipGap),
                ) {
                    for (chipId in row) {
                        ChipCard(
                            chipId = chipId,
                            isNight = isNight,
                            isSwapped = chipId == ChipId.CANT_SWITCH_OFF || chipId == ChipId.BEEN_SITTING_FOREVER,
                            isSelected = false,
                            isSuggested = chipId == state.suggestedChip,
                            anySelected = false,
                            onClick = { onIntent(CheckInIntent.SelectChip(chipId)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = stringResource(R.string.checkin_just_browsing),
            style = AppType.skipLabel,
            color = inkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onIntent(CheckInIntent.JustBrowsing) },
        )
    }
}
