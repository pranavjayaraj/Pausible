package com.reset.feature.home

import androidx.compose.ui.unit.dp

/** All timing, dimension, and analytics constants for the Home feature. */
object HomeConstants {

    // ── Timing ────────────────────────────────────────────────
    /** How long the "Nice breather!" banner stays up. */
    const val CELEBRATION_VISIBLE_MS = 3_200L

    /** Number of entries in the `home_break_facts` string array. */
    const val FACT_COUNT = 10

    // ── Dimens ────────────────────────────────────────────────
    val sectionSpacing = 22.dp
    val cardSpacing = 14.dp
    val gridSpacing = 10.dp
    val heroIconSize = 44.dp
    val heroMascotSize = 30.dp
    val tipMascotSize = 34.dp
    val bannerMascotSize = 46.dp
}
