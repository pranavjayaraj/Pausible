package com.reset.feature.mood.ui

import androidx.compose.ui.graphics.Color
import com.reset.feature.mood.MoodConstants

/**
 * The three mood bands the slider maps to, each carrying the exact palette from the design.
 * Colours live here (not the shared [com.reset.core.designsystem.AppColors]) because they
 * are specific to this one screen's mood-reactive backdrop and face.
 *
 * @param backgroundTop   top of the full-screen gradient
 * @param backgroundBottom bottom of the full-screen gradient
 * @param face            the blob's fill
 * @param accent          eyes, mouth, slider thumb and the save-button label
 */
enum class MoodTone(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val face: Color,
    val accent: Color,
) {
    Rough(
        backgroundTop = Color(0xFFE87A4F),
        backgroundBottom = Color(0xFFF4A261),
        face = Color(0xFFFDF5E6),
        accent = Color(0xFFE87A4F),
    ),
    Okay(
        backgroundTop = Color(0xFFFF9C45),
        backgroundBottom = Color(0xFFFFB77F),
        face = Color(0xFFFFFFFF),
        accent = Color(0xFFFF9C45),
    ),
    Great(
        backgroundTop = Color(0xFFF4BF3A),
        backgroundBottom = Color(0xFFFEC742),
        face = Color(0xFFFFFFFF),
        accent = Color(0xFFF4BF3A),
    );

    companion object {
        /** The band a 0–100 slider [level] falls into, matching the design's thresholds. */
        fun fromLevel(level: Int): MoodTone = when {
            level < MoodConstants.ROUGH_MAX -> Rough
            level < MoodConstants.OKAY_MAX -> Okay
            else -> Great
        }
    }
}
