package com.reset.feature.sessions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.reset.core.designsystem.AppColors
import com.reset.feature.sessions.api.SessionSoundKeys
import com.reset.feature.sessions.content.Modality
import com.reset.feature.sessions.content.SessionScript

/** Timing, theming, and format constants for the session experience. */
object SessionConstants {

    /** Interval between focus countdown ticks, and the break player's honest-duration ring. */
    const val SESSION_TICK_MS = 1_000L

    /** One guided-breathing phase (inhale or exhale) during the custom-sit warm-up. */
    const val BREATH_PHASE_MS = 4_000

    /** The custom warm-up runs 4 phases before the session starts. */
    const val WARMUP_BREATH_TICKS = 4

    const val SECONDS_PER_MINUTE = 60
    const val MS_PER_SECOND = 1_000

    /** Focus backdrop per soundscape (the design's `sessionTheme`). */
    private val focusThemes = mapOf(
        SessionSoundKeys.RAIN to Color(0xFF4A6688),
        SessionSoundKeys.OCEAN to Color(0xFF20606E),
        SessionSoundKeys.FOREST to Color(0xFF3E6E48),
        SessionSoundKeys.FIRE to Color(0xFF7A4230),
        SessionSoundKeys.CHIMES to Color(0xFF6B4EAA),
        SessionSoundKeys.SILENCE to Color(0xFF2E2A5C),
    )

    fun focusTheme(soundKey: String?): Color? = soundKey?.let(focusThemes::get)

    /** Backdrop for a catalog session by [SessionScript.modality]; night scripts (Ember) run
     *  the deepest tone regardless of modality. */
    fun breakBackdrop(script: SessionScript): Color = when {
        script.arrivalByTrigger.containsKey("WIND_DOWN") -> AppColors.tealDeep
        else -> when (script.modality) {
            Modality.CALM, Modality.MIND -> AppColors.breathingBg
            Modality.EYES, Modality.AMBIENT -> AppColors.teal
            Modality.MOVE, Modality.STRETCH -> AppColors.tealDeep
            Modality.CONNECT -> AppColors.accentDark
        }
    }

    // ── Dimens ────────────────────────────────────────────────
    val focusMascotSize = 66.dp
    val focusTitleSpacing = 22.dp
    val ringSpacing = 44.dp
    val controlSpacing = 14.dp
    val controlMinWidth = 130.dp

    /** Formats a seconds count as `m:ss` for the focus countdown. */
    fun formatMmSs(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        return "${safe / SECONDS_PER_MINUTE}:${(safe % SECONDS_PER_MINUTE).toString().padStart(2, '0')}"
    }
}
