package com.reset.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The app's cream base surface. Screens draw edge-to-edge on top of it (each surface owns
 * its background + status-bar inset so full-bleed screens like Focus or the splash can
 * colour the whole window). Shared so every feature renders on the same backdrop.
 */
@Composable
fun AppBackground(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(AppColors.surface)) {
        content()
    }
}
