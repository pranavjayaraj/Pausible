package com.reset.repository.data

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import com.reset.model.domain.SoundController
import com.reset.model.domain.model.ChimeKind
import com.reset.model.domain.preferences.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Minimal stand-in for the design's synthesized singing-bowl chimes. Plays a soft,
 * low-volume system tone so the audio wiring is real and audible; intentionally
 * lighter than the reference Web-Audio synthesis and easy to replace later.
 *
 * Tone construction/playback runs off the main thread: [ToneGenerator] init can block
 * for a second or more on some devices, and this is called from the UI side-effect
 * handler — blocking there would stall in-flight animations (e.g. the leave spin).
 */
class AndroidSoundController @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : SoundController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun playChime(kind: ChimeKind) {
        scope.launch {
            // Every chime funnels through here, so the Settings "Sounds & haptics"
            // switch is honoured in one place rather than at each call site.
            if (!preferencesRepository.preferences.first().soundsEnabled) return@launch
            runCatching {
                val tone = when (kind) {
                    ChimeKind.Start -> ToneGenerator.TONE_PROP_BEEP
                    ChimeKind.End -> ToneGenerator.TONE_PROP_ACK
                }
                // Construct per-call and release after the tone so we never hold the
                // single global ToneGenerator slot open between chimes. Let the tone
                // finish before releasing, otherwise release() cuts it short.
                val generator = ToneGenerator(AudioManager.STREAM_MUSIC, CHIME_VOLUME)
                generator.startTone(tone, CHIME_DURATION_MS)
                delay(CHIME_DURATION_MS.toLong() + RELEASE_GRACE_MS)
                generator.release()
            }.onFailure { Log.w(TAG, "Unable to play chime $kind", it) }
        }
    }

    private companion object {
        const val TAG = "HomeSoundController"
        const val CHIME_VOLUME = 40 // 0..100, kept gentle
        const val CHIME_DURATION_MS = 300
        const val RELEASE_GRACE_MS = 50L
    }
}
