package com.reset.feature.sessions

import com.reset.feature.sessions.utils.SessionAudioDelegate

/** Records chime/tick calls instead of touching real audio playback. */
class FakeSessionAudioDelegate : SessionAudioDelegate {

    var arrivalChimes = 0
        private set
    var landingChimes = 0
        private set
    var breathTicks = 0
        private set

    override suspend fun playArrivalChime() {
        arrivalChimes += 1
    }

    override suspend fun playLandingChime() {
        landingChimes += 1
    }

    override suspend fun playBreathTick() {
        breathTicks += 1
    }
}
