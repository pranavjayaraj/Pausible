package com.reset.feature.builder

import androidx.compose.runtime.Stable
import com.reset.model.domain.model.SessionPreset

/** Everything the "Craft your session" screen edits. */
@Stable
data class BuilderState(
    val intentKey: String = BuilderConstants.INTENT_FOCUS,
    val durationMin: Int = BuilderConstants.DEFAULT_CUSTOM_MIN,
    val soundKey: String = BuilderConstants.SOUND_RAIN,
    val layerSoundKey: String? = null,
    val mixPercent: Int = SessionPreset.DEFAULT_MIX_PERCENT,
    val paceSec: Int = BuilderConstants.PACE_MEDIUM_SEC,
    val bellMin: Int = BuilderConstants.BELL_OFF,
    val guidedVoice: Boolean = true,
    val warmup: Boolean = true,
    val gong: Boolean = true,
    val remindKey: String = BuilderConstants.REMIND_OFF,
    val sessionName: String = "",
    val presets: List<SessionPreset> = emptyList(),
) {
    companion object {
        fun getDefault() = BuilderState()
    }
}

/** Represents a dynamically generated builder suggestion based on time of day. */
data class BuilderSuggestion(
    val intentKey: String,
    val durationMin: Int,
    val soundKey: String,
    val paceSec: Int,
)
