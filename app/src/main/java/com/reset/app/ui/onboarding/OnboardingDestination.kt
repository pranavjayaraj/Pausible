package com.reset.app.ui.onboarding

import com.reset.navigation.Screen
import kotlinx.serialization.Serializable

/**
 * The app-owned onboarding route (brand splash → sign-in). It lives in `:app`, not a
 * feature api module, because only the host ever navigates here: it is pushed over the
 * Home tab on a fresh launch and popped when onboarding finishes, so no feature needs a
 * contract for it.
 */
@Serializable
data object OnboardingDestination : Screen
