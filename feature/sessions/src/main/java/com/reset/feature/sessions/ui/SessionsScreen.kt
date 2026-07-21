package com.reset.feature.sessions.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppDimens
import com.reset.core.designsystem.AppType
import com.reset.core.designsystem.BreathIcon
import com.reset.core.designsystem.ClosedEyeIcon
import com.reset.core.designsystem.StretchIcon
import com.reset.feature.sessions.R
import com.reset.feature.sessions.SessionsConstants
import com.reset.feature.sessions.SessionsState
import com.reset.feature.sessions.api.SessionScriptIds
import com.reset.feature.sessions.navigation.SessionsIntent
import com.reset.feature.sessions.ui.components.BreakOptionCard

/**
 * Stateless break-suggestion surface — "Time for a breather!" over the yellow header band,
 * with the three break cards and the skip link. Renders purely from [SessionsState]; every
 * event flows up through [onIntent].
 */
@Composable
fun SessionsScreen(
    state: SessionsState,
    onIntent: (SessionsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(AppColors.surfaceBreak)) {
        // The yellow band with the cream curve rising over it, like the design.
        Box(
            Modifier
                .fillMaxWidth()
                .height(SessionsConstants.headerBandHeight)
                .background(AppColors.breakHeader),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = SessionsConstants.headerBandHeight - SessionsConstants.headerCurveHeight)
                .height(SessionsConstants.headerCurveHeight)
                .background(
                    color = AppColors.surfaceBreak,
                    shape = RoundedCornerShape(
                        topStart = SessionsConstants.headerCurveHeight,
                        topEnd = SessionsConstants.headerCurveHeight,
                    ),
                ),
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimens.screenPaddingH),
        ) {
            Text(
                text = stringResource(R.string.sessions_title),
                style = AppType.displayTitle,
                color = AppColors.ink,
                modifier = Modifier.padding(top = 32.dp),
            )
            Text(
                text = stringResource(R.string.sessions_subtitle),
                style = AppType.bodySmall,
                color = AppColors.breakHeaderInk,
                modifier = Modifier.padding(top = 8.dp, end = 60.dp),
            )

            Column(
                Modifier.padding(top = 26.dp),
                verticalArrangement = Arrangement.spacedBy(SessionsConstants.cardSpacing),
            ) {
                BreakOptionCard(
                    title = stringResource(R.string.sessions_stretch),
                    duration = stringResource(R.string.sessions_stretch_mins),
                    background = AppColors.stretchCardGradient,
                    contentColor = AppColors.textOnDark,
                    metaColor = AppColors.textOnDark.copy(alpha = 0.9f),
                    icon = { iconModifier -> StretchIcon(iconModifier, tint = AppColors.textOnDark) },
                    onClick = { onIntent(SessionsIntent.PickScript(SessionScriptIds.THE_UNFOLD)) },
                )
                BreakOptionCard(
                    title = stringResource(R.string.sessions_meditate),
                    duration = stringResource(R.string.sessions_meditate_mins),
                    background = androidx.compose.ui.graphics.SolidColor(AppColors.meditateCard),
                    contentColor = AppColors.meditateCardInk,
                    metaColor = AppColors.meditateCardSub,
                    icon = { iconModifier -> ClosedEyeIcon(iconModifier, tint = AppColors.meditateCardInk) },
                    onClick = { onIntent(SessionsIntent.PickScript(SessionScriptIds.HORIZON)) },
                )
                BreakOptionCard(
                    title = stringResource(R.string.sessions_breathing),
                    duration = stringResource(R.string.sessions_breathing_mins),
                    background = androidx.compose.ui.graphics.SolidColor(AppColors.breathingCard),
                    contentColor = AppColors.textOnDark,
                    metaColor = AppColors.textOnDark.copy(alpha = 0.92f),
                    icon = { iconModifier -> BreathIcon(iconModifier, tint = AppColors.textOnDark) },
                    onClick = { onIntent(SessionsIntent.PickScript(SessionScriptIds.THE_SIGH)) },
                )
            }

            Text(
                text = stringResource(R.string.sessions_skip),
                style = AppType.skipLabel,
                color = AppColors.inkFaint,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = SessionsConstants.skipSpacing)
                    .clickable { onIntent(SessionsIntent.SkipBreak) },
            )
        }
    }
}
