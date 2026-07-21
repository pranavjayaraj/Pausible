package com.reset.feature.checkin.ui

import androidx.compose.ui.graphics.Color

/**
 * Check In-specific colours (night grid, the wound-up/worn-out and neck/wrists follow-up
 * icons, the empathy-toned offer variant). Not in the shared
 * [com.reset.core.designsystem.AppColors] because they're specific to this one feature's
 * frames.
 */
object CheckInTone {
    // Night chip grid (design 2d).
    val nightBackground = Color(0xFF123543)
    val nightChip = Color(0xFF1C4A5C)
    val nightChipSwapped = Color(0xFF16404F)
    val nightInk = Color(0xFFF2EEE3)
    val nightInkMuted = Color(0xFFA8C0C8)
    val nightSwapAccent = Color(0xFFE8C289)

    // Follow-up option cards (design 3a).
    val woundUpCard = Color(0xFFFCEFE6)
    val woundUpIcon = Color(0xFFC0552F)
    val wornOutCard = Color(0xFFE8F1F4)
    val wornOutIcon = Color(0xFF1F6F8B)

    // Offer, empathy variant (design 3c) — LOW_MOOD / DISCONNECTED.
    val empathyCard = Color(0xFFFCEFE6)
}
