package com.reset.feature.profile.navigation

/**
 * Feature-local one-shot effects for Profile. The tab is read-only today, so there are no
 * cases yet; the seam stays so effects can be added without reshaping the ViewModel.
 * Navigation (including closing) is NOT here — it goes through the injected
 * [com.reset.navigation.Navigator].
 */
sealed interface ProfileSideEffect
