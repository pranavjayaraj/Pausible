package com.reset.feature.checkin.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppDimens
import com.reset.core.designsystem.AppShapes
import com.reset.core.designsystem.AppType
import com.reset.core.designsystem.BackIcon
import com.reset.core.designsystem.ClockIcon
import com.reset.core.designsystem.SproutExpression
import com.reset.core.designsystem.SproutMascot
import com.reset.feature.checkin.CheckInState
import com.reset.feature.checkin.CheckInStep
import com.reset.feature.checkin.R
import com.reset.feature.checkin.navigation.CheckInIntent
import com.reset.feature.checkin.ui.components.EvidenceTag
import com.reset.model.domain.checkin.NeedState

/**
 * The offer: one session, honest duration, a warm "why this one" line, a quiet evidence tag,
 * a full-width start, a subordinate "Not it?" swap — design "3b" (direct) / "3c" (empathy).
 */
@Composable
fun OfferScreen(
    state: CheckInState,
    onIntent: (CheckInIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val step = state.step as? CheckInStep.Offer ?: return
    val session = step.current
    val empathyToned = step.needState.isEmpathyToned()
    val cardBackground = if (empathyToned) CheckInTone.empathyCard else AppColors.surfaceWhite

    var pressed by remember { mutableStateOf(false) }
    val ctaScale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "ctaScale")

    Column(
        modifier
            .fillMaxSize()
            .background(AppColors.surface)
            .statusBarsPadding()
            .padding(horizontal = AppDimens.screenPaddingH)
            .padding(top = 38.dp, bottom = 30.dp),
    ) {
        BackIcon(
            modifier = Modifier
                .size(20.dp)
                .clickable { onIntent(CheckInIntent.HandleBackPress) },
            tint = AppColors.inkMuted,
        )

        Text(
            text = stringResource(R.string.checkin_offer_for, stringResource(step.needState.labelRes())),
            style = AppType.eyebrowWide,
            color = AppColors.inkMuted,
            modifier = Modifier.padding(top = 26.dp),
        )

        Box(
            Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .clip(AppShapes.card)
                .background(cardBackground)
                .padding(24.dp),
        ) {
            SproutMascot(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(56.dp),
                expression = if (empathyToned) SproutExpression.Calm else SproutExpression.Happy,
            )

            Column {
                Text(text = session.displayName, style = AppType.displayTitle, color = AppColors.ink)

                Row(
                    Modifier
                        .padding(top = 10.dp)
                        .clip(AppShapes.input)
                        .background(if (empathyToned) AppColors.surfaceWhite else AppColors.panel)
                        .padding(horizontal = 13.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ClockIcon(Modifier.size(15.dp), tint = AppColors.teal)
                    Text(
                        text = formatDuration(session.totalSec),
                        style = AppType.suggestTitle,
                        color = AppColors.teal,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                Text(
                    text = session.whyThisOne,
                    style = AppType.body,
                    color = AppColors.inkBody,
                    modifier = Modifier.padding(top = 16.dp),
                )

                EvidenceTag(text = session.evidenceTag, modifier = Modifier.padding(top = 16.dp))

                Box(
                    Modifier
                        .padding(top = 20.dp)
                        .fillMaxWidth()
                        .scale(ctaScale)
                        .clip(AppShapes.button)
                        .background(AppColors.teal)
                        .clickable {
                            pressed = true
                            onIntent(CheckInIntent.StartOffered)
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = stringResource(R.string.checkin_offer_start), style = AppType.button, color = AppColors.textOnDark)
                }
            }
        }

        if (!step.showingAlternate) {
            Text(
                text = stringResource(R.string.checkin_offer_not_it, step.alternate.displayName),
                style = AppType.caption,
                color = AppColors.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
                    .clickable { onIntent(CheckInIntent.TryAlternate) },
            )
        }

        if (step.needState.showsSupportLink()) {
            Text(
                text = stringResource(R.string.checkin_offer_support),
                style = AppType.legal,
                color = AppColors.inkFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .clickable { onIntent(CheckInIntent.OpenSupportResources) },
            )
        }
    }
}

@Composable
private fun formatDuration(totalSec: Int): String = if (totalSec < 60) {
    stringResource(R.string.checkin_offer_duration_seconds, totalSec)
} else {
    stringResource(R.string.checkin_offer_duration_minutes, (totalSec + 30) / 60)
}
