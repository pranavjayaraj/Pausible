package com.reset.sense.store

/**
 * Terminal (and pending) states of a shown prompt. This is the label taxonomy
 * for training — the distinctions matter:
 *
 *  - [DISMISSED_FAST] is a hard negative (bad timing, user actively rejected);
 *  - [DISMISSED_SLOW] and [IGNORED] are weak negatives (user may not even
 *    have seen it);
 *  - [SNOOZED] is "right idea, wrong minute" — negative for this timestamp,
 *    positive for interrupt-need;
 *  - [COMPLETED] (host-reported via reportOutcome) is the true objective,
 *    stronger than [ACCEPTED] (tap only — could still abandon the break).
 */
enum class PromptOutcome {
    /** Prompt shown, no response yet. Swept to IGNORED after a timeout. */
    PENDING,
    ACCEPTED,

    /**
     * The host app reached the foreground within the attribution window of a
     * still-pending prompt — the user came, just not through the tap.
     * Inferred by [SenseDecisionLog.resolveAppOpenOutcomes] (host onResume);
     * an explicit tap racing this inference refines it to [ACCEPTED].
     * Success under the CLICK_IS_SUCCESS launch policy.
     */
    OPENED_APP,
    COMPLETED,
    DISMISSED_FAST,
    DISMISSED_SLOW,
    SNOOZED,
    IGNORED,

    /**
     * Race guard: the engine decided to prompt, but the notification was
     * withheld at the last moment (host app came to the foreground between
     * decision and notify — e.g. an overdue WorkManager tick released by the
     * very app-open it would have interrupted). The user never saw anything,
     * so this row is excluded from EVERY statistic: caps, accept rates,
     * training. It exists purely as a diagnostics trail.
     */
    NOT_SHOWN,
    ;

    val isTerminal: Boolean get() = this != PENDING

    /**
     * Whether this outcome counts as SUCCESS under the current policy —
     * drives the cooldown gate, the earned-trust daily cap, and the
     * `break_completion_rate` stat. See [CLICK_IS_SUCCESS].
     */
    val isSuccess: Boolean
        get() = this == COMPLETED ||
            (CLICK_IS_SUCCESS && (this == ACCEPTED || this == OPENED_APP))

    /**
     * Responsiveness signal for the accept-rate FEATURES (Block 5 of the
     * vector), where "did they react at all" is legitimately the question.
     * NOT the training label — that is [trainingWeight]. Do not "simplify"
     * training code to use this: rewarding taps teaches the copy bandit
     * clickbait (taps that abandon), see docs/SENSE_ML.md §7.
     */
    val isPositive: Boolean get() = this == ACCEPTED || this == COMPLETED || this == OPENED_APP

    /**
     * The training label, as a signed sample weight in [-1, 1]: sign is the
     * label, magnitude is how much this outcome should teach the model.
     * This value table IS the product's reward philosophy — completion is
     * the objective, a tap is faint praise, a fast dismissal is the loudest
     * signal we get. Changing these numbers changes what the system
     * optimizes for; treat any edit as a design decision, not a tweak.
     */
    val trainingWeight: Float
        get() = when (this) {
            COMPLETED -> 1.0f        // the actual product outcome: rest happened
            // Under CLICK_IS_SUCCESS a tap or an attributed app open is a
            // full positive; the durable philosophy (completion > clicks)
            // returns when the flag flips.
            ACCEPTED -> if (CLICK_IS_SUCCESS) 1.0f else 0.2f
            OPENED_APP -> if (CLICK_IS_SUCCESS) 1.0f else 0.2f
            SNOOZED -> 0.1f          // "right idea, wrong minute": mild positive for need
            IGNORED -> -0.1f         // barely informative — may never have been seen
            DISMISSED_SLOW -> -0.5f  // considered and declined
            DISMISSED_FAST -> -1.0f  // hard negative: actively swatted away
            PENDING -> 0.0f          // unlabeled; never exported for training
            NOT_SHOWN -> 0.0f        // never seen by the user; teaches nothing
        }

    companion object {
        /**
         * TEMPORARY LAUNCH POLICY: a notification TAP counts as success.
         * The break-completion loop (deep link → session → host report) is
         * not yet the trusted signal, so cooldown, earned trust, the
         * completion-rate stat, and training weights all run on clicks for
         * now. The LOG stays honest — ACCEPTED and COMPLETED are still
         * recorded as distinct outcomes — only the interpretation changes,
         * so flipping this back is lossless for historical data.
         *
         * FLIP BACK to false before re-enabling the model or building any
         * copy bandit: training on clicks optimizes for taps that abandon
         * (docs/SENSE_ML.md §7 labeling rules).
         */
        const val CLICK_IS_SUCCESS = true

        /** Outcome names counting as success right now — for SQL `IN` filters. */
        fun successNames(): List<String> =
            if (CLICK_IS_SUCCESS) listOf(COMPLETED.name, ACCEPTED.name, OPENED_APP.name)
            else listOf(COMPLETED.name)
    }
}
