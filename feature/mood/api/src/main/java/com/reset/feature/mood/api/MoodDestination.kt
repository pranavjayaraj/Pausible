package com.reset.feature.mood.api

import com.reset.navigation.Screen
import kotlinx.serialization.Serializable

/**
 * The Mood feature's navigable destination — the full-screen "Mood Log" shown after a
 * session so the user can log how the sit left them feeling. Living in the feature's `api`
 * submodule, it is the only thing another feature needs to navigate here (the Sessions
 * feature opens it when a focus countdown completes); the implementation (screen, ViewModel)
 * stays in the sibling impl module, so churn there never recompiles callers. The type itself
 * is the route (Navigation 2.8 type-safe DSL); it takes no args today.
 */
@Serializable
data object MoodDestination : Screen
