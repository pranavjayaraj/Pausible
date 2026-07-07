package com.reset.feature.profile.api

import com.reset.navigation.Screen
import kotlinx.serialization.Serializable

/**
 * The Profile feature's navigable destination — the "Weekly Progress" stats tab. Living in
 * the feature's `api` submodule, it is the only thing another feature needs to navigate
 * here — implementations (screens, ViewModels) stay in the sibling impl module, so
 * destination churn there never recompiles callers. The type itself is the route
 * (Navigation 2.8 type-safe DSL); args, when a destination gains them, are constructor
 * properties on a `@Serializable data class`.
 */
@Serializable
data object ProfileDestination : Screen
