package com.reset.feature.checkin.navigation

/** Feature-local one-shot effects for Check In. Navigation goes through the injected
 *  [com.reset.navigation.Navigator]; these are transient UI cues only. */
sealed class CheckInSideEffect {
    /** Typed text didn't clear the confidence floor — nudge the user toward the chips. */
    data object UnrecognizedText : CheckInSideEffect()

    /** Voice mode isn't wired yet. */
    data object VoiceComingSoon : CheckInSideEffect()

    /** Play wants explicit consent before spending cellular data on the embedding model
     *  download — the Route resolves this by calling
     *  `EmbedModelManager.confirmCellularDownload(activity)` with its own transient Activity,
     *  never the ViewModel (leak-safe activity-launching rule). */
    data object RequestCellularConfirmation : CheckInSideEffect()
}
