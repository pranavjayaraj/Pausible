package com.reset.feature.checkin.classify

import com.reset.model.domain.checkin.NeedState

/**
 * Turns free text into a [NeedState] — the second writer to the NeedState seam (chips are
 * the first; see [com.reset.feature.checkin.ChipId]). Everything downstream of the seam
 * (selection, gating, offer, logging) consumes only [NeedState], so text and chips are
 * interchangeable producers: the only new thing text needs is *this* mapping.
 *
 * The output is deliberately a tiny closed set even though the input is open — the classifier
 * recognizes the 12 needs it knows and honestly punts everything else to [Result.NoMatch]
 * (→ the quick chips) rather than guessing. Precision over recall: a wrong confident offer
 * erodes trust; a graceful "tap one instead" does not.
 */
interface NeedStateClassifier {
    fun classify(text: String): Result

    sealed interface Result {
        /** A clear winner — route straight to selection (or its follow-up). */
        data class Confident(val needState: NeedState) : Result

        /** Two close candidates — route to a binary clarifier when the pair has one. */
        data class Ambiguous(val top: NeedState, val second: NeedState) : Result

        /** Nothing cleared the confidence floor (vague, off-topic, out-of-scope) → chips. */
        data object NoMatch : Result

        /** Self-harm / crisis language — bypass everything, straight to support resources.
         *  Runs first and biased toward recall: a false positive is mildly annoying, a
         *  false negative is dangerous. */
        data object Crisis : Result
    }
}
