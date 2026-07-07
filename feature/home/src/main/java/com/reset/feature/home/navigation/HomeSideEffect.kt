package com.reset.feature.home.navigation

/**
 * Feature-local one-shot effects for Home. Currently empty — navigation goes through the
 * injected [com.reset.navigation.Navigator] and the session chimes live in the Sessions
 * feature — but the seam stays so effects can be added without re-plumbing the route.
 */
sealed class HomeSideEffect
