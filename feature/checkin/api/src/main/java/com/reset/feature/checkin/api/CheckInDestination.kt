package com.reset.feature.checkin.api

import com.reset.navigation.Screen
import kotlinx.serialization.Serializable

/**
 * The Check In feature's navigable destination: chip grid → optional follow-up → offer.
 * Living in the feature's `api` submodule, it is the only thing another feature (Home's
 * check-in entry card) needs to navigate here; the implementation stays in the sibling impl
 * module. The type itself is the route (Navigation 2.8 type-safe DSL); it takes no args —
 * the flow always starts at the chip grid.
 */
@Serializable
data object CheckInDestination : Screen
