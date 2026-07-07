package com.reset.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

private const val GUIDE_SCALE_IN = 1.12f
private const val GUIDE_SCALE_OUT = 0.92f
private val phaseSpacing = 74.dp
private val dotSize = 9.dp
private val dotSpacing = 8.dp

/**
 * The guided-break pulse: two soft halos and Sprout expanding on the inhale and settling
 * on the exhale, with the phase label and cycle dots underneath. Stateless — the ViewModel
 * owns the breath clock and passes the current phase down; [phaseLabel] is feature copy.
 */
@Composable
fun BreathingGuide(
    inhale: Boolean,
    phaseDurationMs: Int,
    phaseLabel: String,
    totalCycles: Int,
    completedCycles: Int,
    modifier: Modifier = Modifier,
) {
    val breathScale by animateFloatAsState(
        targetValue = if (inhale) GUIDE_SCALE_IN else GUIDE_SCALE_OUT,
        animationSpec = tween(phaseDurationMs),
        label = "guideScale",
    )

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(AppDimens.guideHaloOuter)
                    .scale(breathScale)
                    .background(AppColors.guideHalo, CircleShape),
            )
            Box(
                Modifier
                    .size(AppDimens.guideHaloInner)
                    .scale(breathScale)
                    .background(AppColors.guideHalo, CircleShape),
            )
            SproutMascot(
                modifier = Modifier.size(AppDimens.guideMascot),
                expression = SproutExpression.Calm,
                breathe = false,
                externalScale = breathScale,
            )
        }

        Column(
            Modifier.padding(top = phaseSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(text = phaseLabel, style = AppType.breathPhase.copy(color = AppColors.textOnDark))
            Row(
                Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(dotSpacing),
            ) {
                repeat(totalCycles) { index ->
                    val color = if (index <= completedCycles) AppColors.cycleDotActive else AppColors.cycleDotIdle
                    Box(Modifier.size(dotSize).background(color, CircleShape))
                }
            }
        }
    }
}
