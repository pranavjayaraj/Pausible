package com.reset.feature.checkin

import com.reset.model.domain.checkin.NeedState

/**
 * The chip grid's own vocabulary — deliberately never exported past this feature. Session
 * selection consumes only [NeedState] (see [com.reset.model.domain.checkin]); chip identity,
 * label, and emoji stop here, at the one place that turns a tap into a need.
 */
enum class ChipId(val needState: NeedState) {
    STRESSED(NeedState.WOUND_UP),
    CANT_FOCUS(NeedState.SCATTERED),
    NO_ENERGY(NeedState.DRAINED),
    FEELING_LOW(NeedState.LOW_MOOD),
    STIFF_ACHY(NeedState.BODY_TENSION),
    WRISTS_TIRED(NeedState.HAND_STRAIN),
    EYES_TIRED(NeedState.EYE_STRAIN),
    OVERTHINKING(NeedState.STUCK_ON_A_THOUGHT),
    OVERWHELMED(NeedState.OVERWHELMED),
    FEELING_DISCONNECTED(NeedState.DISCONNECTED),

    /** Contextual-only: swaps in for [NO_ENERGY] after quiet hours start — never shown by
     *  default, never auto-selected. */
    CANT_SWITCH_OFF(NeedState.CANT_WIND_DOWN),

    /** Contextual-only: swaps in for [STIFF_ACHY] after 60+ minutes of stillness — surfaced
     *  pre-highlighted (glow), never auto-selected. */
    BEEN_SITTING_FOREVER(NeedState.RESTLESS),
    ;

    /** True for a [NeedState] with a binary follow-up clarifier before selection. */
    val hasFollowUp: Boolean get() = needState == NeedState.WOUND_UP || needState == NeedState.BODY_TENSION

    companion object {
        /** Always-visible grid order — 10 chips, no scroll. [CANT_SWITCH_OFF] and
         *  [BEEN_SITTING_FOREVER] are never in this list; they replace [NO_ENERGY] /
         *  [STIFF_ACHY] contextually (see [gridFor]). */
        val BASE_GRID = listOf(
            STRESSED, CANT_FOCUS, NO_ENERGY, FEELING_LOW, STIFF_ACHY,
            WRISTS_TIRED, EYES_TIRED, OVERTHINKING, OVERWHELMED, FEELING_DISCONNECTED,
        )

        /** The grid to render right now: [isNight] swaps [NO_ENERGY]→[CANT_SWITCH_OFF];
         *  [longStillness] swaps [STIFF_ACHY]→[BEEN_SITTING_FOREVER]. Both are driven by
         *  context passed in, never by the chip grid reading Sense/clock itself. */
        fun gridFor(isNight: Boolean, longStillness: Boolean): List<ChipId> = BASE_GRID.map { chip ->
            when {
                chip == NO_ENERGY && isNight -> CANT_SWITCH_OFF
                chip == STIFF_ACHY && longStillness -> BEEN_SITTING_FOREVER
                else -> chip
            }
        }
    }
}
