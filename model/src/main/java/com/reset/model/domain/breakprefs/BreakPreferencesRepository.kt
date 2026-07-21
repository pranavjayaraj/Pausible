package com.reset.model.domain.breakprefs

import com.reset.model.domain.checkin.NeedState
import kotlinx.coroutines.flow.Flow

/**
 * Environment context the session selector's requirement gates check against (headphones,
 * stairs, room to move, running water nearby), plus a decaying per-[NeedState] echo of check-in
 * choices. The echo is captured now — it can't be back-filled later — as a gentle standing
 * signal meant to eventually inform the notification path's own priors without hijacking
 * them; nothing reads it yet, so the `sense-*` modules stay untouched in this phase.
 */
data class BreakPreferences(
    val audioAvailable: Boolean = true,
    val stairsAvailable: Boolean = false,
    val moveSpaceAvailable: Boolean = true,
    val waterAccessAvailable: Boolean = true,
    /** Decaying weight per [NeedState] — see [BreakPreferencesRepository.recordNeedStateEcho]. */
    val needStateEcho: Map<NeedState, Float> = emptyMap(),
)

/** Persistence boundary for [BreakPreferences]. */
interface BreakPreferencesRepository {

    val preferences: Flow<BreakPreferences>

    suspend fun setAudioAvailable(available: Boolean)
    suspend fun setStairsAvailable(available: Boolean)
    suspend fun setMoveSpaceAvailable(available: Boolean)
    suspend fun setWaterAccessAvailable(available: Boolean)

    /** Decays every existing echo weight, then bumps [needState]'s — acute, explicit
     *  check-in choices leaving a fading trace rather than a single hard override. */
    suspend fun recordNeedStateEcho(needState: NeedState)
}
