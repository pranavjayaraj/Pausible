package com.reset.feature.checkin.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppType
import com.reset.core.designsystem.SproutExpression
import com.reset.core.designsystem.SproutMascot
import com.reset.feature.checkin.R

/** The brief "Finding your minute…" beat (design "Finding Session"): a deep-teal field with
 *  a breathing Sprout inside expanding ripple rings. Pure ambient loading — no interaction. */
@Composable
fun FindingScreen(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .background(AppColors.tealDeep),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
            Ripple(delayFraction = 0f)
            Ripple(delayFraction = 0.33f)
            Ripple(delayFraction = 0.66f)
            SproutMascot(modifier = Modifier.size(86.dp), expression = SproutExpression.Calm)
        }
        Text(
            text = stringResource(R.string.checkin_finding),
            style = AppType.statsSection,
            color = AppColors.textOnDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 28.dp),
        )
    }
}

@Composable
private fun Ripple(delayFraction: Float) {
    val transition = rememberInfiniteTransition(label = "ripple")
    val period = 2200
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(period), repeatMode = RepeatMode.Restart),
        label = "rippleProgress",
    )
    // Offset each ring in its cycle for the staggered-wave look.
    val p = (progress + delayFraction) % 1f
    Box(
        Modifier
            .size(180.dp)
            .scale(0.6f + p * 0.4f)
            .graphicsLayer { alpha = (1f - p) * 0.35f }
            .border(2.dp, AppColors.textOnDark, CircleShape),
    )
}
