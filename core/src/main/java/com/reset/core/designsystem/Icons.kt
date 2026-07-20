package com.reset.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Tiny Canvas icons that mirror the inline SVGs in the design, avoiding a
 * material-icons dependency. All paths are authored on a 24x24 grid and scaled.
 */

private const val GRID = 24f

@Composable
fun HomeIcon(modifier: Modifier = Modifier, tint: Color = AppColors.ink) {
    Canvas(modifier) {
        val unit = size.minDimension / GRID
        fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
        val stroke = Stroke(2f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // M3 10.5 12 3l9 7.5 / M5 9.5V20h14V9.5
        val roof = Path().apply {
            moveTo(p(3f, 10.5f).x, p(3f, 10.5f).y)
            lineTo(p(12f, 3f).x, p(12f, 3f).y)
            lineTo(p(21f, 10.5f).x, p(21f, 10.5f).y)
        }
        drawPath(roof, tint, style = stroke)
        val walls = Path().apply {
            moveTo(p(5f, 9.5f).x, p(5f, 9.5f).y)
            lineTo(p(5f, 20f).x, p(5f, 20f).y)
            lineTo(p(19f, 20f).x, p(19f, 20f).y)
            lineTo(p(19f, 9.5f).x, p(19f, 9.5f).y)
        }
        drawPath(walls, tint, style = stroke)
    }
}

/** The design's stopwatch: top button, round body, hands at 12→v-l. */
@Composable
fun TimerIcon(modifier: Modifier = Modifier, tint: Color = AppColors.ink) {
    Canvas(modifier) {
        val unit = size.minDimension / GRID
        fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
        val strokeWidth = 2f * unit
        // M10 2h4
        drawLine(tint, p(10f, 2f), p(14f, 2f), strokeWidth, StrokeCap.Round)
        // Body circle r8 centred at (12,12).
        drawCircle(tint, radius = 8f * unit, center = p(12f, 12f), style = Stroke(strokeWidth))
        // M12 9v4l2.5 1.5
        val hands = Path().apply {
            moveTo(p(12f, 9f).x, p(12f, 9f).y)
            lineTo(p(12f, 13f).x, p(12f, 13f).y)
            lineTo(p(14.5f, 14.5f).x, p(14.5f, 14.5f).y)
        }
        drawPath(hands, tint, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Three rising bars — the stats tab marker. */
@Composable
fun StatsIcon(modifier: Modifier = Modifier, tint: Color = AppColors.ink) {
    Canvas(modifier) {
        val unit = size.minDimension / GRID
        fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
        val strokeWidth = 2f * unit
        drawLine(tint, p(6f, 20f), p(6f, 14f), strokeWidth, StrokeCap.Round)
        drawLine(tint, p(12f, 20f), p(12f, 8f), strokeWidth, StrokeCap.Round)
        drawLine(tint, p(18f, 20f), p(18f, 11f), strokeWidth, StrokeCap.Round)
    }
}

@Composable
fun PlayIcon(modifier: Modifier = Modifier, tint: Color = AppColors.accent) {
    Canvas(modifier) {
        val unit = size.minDimension / GRID
        fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
        val triangle = Path().apply {
            moveTo(p(7f, 4f).x, p(7f, 4f).y)
            lineTo(p(20f, 12f).x, p(20f, 12f).y)
            lineTo(p(7f, 20f).x, p(7f, 20f).y)
            close()
        }
        drawPath(triangle, tint)
    }
}

/** Small clock used on the break-card duration tags. */
@Composable
fun ClockIcon(modifier: Modifier = Modifier, tint: Color = AppColors.ink) {
    Canvas(modifier) {
        val unit = size.minDimension / GRID
        fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
        val strokeWidth = 2.4f * unit
        drawCircle(tint, radius = 9f * unit, center = p(12f, 12f), style = Stroke(strokeWidth))
        val hands = Path().apply {
            moveTo(p(12f, 7f).x, p(12f, 7f).y)
            lineTo(p(12f, 12f).x, p(12f, 12f).y)
            lineTo(p(15f, 14f).x, p(15f, 14f).y)
        }
        drawPath(hands, tint, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Jumping-jack figure — the body-stretches break card. */
@Composable
fun StretchIcon(modifier: Modifier = Modifier, tint: Color = AppColors.textOnDark) {
    Canvas(modifier) {
        val unit = size.minDimension / GRID
        fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
        val strokeWidth = 2f * unit
        drawCircle(tint, radius = 1.8f * unit, center = p(12f, 4.5f), style = Stroke(strokeWidth))
        // Torso M12 7v6, arms M12 9l±5 2, legs M12 13l±3 6
        drawLine(tint, p(12f, 7f), p(12f, 13f), strokeWidth, StrokeCap.Round)
        drawLine(tint, p(12f, 9f), p(7f, 11f), strokeWidth, StrokeCap.Round)
        drawLine(tint, p(12f, 9f), p(17f, 11f), strokeWidth, StrokeCap.Round)
        drawLine(tint, p(12f, 13f), p(9f, 19f), strokeWidth, StrokeCap.Round)
        drawLine(tint, p(12f, 13f), p(15f, 19f), strokeWidth, StrokeCap.Round)
    }
}

/** Closed eye with a strike — the design's meditate break card. */
@Composable
fun ClosedEyeIcon(modifier: Modifier = Modifier, tint: Color = AppColors.ink) {
    Canvas(modifier) {
        val unit = size.minDimension / GRID
        fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
        val strokeWidth = 2f * unit
        // Upper lid: M2 12s3.5-6 10-6 10 6 10 6
        val lid = Path().apply {
            moveTo(p(2f, 12f).x, p(2f, 12f).y)
            cubicTo(p(5.5f, 6f).x, p(5.5f, 6f).y, p(18.5f, 6f).x, p(18.5f, 6f).y, p(22f, 12f).x, p(22f, 12f).y)
        }
        drawPath(lid, tint, style = Stroke(strokeWidth, cap = StrokeCap.Round))
        // Small under-curve: M8.5 13.5a3.5 3.5 0 0 0 5 0
        val under = Path().apply {
            addArc(
                Rect(center = p(11f, 13.5f), radius = 2.5f * unit),
                startAngleDegrees = 20f,
                sweepAngleDegrees = 140f,
            )
        }
        drawPath(under, tint, style = Stroke(strokeWidth, cap = StrokeCap.Round))
        // Strike: M3 4l18 16
        drawLine(tint, p(3f, 4f), p(21f, 20f), strokeWidth, StrokeCap.Round)
    }
}

/** Three wind lines — the deep-breathing break card. */
@Composable
fun BreathIcon(modifier: Modifier = Modifier, tint: Color = AppColors.textOnDark) {
    Canvas(modifier) {
        val unit = size.minDimension / GRID
        fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
        val stroke = Stroke(2f * unit, cap = StrokeCap.Round)
        // Simplified wind glyph: three streaks with curled tails.
        val top = Path().apply {
            moveTo(p(3f, 8f).x, p(3f, 8f).y)
            lineTo(p(12f, 8f).x, p(12f, 8f).y)
            cubicTo(p(15f, 8f).x, p(15f, 8f).y, p(15f, 3.5f).x, p(15f, 3.5f).y, p(12f, 4.2f).x, p(12f, 4.2f).y)
        }
        drawPath(top, tint, style = stroke)
        val mid = Path().apply {
            moveTo(p(3f, 12f).x, p(3f, 12f).y)
            lineTo(p(19f, 12f).x, p(19f, 12f).y)
            cubicTo(p(22f, 12f).x, p(22f, 12f).y, p(22f, 7.5f).x, p(22f, 7.5f).y, p(19f, 8.2f).x, p(19f, 8.2f).y)
        }
        drawPath(mid, tint, style = stroke)
        val bottom = Path().apply {
            moveTo(p(3f, 16f).x, p(3f, 16f).y)
            lineTo(p(16f, 16f).x, p(16f, 16f).y)
            cubicTo(p(19f, 16f).x, p(19f, 16f).y, p(19f, 20.5f).x, p(19f, 20.5f).y, p(16f, 19.8f).x, p(16f, 19.8f).y)
        }
        drawPath(bottom, tint, style = stroke)
    }
}

@Composable
fun BackIcon(modifier: Modifier = Modifier, tint: Color = AppColors.ink) {
    Canvas(modifier) {
        val unit = size.minDimension / GRID
        fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
        val path = Path().apply {
            moveTo(p(15f, 5f).x, p(15f, 5f).y)
            lineTo(p(8f, 12f).x, p(8f, 12f).y)
            lineTo(p(15f, 19f).x, p(15f, 19f).y)
        }
        drawPath(path, tint, style = Stroke(2f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun CloseIcon(modifier: Modifier = Modifier, tint: Color = AppColors.accentDark) {
    Canvas(modifier) {
        val unit = size.minDimension / GRID
        fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
        val strokeWidth = 2f * unit
        drawLine(tint, p(6f, 6f), p(18f, 18f), strokeWidth, StrokeCap.Round)
        drawLine(tint, p(18f, 6f), p(6f, 18f), strokeWidth, StrokeCap.Round)
    }
}

@Composable
fun PlusIcon(modifier: Modifier = Modifier, tint: Color = AppColors.textOnDark) {
    Canvas(modifier) {
        val unit = size.minDimension / GRID
        fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
        val strokeWidth = 2f * unit
        drawLine(tint, p(12f, 5f), p(12f, 19f), strokeWidth, StrokeCap.Round)
        drawLine(tint, p(5f, 12f), p(19f, 12f), strokeWidth, StrokeCap.Round)
    }
}

@Composable
fun AppleIcon(modifier: Modifier = Modifier, tint: Color = AppColors.textOnDark) {
    Canvas(modifier) {
        val unit = size.minDimension / GRID
        fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
        // Compact solid apple: body + leaf, close enough at 15dp.
        drawCircle(tint, radius = 4.6f * unit, center = p(9.5f, 13.5f))
        drawCircle(tint, radius = 4.6f * unit, center = p(14.5f, 13.5f))
        val leaf = Path().apply {
            moveTo(p(12f, 7.5f).x, p(12f, 7.5f).y)
            cubicTo(p(12.5f, 4.5f).x, p(12.5f, 4.5f).y, p(14.5f, 3f).x, p(14.5f, 3f).y, p(15.5f, 3f).x, p(15.5f, 3f).y)
            cubicTo(p(15.5f, 5f).x, p(15.5f, 5f).y, p(14f, 7.5f).x, p(14f, 7.5f).y, p(12f, 7.5f).x, p(12f, 7.5f).y)
            close()
        }
        drawPath(leaf, tint)
    }
}

/** Settings cog: hub circle with radiating teeth — the design's gear button glyph. */
@Composable
fun GearIcon(modifier: Modifier = Modifier, tint: Color = AppColors.ink) {
    Canvas(modifier) {
        val unit = size.minDimension / GRID
        fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
        val strokeWidth = 2f * unit
        drawCircle(tint, radius = 3.2f * unit, center = p(12f, 12f), style = Stroke(strokeWidth))
        // Eight teeth: short spokes from the ring toward the rim, every 45°.
        val inner = 6.4f
        val outer = 9f
        for (i in 0 until 8) {
            val angle = Math.toRadians(i * 45.0)
            val cos = kotlin.math.cos(angle).toFloat()
            val sin = kotlin.math.sin(angle).toFloat()
            drawLine(
                tint,
                p(12f + inner * cos, 12f + inner * sin),
                p(12f + outer * cos, 12f + outer * sin),
                strokeWidth,
                StrokeCap.Round,
            )
        }
    }
}
