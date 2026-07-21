package com.reset.feature.checkin.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppShapes
import com.reset.core.designsystem.AppType
import com.reset.feature.checkin.ChipId
import com.reset.feature.checkin.CheckInConstants
import com.reset.feature.checkin.ui.CheckInTone
import com.reset.feature.checkin.ui.emoji
import com.reset.feature.checkin.ui.labelRes

/** One chip: emoji + label, selected/glow/night states — design "2a–2d". Unselected chips
 *  fade once anything is selected, echoing the design's "others recede" state. */
@Composable
fun ChipCard(
    chipId: ChipId,
    isNight: Boolean,
    isSwapped: Boolean,
    isSelected: Boolean,
    isSuggested: Boolean,
    anySelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = when {
        isSelected -> AppColors.teal
        isNight && isSwapped -> CheckInTone.nightChipSwapped
        isNight -> CheckInTone.nightChip
        else -> AppColors.surfaceWhite
    }
    val textColor = when {
        isSelected -> AppColors.textOnDark
        isNight && isSwapped -> CheckInTone.nightSwapAccent
        isNight -> CheckInTone.nightInk
        else -> AppColors.ink
    }
    val fadedAlpha = if (anySelected && !isSelected) DIMMED_ALPHA else 1f

    val glowAlpha by if (isSuggested) {
        val transition = rememberInfiniteTransition(label = "chipGlow")
        transition.animateFloat(
            initialValue = GLOW_MIN_ALPHA,
            targetValue = GLOW_MAX_ALPHA,
            animationSpec = infiniteRepeatable(tween(GLOW_PERIOD_MS), repeatMode = RepeatMode.Reverse),
            label = "chipGlowAlpha",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Box(
        modifier
            .height(CheckInConstants.chipHeight)
            .clip(AppShapes.card)
            .background(background)
            .let { if (isSuggested) it.border(1.5.dp, AppColors.accent.copy(alpha = glowAlpha), AppShapes.card) else it }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(fadedAlpha)) {
            Text(text = chipId.emoji(), fontSize = 22.sp)
            Text(
                text = stringResource(chipId.labelRes()),
                style = AppType.chip,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, start = 6.dp, end = 6.dp),
            )
        }
    }
}

/** One follow-up option: icon + label + sub-label — design "3a". */
@Composable
fun FollowUpOptionCard(
    label: String,
    sub: String,
    background: Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .height(CheckInConstants.followUpCardHeight)
            .clip(AppShapes.card)
            .background(background)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.padding(top = 40.dp)) { icon() }
        Text(text = label, style = AppType.sectionTitle, color = AppColors.ink, modifier = Modifier.padding(top = 14.dp))
        Text(text = sub, style = AppType.caption, color = AppColors.inkMuted, modifier = Modifier.padding(top = 2.dp))
    }
}

/** The offer's quiet evidence line — a small leaf mark instead of a generic checkmark, so it
 *  reads as "grown," matching Sprout's own leaf motif. */
@Composable
fun EvidenceTag(text: String, modifier: Modifier = Modifier) {
    Text(text = "🌿 $text", style = AppType.legal, color = AppColors.inkMuted, modifier = modifier)
}

private const val DIMMED_ALPHA = 0.45f
private const val GLOW_MIN_ALPHA = 0f
private const val GLOW_MAX_ALPHA = 0.55f
private const val GLOW_PERIOD_MS = 2000
