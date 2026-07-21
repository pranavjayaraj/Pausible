package com.reset.feature.checkin.navigation

/**
 * Feature-local one-shot effects for Check In. Currently empty — navigation goes through the
 * injected [com.reset.navigation.Navigator] — but the seam stays so effects (a chime, a
 * haptic tick) can be added without re-plumbing the route.
 */
sealed class CheckInSideEffect
