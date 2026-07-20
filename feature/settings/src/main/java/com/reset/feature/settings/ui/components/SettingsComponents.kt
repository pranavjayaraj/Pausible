package com.reset.feature.settings.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppShapes
import com.reset.core.designsystem.AppType
import com.reset.feature.settings.SettingsConstants
import com.reset.feature.settings.ui.SettingsTone
import com.reset.feature.settings.utils.SettingsUtils

// ── Card scaffolding ──────────────────────────────────────────

/** White rounded card that stacks [content] rows; put a [SettingsRowDivider] between rows. */
@Composable
fun SettingsCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(AppColors.surfaceWhite)
            .padding(
                horizontal = SettingsConstants.cardPaddingH,
                vertical = SettingsConstants.cardPaddingV,
            ),
    ) {
        content()
    }
}

/** Hairline between two rows of a [SettingsCard]. */
@Composable
fun SettingsRowDivider() {
    HorizontalDivider(thickness = SettingsConstants.dividerWidth, color = SettingsTone.divider)
}

/**
 * One card row from the design: tinted icon box, title (optional subtitle), and a trailing
 * control. [enabled] fades the whole row while its control stays laid out.
 */
@Composable
fun SettingsRow(
    icon: @Composable () -> Unit,
    iconBox: Color,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else SettingsConstants.DISABLED_ROW_ALPHA)
            .padding(vertical = SettingsConstants.rowPaddingV),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(SettingsConstants.iconBoxSize)
                .clip(RoundedCornerShape(SettingsConstants.iconBoxRadius))
                .background(iconBox),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = SettingsConstants.rowGap),
        ) {
            Text(text = title, style = AppType.heroPrompt, color = AppColors.ink)
            if (subtitle != null) {
                Text(text = subtitle, style = AppType.suggestSub, color = AppColors.inkMuted)
            }
        }
        trailing()
    }
}

// ── Controls ──────────────────────────────────────────────────

/** The design's 50x29 pill toggle: sliding white knob over accent (on) / warm grey (off). */
@Composable
fun SettingsToggle(
    checked: Boolean,
    onToggle: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(width = SettingsConstants.toggleWidth, height = SettingsConstants.toggleHeight)
            .clip(AppShapes.pill)
            .background(if (checked) AppColors.accent else SettingsTone.toggleOff)
            .clickable(onClick = onToggle)
            .semantics { this.contentDescription = contentDescription }
            .padding(SettingsConstants.togglePadding),
    ) {
        Box(
            Modifier
                .size(SettingsConstants.toggleKnob)
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .clip(CircleShape)
                .background(AppColors.surfaceWhite),
        )
    }
}

/** Bordered chip showing a quiet-hours bound ("10:00 PM"); tap opens the hour picker. */
@Composable
fun TimeChip(
    hour: Int,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(SettingsConstants.timeChipRadius))
            .background(AppColors.surfaceBreak)
            .border(
                width = SettingsConstants.timeChipBorder,
                color = SettingsTone.chipBorder,
                shape = RoundedCornerShape(SettingsConstants.timeChipRadius),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = SettingsConstants.timeChipPaddingH,
                vertical = SettingsConstants.timeChipPaddingV,
            ),
    ) {
        Text(
            text = SettingsUtils.formatHour(hour),
            style = AppType.button,
            color = AppColors.ink,
        )
    }
}

/** Simple hour list the time chips open; tapping a row selects and closes. */
@Composable
fun HourPickerDialog(
    title: String,
    selectedHour: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(AppShapes.card)
                .background(AppColors.surfaceWhite)
                .padding(vertical = SettingsConstants.pickerPaddingV),
        ) {
            Text(
                text = title,
                style = AppType.sectionTitle,
                color = AppColors.ink,
                modifier = Modifier.padding(
                    horizontal = SettingsConstants.pickerTitlePaddingH,
                ),
            )
            LazyColumn(
                state = rememberLazyListState(initialFirstVisibleItemIndex = selectedHour),
                modifier = Modifier
                    .heightIn(max = SettingsConstants.pickerMaxHeight)
                    .padding(top = SettingsConstants.pickerTitleSpacing),
            ) {
                items(count = SettingsConstants.HOURS_PER_DAY) { hour ->
                    val selected = hour == selectedHour
                    Text(
                        text = SettingsUtils.formatHour(hour),
                        style = AppType.button,
                        color = if (selected) AppColors.accent else AppColors.ink,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(hour) }
                            .padding(
                                horizontal = SettingsConstants.pickerRowPaddingH,
                                vertical = SettingsConstants.pickerRowPaddingV,
                            ),
                    )
                }
            }
        }
    }
}

