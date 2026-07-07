package com.reset.feature.builder.navigation

/** All user/UI intents for the Builder feature. */
sealed interface BuilderIntent {
    data class SelectIntentTag(val key: String) : BuilderIntent
    data class SelectDuration(val minutes: Int) : BuilderIntent
    data class ToggleSound(val key: String) : BuilderIntent
    data class ChangeMix(val percent: Int) : BuilderIntent
    data class SelectPace(val seconds: Int) : BuilderIntent
    data class SelectBell(val minutes: Int) : BuilderIntent
    data object ToggleGuidedVoice : BuilderIntent
    data object ToggleWarmup : BuilderIntent
    data object ToggleGong : BuilderIntent
    data class SelectReminder(val key: String) : BuilderIntent
    data class SetSessionName(val name: String) : BuilderIntent
    data object SavePreset : BuilderIntent
    data class ApplyPreset(val name: String) : BuilderIntent
    data object ApplySuggestion : BuilderIntent
    data object BeginCustomSession : BuilderIntent
    data object HandleBackPress : BuilderIntent
}
