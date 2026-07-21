package com.reset.model.domain.sense

/**
 * Read/ack seam onto the Sense decision log for in-app surfaces (Home's "Sense noticed"
 * check-in card) that want to react to a just-shown prompt without depending on the
 * `:sense-store` module directly.
 */
interface SenseSuggestionRepository {

    /**
     * The id of the most recent still-unanswered "long sitting stretch" prompt (a
     * STRETCH/RECOVERY_BREAK break type) shown recently, or null when there is none —
     * drives Home's pre-lit check-in card.
     */
    suspend fun pendingSittingStretchDecisionId(): Long?

    /** Records that the pre-lit card's dismissal ("I'm okay, keep going") was tapped. */
    suspend fun dismissSittingStretchPrompt(decisionId: Long)
}
