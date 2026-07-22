package com.reset.feature.checkin.classify

import com.reset.model.domain.checkin.NeedState
import javax.inject.Inject
import kotlin.math.max

/**
 * On-device, dependency-free Tier-1 classifier: crisis-first, then a weighted-lexicon scorer
 * with negation/intensifier handling, then confidence thresholds. No network, no model — the
 * output set is only 12 needs (+ crisis/no-match), which is what makes open text tractable.
 *
 * Matching is substring-on-normalized-text so curated roots catch inflections ("stress" →
 * "stressed", "typ" → "typing"); negation/intensifier use a token-window pass for single
 * words (phrases are taken at face value, since "cant stop thinking" is rarely negated).
 *
 * Privacy: pure and local. Callers log only the derived [NeedState] — never the raw text.
 */
class LexiconNeedStateClassifier @Inject constructor() : NeedStateClassifier {

    override fun classify(text: String): NeedStateClassifier.Result {
        val norm = normalize(text)
        if (norm.isBlank()) return NeedStateClassifier.Result.NoMatch

        // 1. Crisis first, high-recall, on the padded surface string.
        val padded = " $norm "
        if (NeedLexicon.CRISIS.any { padded.contains(it) }) {
            return NeedStateClassifier.Result.Crisis
        }

        // 2. Weighted lexicon score per need.
        val tokens = norm.split(' ').filter { it.isNotBlank() }
        val scores = NeedLexicon.TERMS.mapValues { (_, terms) -> scoreNeed(terms, padded, tokens) }
            .filterValues { it > 0.0 }

        if (scores.isEmpty()) return NeedStateClassifier.Result.NoMatch

        // 3. Thresholds. Enum-order iteration keeps ties deterministic.
        val ranked = scores.entries.sortedWith(
            compareByDescending<Map.Entry<NeedState, Double>> { it.value }.thenBy { it.key.ordinal },
        )
        val top = ranked[0]
        val second = ranked.getOrNull(1)

        if (top.value < FLOOR) return NeedStateClassifier.Result.NoMatch
        return if (second != null && second.value >= FLOOR && (top.value - second.value) < MARGIN) {
            NeedStateClassifier.Result.Ambiguous(top.key, second.key)
        } else {
            NeedStateClassifier.Result.Confident(top.key)
        }
    }

    private fun scoreNeed(terms: Map<String, Double>, padded: String, tokens: List<String>): Double {
        var total = 0.0
        for ((term, weight) in terms) {
            if (term.contains(' ')) {
                // Phrase: face-value substring match, no modifier handling.
                if (padded.contains(" $term ") || padded.contains(term)) total += weight
            } else {
                total += scoreWordTerm(term, weight, tokens)
            }
        }
        return total
    }

    /** A word term scores once, at its first occurrence, unless a negator precedes it in the
     *  window; an intensifier in the window amplifies it. */
    private fun scoreWordTerm(term: String, weight: Double, tokens: List<String>): Double {
        val index = tokens.indexOfFirst { it.contains(term) }
        if (index < 0) return 0.0
        val window = tokens.subList(max(0, index - NeedLexicon.MODIFIER_WINDOW), index)
        if (window.any { it in NeedLexicon.NEGATIONS }) return 0.0
        val amplified = if (window.any { it in NeedLexicon.INTENSIFIERS }) NeedLexicon.INTENSIFIER_FACTOR else 1.0
        return weight * amplified
    }

    /** Lowercase, drop apostrophes (can't → cant), non-alphanumerics → spaces, collapse. */
    private fun normalize(text: String): String =
        text.lowercase()
            .replace("'", "")
            .replace("’", "") // curly apostrophe
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

    private companion object {
        /** Below this, the top need is noise → NoMatch (one MEDIUM term clears it). */
        const val FLOOR = NeedLexicon.MEDIUM
        /** Two needs within this of each other are a tie → Ambiguous. */
        const val MARGIN = 0.5
    }
}
