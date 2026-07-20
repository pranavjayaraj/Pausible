package com.reset.feature.settings.ui

import androidx.compose.ui.graphics.Color

/**
 * Colours specific to the Settings page, straight from the BloomNow design. They live here
 * (not the shared [com.reset.core.designsystem.AppColors]) because they only tint this one
 * screen's icon boxes, hairlines, and chips.
 */
object SettingsTone {
    /** Section eyebrows ("QUIET HOURS") and the summary line under the card. */
    val sectionLabel = Color(0xFFA59A86)

    /** Hairline between rows inside a card. */
    val divider = Color(0xFFF2EDE2)

    /** Border of the back button and the time chips. */
    val chipBorder = Color(0xFFE4DDCE)

    /** Pill-toggle track when off (on = the shared accent). */
    val toggleOff = Color(0xFFD8CFC0)

    // Icon boxes: soft tinted square behind each row icon, plus the icon's stroke colour
    // where it isn't already a shared token (moon = AppColors.teal, clock = accentDark).
    val moonBox = Color(0xFFEAF4F5)
    val clockBox = Color(0xFFFCEFE0)
    val bellBox = Color(0xFFE9F3E4)
    val bellInk = Color(0xFF4E8A3C)
    val soundBox = Color(0xFFF3E9F3)
    val soundInk = Color(0xFF8A5A22)
}
