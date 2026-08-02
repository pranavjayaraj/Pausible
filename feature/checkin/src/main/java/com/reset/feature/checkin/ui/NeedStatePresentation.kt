package com.reset.feature.checkin.ui

import com.reset.feature.checkin.R
import com.reset.model.domain.checkin.NeedState

/** The offer eyebrow's "FOR X" label per need — UI-only presentation, never read by selection. */
fun NeedState.labelRes(): Int = when (this) {
    NeedState.BODY_TENSION -> R.string.checkin_need_body_tension
    NeedState.HAND_STRAIN -> R.string.checkin_need_hand_strain
    NeedState.EYE_STRAIN -> R.string.checkin_need_eye_strain
    NeedState.WOUND_UP -> R.string.checkin_need_wound_up
    NeedState.SCATTERED -> R.string.checkin_need_scattered
    NeedState.DRAINED -> R.string.checkin_need_drained
    NeedState.LOW_MOOD -> R.string.checkin_need_low_mood
    NeedState.STUCK_ON_A_THOUGHT -> R.string.checkin_need_stuck_on_a_thought
    NeedState.RESTLESS -> R.string.checkin_need_restless
    NeedState.CANT_WIND_DOWN -> R.string.checkin_need_cant_wind_down
    NeedState.OVERWHELMED -> R.string.checkin_need_overwhelmed
    NeedState.DISCONNECTED -> R.string.checkin_need_disconnected
    NeedState.CONFUSED_LOST -> R.string.checkin_need_confused_lost
    NeedState.TASK_PARALYSIS -> R.string.checkin_need_task_paralysis
    NeedState.SENSORY_OVERLOAD -> R.string.checkin_need_sensory_overload
    NeedState.BIOLOGICAL_DEPLETION -> R.string.checkin_need_biological_depletion
    NeedState.ACCOMPLISHED_FLOW -> R.string.checkin_need_accomplished_flow
}

/** Empathy-toned offer palette (design "3c") for the two feelings-first NeedStates. */
fun NeedState.isEmpathyToned(): Boolean = this == NeedState.LOW_MOOD || this == NeedState.DISCONNECTED

/** The quiet "Need support now?" link only shows for these two — see Deliverable 7. */
fun NeedState.showsSupportLink(): Boolean = this == NeedState.LOW_MOOD || this == NeedState.DISCONNECTED
