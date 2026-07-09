package com.reset.feature.mood.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.reset.core.designsystem.AppShapes
import com.reset.feature.mood.MoodConstants
import com.reset.feature.mood.ui.MoodTone
import kotlin.math.roundToInt

/** The design's SVG face is authored on a 200×200 grid; every point is scaled from it. */
private const val FACE_GRID = 200f
private const val EYE_STROKE = 4f
private const val MOUTH_STROKE = 5f
private const val COLOR_TWEEN_MS = 500

/**
 * The interactive mood blob — a Canvas port of the design's morphing SVG. The blob shape,
 * eyes and mouth switch between the three [MoodTone] faces while the fill and feature
 * colours cross-fade, mirroring the CSS transitions.
 */
@Composable
fun MoodFace(tone: MoodTone, modifier: Modifier = Modifier) {
    val faceColor by animateColorAsState(tone.face, tween(COLOR_TWEEN_MS), label = "face")
    val accentColor by animateColorAsState(tone.accent, tween(COLOR_TWEEN_MS), label = "accent")

    Canvas(modifier) {
        val unit = size.minDimension / FACE_GRID
        val p = { x: Float, y: Float -> Offset(x * unit, y * unit) }

        drawPath(blobPath(tone, p), faceColor)

        val eyeStroke = Stroke(EYE_STROKE * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val mouthStroke = Stroke(MOUTH_STROKE * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val (eyeLeft, eyeRight, mouth) = facePaths(tone, p)
        drawPath(eyeLeft, accentColor, style = eyeStroke)
        drawPath(eyeRight, accentColor, style = eyeStroke)
        drawPath(mouth, accentColor, style = mouthStroke)
    }
}

/**
 * The mood slider — a custom track/thumb matching the design: a translucent white rail with
 * a solid white fill, and a white knob ringed in the current [accent] with an accent dot.
 * Tap or drag anywhere along it to set the 0–100 [level].
 */
@Composable
fun MoodSlider(
    level: Int,
    onLevelChange: (Int) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val thumbPx = with(density) { MoodConstants.sliderThumbSize.toPx() }
    val span = (MoodConstants.LEVEL_MAX - MoodConstants.LEVEL_MIN).toFloat()

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(MoodConstants.sliderThumbSize),
        contentAlignment = Alignment.CenterStart,
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        // The thumb centre travels between half-a-thumb from each edge, so it never clips.
        val travel = (widthPx - thumbPx).coerceAtLeast(1f)
        val levelFor = { x: Float ->
            val fraction = ((x - thumbPx / 2f) / travel).coerceIn(0f, 1f)
            (MoodConstants.LEVEL_MIN + fraction * span).roundToInt()
        }

        val fraction = ((level - MoodConstants.LEVEL_MIN) / span).coerceIn(0f, 1f)
        val thumbCenter = thumbPx / 2f + fraction * travel

        // Inactive rail.
        Box(
            Modifier
                .fillMaxWidth()
                .height(MoodConstants.sliderTrackHeight)
                .clip(AppShapes.pill)
                .background(Color.White.copy(alpha = 0.3f)),
        )
        // Active fill up to the thumb.
        Box(
            Modifier
                .width(with(density) { thumbCenter.toDp() })
                .height(MoodConstants.sliderTrackHeight)
                .clip(AppShapes.pill)
                .background(Color.White),
        )
        // Thumb.
        Box(
            Modifier
                .offset { IntOffset((thumbCenter - thumbPx / 2f).roundToInt(), 0) }
                .size(MoodConstants.sliderThumbSize)
                .clip(CircleShape)
                .background(Color.White)
                .border(MoodConstants.sliderThumbBorder, accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(MoodConstants.sliderThumbDot)
                    .clip(CircleShape)
                    .background(accent),
            )
        }
        // Transparent gesture layer on top: tap-to-set and drag-to-scrub.
        Box(
            Modifier
                .matchParentSize()
                .pointerInput(widthPx, thumbPx) {
                    detectTapGestures { onLevelChange(levelFor(it.x)) }
                }
                .pointerInput(widthPx, thumbPx) {
                    detectHorizontalDragGestures { change, _ ->
                        onLevelChange(levelFor(change.position.x))
                    }
                },
        )
    }
}

/** Small filled heart for the "Save Mood" button, tinted to the current mood [tint]. */
@Composable
fun HeartIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val unit = size.minDimension / 24f
        val p = { x: Float, y: Float -> Offset(x * unit, y * unit) }
        val heart = Path().apply {
            moveTo(p, 12f, 6.6f)
            cubicTo(p, 10.4f, 4f, 6.4f, 3.9f, 5f, 6.4f)
            cubicTo(p, 3.3f, 9.1f, 5.3f, 12.6f, 12f, 18.5f)
            cubicTo(p, 18.7f, 12.6f, 20.7f, 9.1f, 19f, 6.4f)
            cubicTo(p, 17.6f, 3.9f, 13.6f, 4f, 12f, 6.6f)
            close()
        }
        drawPath(heart, tint)
    }
}

