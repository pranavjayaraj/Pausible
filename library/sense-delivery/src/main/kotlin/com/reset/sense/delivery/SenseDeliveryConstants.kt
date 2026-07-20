package com.reset.sense.delivery

import com.reset.sense.ml.BreakType

object SenseDeliveryConstants {

    /**
     * Launch configuration: rules-only. The bundled model was trained purely
     * on the simulator (`ml/bootstrap_data.py`), so it can only re-learn the
     * hand-designed receptivity formula — and the α ramp kept it near-inert
     * for a user's first ~2 months anyway. Flip to true only after a
     * real-data retrain passes the ship gate AND off-policy evaluation
     * (SENSE_ML.md §8.6 stages 3–5). Everything the future model needs —
     * decision log, labels, propensity trail — is still recorded while false.
     */
    const val MODEL_ENABLED = false

    const val CHANNEL_ID = "sense_breaks"
    const val CHANNEL_NAME = "Reset breaks"
    const val CHANNEL_DESCRIPTION = "Well-timed suggestions for short resets"

    const val PERIODIC_WORK_NAME = "sense_evaluation_tick"
    const val SNOOZE_WORK_NAME = "sense_snooze_reeval"
    const val TICK_INTERVAL_MINUTES = 15L
    const val SNOOZE_DELAY_MINUTES = 10L

    const val ACTION_ACCEPTED = "com.reset.sense.delivery.ACCEPTED"
    const val ACTION_DISMISSED = "com.reset.sense.delivery.DISMISSED"
    const val ACTION_SNOOZED = "com.reset.sense.delivery.SNOOZED"
    const val EXTRA_DECISION_ID = "decision_id"
    const val EXTRA_BREAK_TYPE = "break_type"

    const val NOTIFICATION_ID = 56_010

    const val SNOOZE_LABEL = "In 10 min"

    /**
     * Honest copy: every prompt states its cost up front (duration in the
     * title beats "take a break" — uncertainty is the #1 silent non-tap).
     * These become the copy-variant bandit's arms later.
     */
    fun titleFor(breakType: BreakType): String = when (breakType) {
        BreakType.EYE_BREAK -> "30-second eye break?"
        BreakType.STRETCH -> "60-second stretch?"
        BreakType.BREATHING_RESET -> "One slow breath — 45 seconds"
        BreakType.RECOVERY_BREAK -> "You've earned 3 minutes away"
        BreakType.WIND_DOWN -> "Winding down for tonight?"
        BreakType.PASSIVE_REMINDER -> "A pause is waiting when you are"
        BreakType.NONE -> ""
    }

    fun bodyFor(breakType: BreakType): String = when (breakType) {
        BreakType.EYE_BREAK -> "Look at something far away. Blink a few times."
        BreakType.STRETCH -> "Good stopping point. Shoulders down, neck loose."
        BreakType.BREATHING_RESET -> "Lots of switching just now. One breath resets the thread."
        BreakType.RECOVERY_BREAK -> "Long stretch of focus. Stand up, water, window."
        BreakType.WIND_DOWN -> "Screens off soon means better sleep tonight."
        BreakType.PASSIVE_REMINDER -> "No rush — whenever you stop moving."
        BreakType.NONE -> ""
    }
}
