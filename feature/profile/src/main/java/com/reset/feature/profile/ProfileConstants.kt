package com.reset.feature.profile

import androidx.compose.ui.unit.dp

/** Dimension/chart constants for the Profile feature. No magic values in UI. */
object ProfileConstants {

    val titleTopSpacing = 10.dp
    val subtitleSpacing = 6.dp
    val gearButtonSize = 40.dp
    val gearIconSize = 20.dp
    val gearButtonBorder = 1.5.dp
    val firstCardSpacing = 18.dp
    val cardSpacing = 14.dp
    val statCardPaddingH = 20.dp
    val statCardPaddingV = 18.dp
    val statValueSpacing = 6.dp
    val chipRowSpacing = 10.dp
    val chipGap = 8.dp
    val streakHintSpacing = 4.dp
    val streakMascotSize = 88.dp
    /** The mascot peeks past the card's bottom edge, like the design. */
    val streakMascotOverhang = 8.dp

    val chartTitleSpacing = 24.dp
    val chartTopSpacing = 16.dp
    /** Tallest possible bar; every bar is a fraction of this. */
    val chartBarAreaHeight = 104.dp
    val chartBarGap = 12.dp
    val chartLabelSpacing = 6.dp
    val chartPaddingH = 6.dp

    /** Bars for zero-minute days keep a visible stub, like the design's shortest bar. */
    const val CHART_MIN_BAR_FRACTION = 0.12f
}
