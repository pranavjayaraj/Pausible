package com.reset.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppDimens
import com.reset.core.designsystem.AppType
import com.reset.feature.profile.ProfileConstants
import com.reset.feature.profile.ProfileState
import com.reset.feature.profile.R
import com.reset.feature.profile.ui.components.BreaksTakenCard
import com.reset.feature.profile.ui.components.FocusBarChart
import com.reset.feature.profile.ui.components.QuietHoursCard
import com.reset.feature.profile.ui.components.StreakCard

/**
 * Stateless "Weekly Progress" surface — the breaks-taken and streak stat cards over the
 * focus-hours bar chart, per the design. Renders purely from [ProfileState].
 */
@Composable
fun ProfileScreen(
    state: ProfileState,
    onAdjustQuietHoursStart: (deltaHours: Int) -> Unit,
    onAdjustQuietHoursEnd: (deltaHours: Int) -> Unit,
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
        Text(
            text = stringResource(R.string.profile_title),
            style = AppType.displayTitle,
            color = AppColors.ink,
            modifier = Modifier.padding(top = AppDimens.screenPaddingV),
        )
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
            modifier = Modifier.padding(top = ProfileConstants.cardSpacing),
        )

        Text(
            text = stringResource(R.string.profile_chart_title),
            style = AppType.statsSection,
            color = AppColors.ink,
            modifier = Modifier.padding(top = ProfileConstants.chartTitleSpacing),
        )
        FocusBarChart(
            minutesPerDay = state.focusMinutesPerDay,
            todayIndex = state.todayIndex,
            dayInitials = stringArrayResource(R.array.profile_day_initials).toList(),
            modifier = Modifier.padding(top = ProfileConstants.chartTopSpacing),
        )

        QuietHoursCard(
            startHour = state.quietHoursStart,
            endHour = state.quietHoursEnd,
            onAdjustStart = onAdjustQuietHoursStart,
            onAdjustEnd = onAdjustQuietHoursEnd,
            modifier = Modifier.padding(
                top = ProfileConstants.chartTitleSpacing,
                bottom = AppDimens.screenPaddingV,
            ),
        )
    }
}
