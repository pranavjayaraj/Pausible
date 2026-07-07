package com.reset.core.designsystem

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale

/**
 * Sprout, the app mascot — a Canvas port of the design's inline SVG (200×210 viewBox) so
 * every screen shares one implementation instead of per-feature vector assets.
 *
 * The idle "breathe" scale and leaf sway mirror the design's CSS keyframes. Screens that
 * drive the breathing themselves (the guided-break pulse) pass [externalScale] and the
 * internal animation is skipped.
 */
enum class SproutExpression {
    /** Round eyes + open smile — the default face. */
    Happy,

    /** Eyes closed in soft upward arcs + a small smile — focus and guided breaks. */
    Calm,

    /** Squeezed-shut eyes + wide open mouth — the celebration face. */
    Excited,
}

private const val VIEWPORT_W = 200f
private const val VIEWPORT_H = 210f
private const val BREATHE_MIN = 1f
private const val BREATHE_MAX = 1.06f
private const val SWAY_MIN_DEG = -4f
private const val SWAY_MAX_DEG = 5f
private const val CHEEK_ALPHA = 0.45f

@Composable
fun SproutMascot(
    modifier: Modifier,
    expression: SproutExpression = SproutExpression.Happy,
    bodyColor: Color = AppColors.sproutBody,
    showCheeks: Boolean = true,
    breathe: Boolean = true,
    breathePeriodMs: Int = 4_000,
    externalScale: Float? = null,
) {
    val transition = rememberInfiniteTransition(label = "sprout")
    val breatheScale by transition.animateFloat(
        initialValue = BREATHE_MIN,
        targetValue = BREATHE_MAX,
        animationSpec = infiniteRepeatable(tween(breathePeriodMs / 2), repeatMode = RepeatMode.Reverse),
        label = "breathe",
    )
    val sway by transition.animateFloat(
        initialValue = SWAY_MIN_DEG,
        targetValue = SWAY_MAX_DEG,
        animationSpec = infiniteRepeatable(tween(breathePeriodMs / 2), repeatMode = RepeatMode.Reverse),
        label = "sway",
    )

    val bodyScale = externalScale ?: if (breathe) breatheScale else 1f

    Canvas(modifier) {
        val sx = size.width / VIEWPORT_W
        val sy = size.height / VIEWPORT_H
        fun p(x: Float, y: Float) = Offset(x * sx, y * sy)

        // Design transform-origin: 50% 75%.
        scale(bodyScale, pivot = p(100f, 157.5f)) {
            rotate(sway, pivot = p(100f, 60f)) {
                drawLeaves(sx, sy)
            }
            drawBody(sx, sy, bodyColor)
            drawFace(sx, sy, expression)
            if (showCheeks) drawCheeks(sx, sy, expression)
        }
    }
}

private fun DrawScope.drawLeaves(sx: Float, sy: Float) {
    fun path(block: Path.() -> Unit) = Path().apply(block)
    fun Path.m(x: Float, y: Float) = moveTo(x * sx, y * sy)
    fun Path.c(x1: Float, y1: Float, x2: Float, y2: Float, x: Float, y: Float) =
        cubicTo(x1 * sx, y1 * sy, x2 * sx, y2 * sy, x * sx, y * sy)

    // Left leaf: M100 62 C100 40 88 26 72 20 C74 40 84 56 100 62 Z
    drawPath(
        path {
            m(100f, 62f); c(100f, 40f, 88f, 26f, 72f, 20f); c(74f, 40f, 84f, 56f, 100f, 62f); close()
        },
        AppColors.sproutLeafDark,
    )
    // Right leaf: M100 62 C100 40 112 26 128 20 C126 40 116 56 100 62 Z
    drawPath(
        path {
            m(100f, 62f); c(100f, 40f, 112f, 26f, 128f, 20f); c(126f, 40f, 116f, 56f, 100f, 62f); close()
        },
        AppColors.sproutLeafLight,
    )
    // Stem.
    drawLine(
        AppColors.sproutLeafDark,
        Offset(100f * sx, 62f * sy),
        Offset(100f * sx, 76f * sy),
        strokeWidth = 7f * sx,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawBody(sx: Float, sy: Float, bodyColor: Color) {
    // M100 70 C146 70 168 102 168 138 C168 172 140 194 100 194 C60 194 32 172 32 138 C32 102 54 70 100 70 Z
    val body = Path().apply {
        moveTo(100f * sx, 70f * sy)
        cubicTo(146f * sx, 70f * sy, 168f * sx, 102f * sy, 168f * sx, 138f * sy)
        cubicTo(168f * sx, 172f * sy, 140f * sx, 194f * sy, 100f * sx, 194f * sy)
        cubicTo(60f * sx, 194f * sy, 32f * sx, 172f * sy, 32f * sx, 138f * sy)
        cubicTo(32f * sx, 102f * sy, 54f * sx, 70f * sy, 100f * sx, 70f * sy)
        close()
    }
    drawPath(body, bodyColor)
}

private fun DrawScope.drawFace(sx: Float, sy: Float, expression: SproutExpression) {
    val face = AppColors.sproutFace
    fun arc(x0: Float, y0: Float, cx: Float, cy: Float, x1: Float, y1: Float, width: Float) {
        val path = Path().apply {
            moveTo(x0 * sx, y0 * sy)
            quadraticTo(cx * sx, cy * sy, x1 * sx, y1 * sy)
        }
        drawPath(path, face, style = Stroke(width * sx, cap = StrokeCap.Round))
    }
    when (expression) {
        SproutExpression.Happy -> {
            drawCircle(face, radius = 7f * sx, center = Offset(76f * sx, 126f * sy))
            drawCircle(face, radius = 7f * sx, center = Offset(124f * sx, 126f * sy))
            arc(82f, 148f, 100f, 162f, 118f, 148f, width = 8f)
        }

        SproutExpression.Calm -> {
            arc(66f, 128f, 76f, 118f, 86f, 128f, width = 7f)
            arc(114f, 128f, 124f, 118f, 134f, 128f, width = 7f)
            arc(86f, 152f, 100f, 160f, 114f, 152f, width = 7f)
        }

        SproutExpression.Excited -> {
            arc(68f, 122f, 78f, 130f, 88f, 122f, width = 7f)
            arc(112f, 122f, 122f, 130f, 132f, 122f, width = 7f)
            val mouth = Path().apply {
                moveTo(76f * sx, 144f * sy)
                quadraticTo(100f * sx, 170f * sy, 124f * sx, 144f * sy)
                close()
            }
            drawPath(mouth, face)
        }
    }
}

private fun DrawScope.drawCheeks(sx: Float, sy: Float, expression: SproutExpression) {
    val cheek = AppColors.sproutCheek.copy(alpha = CHEEK_ALPHA)
    val cy = if (expression == SproutExpression.Excited) 138f else 142f
    val cxL = if (expression == SproutExpression.Excited) 56f else 58f
    val cxR = if (expression == SproutExpression.Excited) 144f else 142f
    drawCircle(cheek, radius = 9f * sx, center = Offset(cxL * sx, cy * sy))
    drawCircle(cheek, radius = 9f * sx, center = Offset(cxR * sx, cy * sy))
}
