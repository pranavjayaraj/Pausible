package com.reset.feature.settings.api

import com.reset.navigation.Screen
import kotlinx.serialization.Serializable

/**
 * The full-screen Settings destination — quiet hours and notification switches. Living in
 * the feature's `api` submodule, it is the only thing another feature needs to navigate
 * here (Home's gear button opens it); the implementation (screen, ViewModel) stays in the
 * sibling impl module, so churn there never recompiles callers. The type itself is the
 * route (Navigation 2.8 type-safe DSL); it takes no args today.
 */
@Serializable
data object SettingsDestination : Screen
