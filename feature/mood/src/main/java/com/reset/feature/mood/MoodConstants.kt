package com.reset.feature.mood

import androidx.compose.ui.unit.dp

/** Scale bounds and dimension constants for the Mood Log. No magic values in UI. */
object MoodConstants {

    /** Inclusive slider bounds; mirrors the design's 0–100 range. */
    const val LEVEL_MIN = 0
    const val LEVEL_MAX = 100

    /** Band thresholds: below [ROUGH_MAX] is Rough, below [OKAY_MAX] is Okay, else Great. */
    const val ROUGH_MAX = 33
    const val OKAY_MAX = 66

    // Layout
    val titleSpacing = 8.dp
    val subtitleSpacing = 6.dp
    val faceTopSpacing = 44.dp
    val faceSize = 240.dp
    val sliderTopSpacing = 52.dp
    val sliderWidthMax = 360.dp
    val sliderTrackHeight = 8.dp
    val sliderThumbSize = 32.dp
    val sliderThumbBorder = 2.dp
    val sliderThumbDot = 8.dp
    val scaleLabelSpacing = 14.dp
    val actionsTopSpacing = 48.dp
    val saveButtonHeight = 56.dp
    val saveIconSize = 20.dp
    val saveIconSpacing = 8.dp
    val skipSpacing = 8.dp
    val closeButtonSize = 40.dp
    val closeIconSize = 22.dp
    val closeButtonInset = 8.dp
}
