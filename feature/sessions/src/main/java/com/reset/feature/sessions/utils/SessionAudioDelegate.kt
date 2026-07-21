package com.reset.feature.sessions.utils

import android.media.AudioManager
import android.media.ToneGenerator
import com.reset.core.dispatcher.DispatcherProvider
import com.reset.model.domain.SoundController
import com.reset.model.domain.model.ChimeKind
import com.reset.model.domain.preferences.PreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Eyes-closed session audio: bookend chimes (reusing [SoundController], same as the Focus
 * sit) plus a soft procedural tick at each breath-phase boundary — the pivot's audio
 * delivery mechanism. No bundled media assets, per the catalog's constraints.
 */
interface SessionAudioDelegate {
    suspend fun playArrivalChime()
    suspend fun playLandingChime()
    suspend fun playBreathTick()
}

class SessionAudioDelegateImpl @Inject constructor(
    private val soundController: SoundController,
    private val preferencesRepository: PreferencesRepository,
    private val dispatcherProvider: DispatcherProvider,
) : SessionAudioDelegate {

    // Lazy + best-effort: ToneGenerator init can block briefly and some devices lack an
    // audio output track for it; a session should never fail to run because of a tick.
    private val toneGenerator by lazy {
        runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, TICK_VOLUME) }.getOrNull()
    }

    override suspend fun playArrivalChime() = withContext(dispatcherProvider.io) {
        soundController.playChime(ChimeKind.Start)
    }

    override suspend fun playLandingChime() = withContext(dispatcherProvider.io) {
        soundController.playChime(ChimeKind.End)
    }

    override suspend fun playBreathTick() = withContext(dispatcherProvider.io) {
        if (!preferencesRepository.preferences.first().soundsEnabled) return@withContext
        runCatching { toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, TICK_DURATION_MS) }
        Unit
    }

    private companion object {
        const val TICK_VOLUME = 30
        const val TICK_DURATION_MS = 60
    }
}
