package com.reset.feature.home.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppDimens
import com.reset.core.designsystem.AppType
import com.reset.core.designsystem.BreathIcon
import com.reset.core.designsystem.PlusIcon
import com.reset.feature.home.DayPeriod
import com.reset.feature.home.HomeConstants
import com.reset.feature.home.HomeState
import com.reset.feature.home.R
import com.reset.feature.home.navigation.HomeIntent
import com.reset.feature.home.ui.components.CelebrationBannerCard
import com.reset.feature.home.ui.components.CheckInCard
import com.reset.feature.home.ui.components.HomeQuickActionRow
import com.reset.feature.home.ui.components.HomeStatTile

/**
 * Stateless Home surface — the design's "Check In" Home tab. Renders purely from
 * [HomeState]; every event flows up through [onIntent]. The celebration banner pops over
 * the content when a break was just finished.
 */
@Composable
fun HomeScreen(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isNight = state.dayPeriod == DayPeriod.NIGHT
    val backdrop = if (isNight) HomeTone.nightBackground else AppColors.surfaceWarm
    val ink = if (isNight) HomeTone.nightInk else AppColors.ink
    val inkMuted = if (isNight) HomeTone.nightInkMuted else AppColors.inkMuted

    Box(modifier.fillMaxSize().background(backdrop)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = AppDimens.screenPaddingH,
                    vertical = AppDimens.screenPaddingV,
                ),
        ) {
            val weekdayNames = stringArrayResource(R.array.home_weekday_names)
            if (!isNight) {
                Text(
                    text = weekdayNames.getOrElse(state.dayOfWeekIndex) { weekdayNames[0] },
                    style = AppType.body,
                    color = inkMuted,
                )
            }
            Text(
                text = stringResource(greetingRes(state.dayPeriod)),
                style = AppType.welcomeTitle,
                color = ink,
                modifier = Modifier.padding(top = 4.dp),
            )

            CheckInCard(
                checkIn = state.checkIn,
                onClickCta = { onIntent(HomeIntent.OpenCheckIn) },
                onClickSkip = { onIntent(HomeIntent.DismissCheckIn) },
                modifier = Modifier.padding(top = HomeConstants.sectionSpacing),
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = HomeConstants.cardSpacing),
                horizontalArrangement = Arrangement.spacedBy(HomeConstants.cardSpacing),
            ) {
                HomeStatTile(
                    eyebrow = stringResource(R.string.home_stat_today),
                    value = pluralStringResource(R.plurals.home_stat_today_pauses, state.todayPauses, state.todayPauses),
                    sub = stringResource(R.string.home_stat_today_quiet_min, state.todayQuietMin),
                    eyebrowColor = AppColors.accentDark,
                    modifier = Modifier.weight(1f),
                )
                state.nextNudgeAt?.let { nextNudgeAt ->
                    HomeStatTile(
                        eyebrow = stringResource(R.string.home_stat_next_nudge),
                        value = nextNudgeAt,
                        sub = stringResource(R.string.home_stat_next_nudge_sub),
                        eyebrowColor = AppColors.teal,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Column(
                Modifier.padding(top = HomeConstants.cardSpacing),
                verticalArrangement = Arrangement.spacedBy(HomeConstants.gridSpacing),
            ) {
                HomeQuickActionRow(
                    title = stringResource(R.string.home_quick_breather_title),
                    sub = stringResource(R.string.home_quick_breather_sub),
                    icon = { iconModifier -> BreathIcon(iconModifier, tint = AppColors.teal) },
                    onClick = { onIntent(HomeIntent.StartDeepBreathing) },
                )
                HomeQuickActionRow(
                    title = stringResource(R.string.home_quick_builder_title),
                    sub = stringResource(R.string.home_quick_builder_sub),
                    icon = { iconModifier -> PlusIcon(iconModifier, tint = AppColors.ink) },
                    onClick = { onIntent(HomeIntent.OpenBuilder) },
                )
            }
        }

        AnimatedVisibility(
            visible = state.celebration != null,
            enter = slideInVertically { it / 2 } + fadeIn(),
            exit = slideOutVertically { it / 2 } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = AppDimens.screenPaddingH,
                    end = AppDimens.screenPaddingH,
                    bottom = AppDimens.screenPaddingH,
                ),
        ) {
            CelebrationBannerCard(
                title = stringResource(R.string.home_celebrate_title),
                message = stringResource(
                    R.string.home_celebrate_message,
                    state.celebration?.streakDays ?: state.streak,
                ),
            )
        }
    }
}

private fun greetingRes(dayPeriod: DayPeriod): Int = when (dayPeriod) {
    DayPeriod.MORNING -> R.string.home_greeting_morning
    DayPeriod.AFTERNOON -> R.string.home_greeting_afternoon
    DayPeriod.EVENING -> R.string.home_greeting_evening
    DayPeriod.NIGHT -> R.string.home_greeting_night
}
