package com.reset.feature.builder

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reset.feature.sessions.api.SessionSoundKeys

/** All option keys, timing, dimension, and analytics constants for the Builder feature. */
object BuilderConstants {

    // ── Option keys (persisted in presets — never display strings) ──
    const val INTENT_FOCUS = "focus"
    const val INTENT_UNWIND = "unwind"
    const val INTENT_SLEEP = "sleep"
    const val INTENT_ANXIETY = "anxiety"
    const val INTENT_ENERGY = "energy"

    const val SOUND_RAIN = SessionSoundKeys.RAIN
    const val SOUND_OCEAN = SessionSoundKeys.OCEAN
    const val SOUND_FOREST = SessionSoundKeys.FOREST
    const val SOUND_FIRE = SessionSoundKeys.FIRE
    const val SOUND_CHIMES = SessionSoundKeys.CHIMES
    const val SOUND_SILENCE = SessionSoundKeys.SILENCE

    const val PACE_SLOW_SEC = 6
    const val PACE_MEDIUM_SEC = 4
    const val PACE_QUICK_SEC = 3

    const val BELL_OFF = 0

    const val REMIND_OFF = "off"
    const val REMIND_MORNING = "morning"
    const val REMIND_MIDDAY = "midday"
    const val REMIND_EVENING = "evening"

    const val DEFAULT_CUSTOM_MIN = 15
    val DURATION_OPTIONS = listOf(5, 10, 15, 20, 30, 45, 60)
    val BELL_OPTIONS = listOf(BELL_OFF, 5, 10)
    const val MIX_MAX_PERCENT = 100
    const val SESSION_NAME_MAX_CHARS = 40

    // ── Suggestion windows (24h clock) ────────────────────────
    const val SUGGEST_MORNING_UNTIL = 11
    const val SUGGEST_AFTERNOON_UNTIL = 17
    const val SUGGEST_EVENING_UNTIL = 22

    // ── Sound catalogue (key ↔ tile colours; labels live in strings.xml) ──
    data class SoundOption(
        val key: String,
        val emoji: String,
        val tile: Color,
        val foreground: Color,
    )

    val soundOptions = listOf(
        SoundOption(SOUND_RAIN, "🌧️", Color(0xFFBDE3F0), Color(0xFF29264A)),
        SoundOption(SOUND_OCEAN, "🌊", Color(0xFF9CCFE8), Color(0xFF29264A)),
        SoundOption(SOUND_FOREST, "🌲", Color(0xFFC9E3B4), Color(0xFF29264A)),
        SoundOption(SOUND_FIRE, "🔥", Color(0xFFF8CFA0), Color(0xFF29264A)),
        SoundOption(SOUND_CHIMES, "🎐", Color(0xFFE3D9F5), Color(0xFF29264A)),
        SoundOption(SOUND_SILENCE, "🌙", Color(0xFF2E2A5C), Color(0xFFFFFFFF)),
    )

    val intentKeys = listOf(INTENT_FOCUS, INTENT_UNWIND, INTENT_SLEEP, INTENT_ANXIETY, INTENT_ENERGY)
    val paceOptions = listOf(PACE_SLOW_SEC, PACE_MEDIUM_SEC, PACE_QUICK_SEC)
    val remindKeys = listOf(REMIND_OFF, REMIND_MORNING, REMIND_MIDDAY, REMIND_EVENING)

    // ── Dimens ────────────────────────────────────────────────
    val sectionSpacing = 22.dp
    val cardSpacing = 14.dp
    val chipSpacing = 8.dp
    val gridSpacing = 10.dp
    val builderMascotSize = 64.dp
    val builderMascotHalo = 84.dp
    val suggestMascotSize = 40.dp
    val toggleWidth = 46.dp
    val toggleHeight = 26.dp
    val toggleKnob = 20.dp
}
