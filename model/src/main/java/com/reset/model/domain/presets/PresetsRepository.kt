package com.reset.model.domain.presets

import com.reset.model.domain.model.SessionPreset
import kotlinx.coroutines.flow.Flow

/** Persistence boundary for session-builder presets. */
interface PresetsRepository {

    /** Saved session-builder presets, most recent last. */
    val presets: Flow<List<SessionPreset>>

    /** Saves (or replaces, by name) a builder preset, keeping the most recent
     *  [SessionPreset.MAX_PRESETS]. */
    suspend fun savePreset(preset: SessionPreset)
}
