package com.reset.feature.checkin

import androidx.compose.ui.unit.dp

/** Timing, lookback, and dimension constants for the Check In feature. */
object CheckInConstants {

    /** How far back to pull completed sessions for the selector's context: wide enough to
     *  cover both the one-hour freshness tiebreak and any script's own, longer self-imposed
     *  frequency cap (currently The Plunge's 4 hours). */
    const val RECENT_COMPLETIONS_LOOKBACK_MS = 24 * 60 * 60 * 1000L

    /** Minimum "Finding your minute…" dwell so selection reads as considered, not instant. */
    const val FINDING_MIN_DWELL_MS = 900L

    // ── Dimens ────────────────────────────────────────────────
    val chipHeight = 78.dp
    val chipGap = 13.dp
    val chipRadius = 22.dp
    val followUpCardHeight = 150.dp
    val sectionSpacing = 22.dp
}
