package com.reset.feature.checkin.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppDimens
import com.reset.core.designsystem.AppShapes
import com.reset.core.designsystem.AppType
import com.reset.feature.checkin.CheckInConstants
import com.reset.feature.checkin.CheckInState
import com.reset.feature.checkin.CheckInStep
import com.reset.feature.checkin.R
import com.reset.feature.checkin.navigation.CheckInIntent
import com.reset.feature.checkin.ui.components.FollowUpOptionCard
import com.reset.model.domain.checkin.NeedState

/**
 * The single binary clarifier — a sheet offered over the (dimmed, non-interactive) chip
 * grid, not "question 2 of N": no progress indicator, ever. Design "3a".
 */
@Composable
fun FollowUpScreen(
    state: CheckInState,
    onIntent: (CheckInIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val step = state.step as? CheckInStep.FollowUp ?: return
    val isNight = state.isNight

    val (title, optionA, optionB) = when (step.originNeedState) {
        NeedState.WOUND_UP -> Triple(
            stringResource(R.string.checkin_followup_wound_up_title),
            FollowUpOption("😤", R.string.checkin_followup_wound_up_label, R.string.checkin_followup_wound_up_sub, CheckInTone.woundUpCard) {
                onIntent(CheckInIntent.AnswerWoundUp)
            },
            FollowUpOption("🥱", R.string.checkin_followup_worn_out_label, R.string.checkin_followup_worn_out_sub, CheckInTone.wornOutCard) {
                onIntent(CheckInIntent.AnswerWornOut)
            },
        )
        else -> Triple(
            stringResource(R.string.checkin_followup_body_title),
            FollowUpOption("🙇", R.string.checkin_followup_neck_label, R.string.checkin_followup_neck_sub, CheckInTone.woundUpCard) {
                onIntent(CheckInIntent.AnswerNeckShoulders)
            },
            FollowUpOption("✋", R.string.checkin_followup_wrists_label, R.string.checkin_followup_wrists_sub, CheckInTone.wornOutCard) {
                onIntent(CheckInIntent.AnswerWristsHands)
            },
        )
    }

    Box(modifier.fillMaxSize()) {
        // The grid behind, dimmed and non-interactive — a scrim stands in for the design's
        // blur (no full RenderEffect blur here, to keep this screen minSdk-simple).
        ChipGridScreen(state = state, onIntent = {})
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = SCRIM_ALPHA)))

        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(animationSpec = tween(SLIDE_IN_MS)) { it },
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(1f),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(if (isNight) CheckInTone.nightChip else AppColors.surfaceWhite)
                    .padding(horizontal = AppDimens.screenPaddingH, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .width(40.dp)
                        .height(5.dp)
                        .padding(bottom = 22.dp)
                        .clip(AppShapes.pill)
                        .background(AppColors.inkFaint.copy(alpha = 0.3f)),
                )

                Text(
                    text = title,
                    style = AppType.sectionTitle,
                    color = if (isNight) CheckInTone.nightInk else AppColors.ink,
                    textAlign = TextAlign.Center,
                )

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(CheckInConstants.chipGap),
                ) {
                    FollowUpOptionCard(
                        label = stringResource(optionA.labelRes),
                        sub = stringResource(optionA.subRes),
                        background = optionA.background,
                        icon = { Text(text = optionA.emoji, fontSize = FOLLOW_UP_ICON_SIZE) },
                        onClick = optionA.onClick,
                        modifier = Modifier.weight(1f),
                    )
                    FollowUpOptionCard(
                        label = stringResource(optionB.labelRes),
                        sub = stringResource(optionB.subRes),
                        background = optionB.background,
                        icon = { Text(text = optionB.emoji, fontSize = FOLLOW_UP_ICON_SIZE) },
                        onClick = optionB.onClick,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private data class FollowUpOption(
    val emoji: String,
    val labelRes: Int,
    val subRes: Int,
    val background: Color,
    val onClick: () -> Unit,
)

private val FOLLOW_UP_ICON_SIZE = 32.sp
private const val SCRIM_ALPHA = 0.55f
private const val SLIDE_IN_MS = 320
