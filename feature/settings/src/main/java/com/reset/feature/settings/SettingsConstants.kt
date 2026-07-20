package com.reset.feature.settings

import androidx.compose.ui.unit.dp

/** Dimension and bound constants for the Settings page. No magic values in UI. */
object SettingsConstants {

    /** Hours offered by the quiet-hours picker (24h clock, 0..23). */
    const val HOURS_PER_DAY = 24

    /** Quiet-hours bound rows fade to this when the master switch is off. */
    const val DISABLED_ROW_ALPHA = 0.4f

    // Header
    val headerPaddingTop = 30.dp
    val headerPaddingBottom = 14.dp
    val headerGap = 14.dp
    val backButtonSize = 38.dp
    val backIconSize = 18.dp

    // Content column
    val contentPaddingTop = 8.dp
    val contentPaddingBottom = 40.dp
    val sectionFirstSpacing = 14.dp
    val sectionSpacing = 26.dp
    val sectionLabelSpacing = 10.dp
    val sectionLabelInset = 4.dp

    // Cards and rows
    val cardPaddingH = 18.dp
    val cardPaddingV = 4.dp
    val rowPaddingV = 16.dp
    val rowGap = 14.dp
    val dividerWidth = 1.5.dp
    val iconBoxSize = 42.dp
    val iconBoxRadius = 12.dp
    val iconSize = 22.dp

    // Pill toggle (50x29 with a 23dp knob, like the design)
    val toggleWidth = 50.dp
    val toggleHeight = 29.dp
    val toggleKnob = 23.dp
    val togglePadding = 3.dp

    // Quiet-hours time chip
    val timeChipRadius = 12.dp
    val timeChipBorder = 1.5.dp
    val timeChipPaddingH = 12.dp
    val timeChipPaddingV = 8.dp

    // Footer summary under the quiet-hours card
    val summarySpacing = 10.dp
    val summaryInset = 6.dp

    // Hour-picker dialog
    val pickerMaxHeight = 380.dp
    val pickerPaddingV = 14.dp
    val pickerTitlePaddingH = 22.dp
    val pickerTitleSpacing = 8.dp
    val pickerRowPaddingH = 22.dp
    val pickerRowPaddingV = 11.dp
}
