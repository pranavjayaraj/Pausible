package com.reset.feature.sessions

import com.reset.feature.sessions.content.BreathPattern
import com.reset.feature.sessions.utils.HapticsDelegate

/** Records breath-cycle pulses instead of touching a real [android.os.Vibrator]. */
class FakeHapticsDelegate : HapticsDelegate {

    var pulseCount = 0
        private set

    override suspend fun pulseBreathCycle(pattern: BreathPattern) {
        pulseCount += 1
    }
}
