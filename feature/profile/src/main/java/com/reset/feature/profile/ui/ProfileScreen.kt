package com.reset.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppDimens
import com.reset.core.designsystem.AppType
import com.reset.feature.profile.ProfileConstants
import com.reset.feature.profile.ProfileState
import com.reset.feature.profile.R
import com.reset.feature.profile.ui.components.BreaksTakenCard
import com.reset.feature.profile.ui.components.SettingsGearButton
import com.reset.feature.profile.ui.components.StreakCard

/**
 * Stateless "Weekly Progress" surface — the breaks-taken and streak stat cards, with the
 * Settings gear in the header (quiet hours live on the Settings page). Renders purely
 * from [ProfileState].
 */
@Composable
fun ProfileScreen(
    state: ProfileState,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(AppColors.surfaceWhite)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppDimens.screenPaddingH),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = AppDimens.screenPaddingV),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.profile_title),
                style = AppType.displayTitle,
                color = AppColors.ink,
                modifier = Modifier.weight(1f),
            )
            SettingsGearButton(onClick = onOpenSettings)
        }
        Text(
            text = stringResource(R.string.profile_subtitle),
            style = AppType.caption,
            color = AppColors.inkMuted,
            modifier = Modifier.padding(top = ProfileConstants.subtitleSpacing),
        )

        BreaksTakenCard(
            breaksTaken = state.breaksTaken,
            modifier = Modifier.padding(top = ProfileConstants.firstCardSpacing),
        )
        StreakCard(
            streakDays = state.streakDays,
            modifier = Modifier.padding(
                top = ProfileConstants.cardSpacing,
                bottom = AppDimens.screenPaddingV,
            ),
        )
    }
}
