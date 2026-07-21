package com.reset.feature.sessions.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.reset.core.dispatcher.DispatcherProvider
import com.reset.feature.sessions.content.BreathPattern
import com.reset.model.domain.preferences.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Vibration swells synced to a breath cycle's phases — the eyes-closed pivot's haptic
 *  delivery, alongside [SessionAudioDelegate]'s audio cues. */
interface HapticsDelegate {
    /** Vibrates one full [pattern] cycle: a rising swell through inhale(s), a light steady
     *  pulse through any hold, a longer fade through exhale. Respects the sounds/haptics
     *  master toggle; a no-op on devices without a vibrator. */
    suspend fun pulseBreathCycle(pattern: BreathPattern)
}

class HapticsDelegateImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val dispatcherProvider: DispatcherProvider,
) : HapticsDelegate {

    override suspend fun pulseBreathCycle(pattern: BreathPattern) = withContext(dispatcherProvider.io) {
        if (!preferencesRepository.preferences.first().soundsEnabled) return@withContext
        val vibrator = systemVibrator() ?: return@withContext
        val (timings, amplitudes) = pattern.toWaveform()
        if (timings.isEmpty()) return@withContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, NO_REPEAT)) }
        } else {
            @Suppress("DEPRECATION")
            runCatching { vibrator.vibrate(timings, NO_REPEAT) }
        }
        Unit
    }

    private fun systemVibrator(): Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private companion object {
        const val NO_REPEAT = -1
    }
}

/** A breath cycle → vibration waveform: ramp in on inhale(s), steady on any hold, fade on
 *  exhale — felt, never startling. Zero-length phases contribute nothing. */
private fun BreathPattern.toWaveform(): Pair<LongArray, IntArray> {
    val timings = mutableListOf<Long>()
    val amplitudes = mutableListOf<Int>()

    fun ramp(durationMs: Int, fromAmp: Int, toAmp: Int) {
        if (durationMs <= 0) return
        val stepMs = (durationMs / RAMP_STEPS).toLong().coerceAtLeast(1)
        repeat(RAMP_STEPS) { i ->
            timings += stepMs
            amplitudes += fromAmp + (toAmp - fromAmp) * (i + 1) / RAMP_STEPS
        }
    }

    ramp(inhaleMs, LOW_AMPLITUDE, PEAK_AMPLITUDE)
    ramp(secondInhaleMs, PEAK_AMPLITUDE, PEAK_AMPLITUDE)
    ramp(holdInMs, PEAK_AMPLITUDE, PEAK_AMPLITUDE)
    ramp(exhaleMs, PEAK_AMPLITUDE, LOW_AMPLITUDE)
    ramp(holdOutMs, LOW_AMPLITUDE, LOW_AMPLITUDE)

    return timings.toLongArray() to amplitudes.toIntArray()
}

private const val RAMP_STEPS = 4
private const val LOW_AMPLITUDE = 1
private const val PEAK_AMPLITUDE = 120
