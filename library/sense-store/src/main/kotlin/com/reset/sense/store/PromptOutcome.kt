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
     * Responsiveness signal for the accept-rate FEATURES (Block 5 of the
     * vector), where "did they react at all" is legitimately the question.
     * NOT the training label — that is [trainingWeight]. Do not "simplify"
     * training code to use this: rewarding taps teaches the copy bandit
     * clickbait (taps that abandon), see docs/SENSE_ML.md §7.
     */
    val isPositive: Boolean get() = this == ACCEPTED || this == COMPLETED

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
            ACCEPTED -> 0.2f         // attention, not success — tapped then abandoned
            SNOOZED -> 0.1f          // "right idea, wrong minute": mild positive for need
            IGNORED -> -0.1f         // barely informative — may never have been seen
            DISMISSED_SLOW -> -0.5f  // considered and declined
            DISMISSED_FAST -> -1.0f  // hard negative: actively swatted away
            PENDING -> 0.0f          // unlabeled; never exported for training
            NOT_SHOWN -> 0.0f        // never seen by the user; teaches nothing
        }
}
