package com.reset.feature.settings.navigation

/**
 * Feature-local one-shot effects for Settings. Navigation (leaving via back) is NOT here —
 * it goes through the injected [com.reset.navigation.Navigator]. The page currently has no
 * one-shot effects; the type keeps the BaseViewModel contract and a home for future ones.
 */
sealed class SettingsSideEffect