// ── Path builders ─────────────────────────────────────────────

private fun Path.moveTo(p: (Float, Float) -> Offset, x: Float, y: Float) {
    val o = p(x, y)
    moveTo(o.x, o.y)
}

private fun Path.cubicTo(
    p: (Float, Float) -> Offset,
    x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float,
) {
    val a = p(x1, y1); val b = p(x2, y2); val c = p(x3, y3)
    cubicTo(a.x, a.y, b.x, b.y, c.x, c.y)
}

private fun Path.quadTo(p: (Float, Float) -> Offset, cx: Float, cy: Float, ex: Float, ey: Float) {
    val a = p(cx, cy); val b = p(ex, ey)
    quadraticTo(a.x, a.y, b.x, b.y)
}

/** The rounded blob outline for each mood, from the design's SVG paths. */
private fun blobPath(tone: MoodTone, p: (Float, Float) -> Offset): Path = Path().apply {
    when (tone) {
        MoodTone.Rough -> {
            moveTo(p, 45f, 110f)
            cubicTo(p, 35f, 60f, 70f, 30f, 100f, 45f)
            cubicTo(p, 130f, 30f, 165f, 60f, 155f, 110f)
            cubicTo(p, 145f, 160f, 120f, 165f, 100f, 155f)
            cubicTo(p, 80f, 165f, 55f, 160f, 45f, 110f)
        }
        MoodTone.Okay -> {
            moveTo(p, 40f, 100f)
            cubicTo(p, 40f, 60f, 60f, 40f, 100f, 40f)
            cubicTo(p, 140f, 40f, 160f, 60f, 160f, 100f)
            cubicTo(p, 160f, 140f, 140f, 160f, 100f, 160f)
            cubicTo(p, 60f, 160f, 40f, 140f, 40f, 100f)
        }
        MoodTone.Great -> {
            moveTo(p, 30f, 100f)
            cubicTo(p, 20f, 40f, 70f, 20f, 100f, 30f)
            cubicTo(p, 130f, 20f, 180f, 40f, 170f, 100f)
            cubicTo(p, 160f, 160f, 130f, 175f, 100f, 170f)
            cubicTo(p, 70f, 175f, 40f, 160f, 30f, 100f)
        }
    }
    close()
}

/** The eyes (left, right) and mouth strokes for each mood face. */
private fun facePaths(tone: MoodTone, p: (Float, Float) -> Offset): Triple<Path, Path, Path> =
    when (tone) {
        MoodTone.Rough -> Triple(
            Path().apply { moveTo(p, 70f, 85f); quadTo(p, 75f, 90f, 85f, 85f) },
            Path().apply { moveTo(p, 115f, 85f); quadTo(p, 125f, 90f, 130f, 85f) },
            Path().apply { moveTo(p, 75f, 125f); quadTo(p, 100f, 105f, 125f, 125f) },
        )
        MoodTone.Okay -> Triple(
            Path().apply { moveTo(p, 75f, 90f); quadTo(p, 80f, 85f, 85f, 90f) },
            Path().apply { moveTo(p, 115f, 90f); quadTo(p, 120f, 85f, 125f, 90f) },
            Path().apply { moveTo(p, 80f, 115f); quadTo(p, 100f, 110f, 120f, 115f) },
        )
        MoodTone.Great -> Triple(
            Path().apply { moveTo(p, 70f, 95f); quadTo(p, 80f, 75f, 90f, 95f) },
            Path().apply { moveTo(p, 110f, 95f); quadTo(p, 120f, 75f, 130f, 95f) },
            Path().apply { moveTo(p, 70f, 110f); quadTo(p, 100f, 140f, 130f, 110f) },
        )
    }