// ── Icons (24x24 grid, mirroring the design's inline SVGs) ────

private const val GRID = 24f

/** Crescent moon — the quiet-hours switch row. */
@Composable
fun MoonIcon(modifier: Modifier = Modifier, tint: Color = AppColors.teal) {
    Canvas(modifier) {
        val unit = size.minDimension / GRID
        // M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z — a big disc minus an offset disc.
        val disc = Path().apply {
            addOval(Rect(center = Offset(11.5f * unit, 12.5f * unit), radius = 9f * unit))
        }
        val bite = Path().apply {
            addOval(Rect(center = Offset(17f * unit, 7f * unit), radius = 8f * unit))
        }
        val crescent = Path.combine(PathOperation.Difference, disc, bite)
        drawPath(crescent, tint, style = Stroke(2f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/**
 * Clock face with hands; [handsForward] mirrors the design's two variants — hands pointing
 * right for "Starts" (7v5l3 2) and left for "Ends" (7v5l-3 2).
 */
@Composable
fun QuietClockIcon(
    handsForward: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = AppColors.accentDark,
) {
    Canvas(modifier) {
        val unit = size.minDimension / GRID
        fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
        val strokeWidth = 2f * unit
        drawCircle(tint, radius = 9f * unit, center = p(12f, 12f), style = Stroke(strokeWidth))
        val hands = Path().apply {
            moveTo(p(12f, 7f).x, p(12f, 7f).y)
            lineTo(p(12f, 12f).x, p(12f, 12f).y)
            val tipX = if (handsForward) 15f else 9f
            lineTo(p(tipX, 14f).x, p(tipX, 14f).y)
        }
        drawPath(hands, tint, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Notification bell — the break-reminders row. */
@Composable
fun BellIcon(modifier: Modifier = Modifier, tint: Color = SettingsTone.bellInk) {
    Canvas(modifier) {
        val unit = size.minDimension / GRID
        fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
        val stroke = Stroke(2f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
        // M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9
        val body = Path().apply {
            moveTo(p(18f, 8f).x, p(18f, 8f).y)
            cubicTo(p(18f, 4.69f).x, p(18f, 4.69f).y, p(15.31f, 2f).x, p(15.31f, 2f).y, p(12f, 2f).x, p(12f, 2f).y)
            cubicTo(p(8.69f, 2f).x, p(8.69f, 2f).y, p(6f, 4.69f).x, p(6f, 4.69f).y, p(6f, 8f).x, p(6f, 8f).y)
            cubicTo(p(6f, 15f).x, p(6f, 15f).y, p(3f, 17f).x, p(3f, 17f).y, p(3f, 17f).x, p(3f, 17f).y)
            lineTo(p(21f, 17f).x, p(21f, 17f).y)
            cubicTo(p(21f, 17f).x, p(21f, 17f).y, p(18f, 15f).x, p(18f, 15f).y, p(18f, 8f).x, p(18f, 8f).y)
        }
        drawPath(body, tint, style = stroke)
        // Clapper: M13.7 21a2 2 0 0 1-3.4 0
        val clapper = Path().apply {
            moveTo(p(13.7f, 20.6f).x, p(13.7f, 20.6f).y)
            cubicTo(p(13.2f, 21.6f).x, p(13.2f, 21.6f).y, p(10.8f, 21.6f).x, p(10.8f, 21.6f).y, p(10.3f, 20.6f).x, p(10.3f, 20.6f).y)
        }
        drawPath(clapper, tint, style = stroke)
    }
}

/** Three wind-swirl lines — the sounds & haptics row. */
@Composable
fun SoundWavesIcon(modifier: Modifier = Modifier, tint: Color = SettingsTone.soundInk) {
    Canvas(modifier) {
        val unit = size.minDimension / GRID
        fun p(x: Float, y: Float) = Offset(x * unit, y * unit)
        val strokeWidth = 2f * unit
        // M3 8h9a2.5 2.5 / M3 12h16a2.5 2.5 / M3 16h13a2.5 2.5 — lines ending in loops.
        fun swirl(endX: Float, y: Float, loopUp: Boolean) {
            drawLine(tint, p(3f, y), p(endX, y), strokeWidth, StrokeCap.Round)
            val loopCenterY = if (loopUp) y - 2.5f else y + 2.5f
            drawCircle(
                tint,
                radius = 2.5f * unit,
                center = p(endX, loopCenterY),
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )
        }
        swirl(endX = 12f, y = 8f, loopUp = true)
        swirl(endX = 19f, y = 12f, loopUp = true)
        swirl(endX = 16f, y = 16f, loopUp = false)
    }
}
