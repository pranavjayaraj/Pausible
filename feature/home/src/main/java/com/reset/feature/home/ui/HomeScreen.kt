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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppDimens
import com.reset.core.designsystem.AppType
import com.reset.feature.home.HomeConstants
import com.reset.feature.home.HomeState
import com.reset.feature.home.R
import com.reset.feature.home.navigation.HomeIntent
import com.reset.feature.home.ui.components.BreathingTile
import com.reset.feature.home.ui.components.CelebrationBannerCard
import com.reset.feature.home.ui.components.CustomTile
import com.reset.feature.home.ui.components.ExploreMoreButton
import com.reset.feature.home.ui.components.MeditateHeroCard
import com.reset.feature.home.ui.components.MindfulTipCard

/**
 * Stateless Home surface — the design's Home tab. Renders purely from [HomeState]; every
 * event flows up through [onIntent]. The celebration banner pops over the content when a
 * break was just finished.
 */
@Composable
fun HomeScreen(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val facts = stringArrayResource(R.array.home_break_facts)
    val factIndex = state.factIndex.coerceIn(0, facts.lastIndex)

    Box(modifier.fillMaxSize().background(AppColors.surfaceWarm)) {
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
            Text(
                text = stringResource(R.string.home_fact, facts[factIndex]),
                style = AppType.factTitle,
                color = AppColors.ink,
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = AppDimens.screenPaddingH / 2),
                horizontalArrangement = Arrangement.End,
            ) {
                ExploreMoreButton(onClick = { onIntent(HomeIntent.ExploreMore) })
            }

            MeditateHeroCard(
                durationMin = state.durationMin,
                onClick = { onIntent(HomeIntent.StartMeditate) },
                modifier = Modifier.padding(top = HomeConstants.cardSpacing),
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = HomeConstants.cardSpacing),
                horizontalArrangement = Arrangement.spacedBy(HomeConstants.cardSpacing),
            ) {
                BreathingTile(
                    onClick = { onIntent(HomeIntent.StartDeepBreathing) },
                    modifier = Modifier.weight(1f),
                )
                CustomTile(
                    onClick = { onIntent(HomeIntent.OpenBuilder) },
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text = stringResource(R.string.home_mindful_tip),
                style = AppType.eyebrowWide,
                color = AppColors.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = HomeConstants.sectionSpacing),
            )
            MindfulTipCard(
                body = stringResource(R.string.home_tip_body),
                modifier = Modifier.padding(top = HomeConstants.gridSpacing),
            )
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
                    state.celebration?.streakDays ?: state.stats.streak,
                ),
            )
        }
    }
}
