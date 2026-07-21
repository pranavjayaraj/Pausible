package com.reset.feature.checkin.ui

import com.reset.feature.checkin.ChipId
import com.reset.feature.checkin.R

/** Emoji + label for each [ChipId] — kept out of the plain-Kotlin [ChipId] enum so that type
 *  never needs an Android resource dependency. */
fun ChipId.emoji(): String = when (this) {
    ChipId.STRESSED -> "😮‍💨"
    ChipId.CANT_FOCUS -> "🌀"
    ChipId.NO_ENERGY -> "🪫"
    ChipId.FEELING_LOW -> "😕"
    ChipId.STIFF_ACHY -> "💆"
    ChipId.WRISTS_TIRED -> "🖐"
    ChipId.EYES_TIRED -> "👀"
    ChipId.OVERTHINKING -> "💭"
    ChipId.OVERWHELMED -> "🌊"
    ChipId.FEELING_DISCONNECTED -> "🫂"
    ChipId.CANT_SWITCH_OFF -> "🌙"
    ChipId.BEEN_SITTING_FOREVER -> "🪑"
}

fun ChipId.labelRes(): Int = when (this) {
    ChipId.STRESSED -> R.string.checkin_chip_stressed
    ChipId.CANT_FOCUS -> R.string.checkin_chip_cant_focus
    ChipId.NO_ENERGY -> R.string.checkin_chip_no_energy
    ChipId.FEELING_LOW -> R.string.checkin_chip_feeling_low
    ChipId.STIFF_ACHY -> R.string.checkin_chip_stiff_achy
    ChipId.WRISTS_TIRED -> R.string.checkin_chip_wrists_tired
    ChipId.EYES_TIRED -> R.string.checkin_chip_eyes_tired
    ChipId.OVERTHINKING -> R.string.checkin_chip_overthinking
    ChipId.OVERWHELMED -> R.string.checkin_chip_overwhelmed
    ChipId.FEELING_DISCONNECTED -> R.string.checkin_chip_feeling_disconnected
    ChipId.CANT_SWITCH_OFF -> R.string.checkin_chip_cant_switch_off
    ChipId.BEEN_SITTING_FOREVER -> R.string.checkin_chip_been_sitting_forever
}
