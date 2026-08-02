package com.reset.feature.checkin.classify

import javax.inject.Inject

/**
 * The bound [NeedStateClassifier]. Tier-1 (lexicon) always runs first — its own crisis check
 * runs before any need-scoring, so crisis language never even reaches Tier-2 — and chips
 * remain the floor no matter what either tier decides (see `CheckInViewModel`'s
 * `UnrecognizedText` side effect). Tier-2 (embedding) is consulted only when Tier-1 comes
 * back unsure ([NoMatch][NeedStateClassifier.Result.NoMatch] or
 * [Ambiguous][NeedStateClassifier.Result.Ambiguous]); if Tier-2 also can't clear its own
 * confidence floor — including "not ready", which [EmbeddingNeedStateClassifier] surfaces as
 * a plain [NoMatch][NeedStateClassifier.Result.NoMatch] — Tier-1's original call stands rather
 * than silently downgrading further.
 */
class CascadeNeedStateClassifier @Inject constructor(
    private val tier1: LexiconNeedStateClassifier,
    private val tier2: EmbeddingNeedStateClassifier,
) : NeedStateClassifier {

    override suspend fun classify(text: String): NeedStateClassifier.Result {
        val first = tier1.classify(text)
        val unsure = first is NeedStateClassifier.Result.NoMatch || first is NeedStateClassifier.Result.Ambiguous
        if (!unsure) return first // Confident or Crisis — Tier-1 already decided.

        val second = tier2.classify(text)
        val secondResolved = second is NeedStateClassifier.Result.Confident || second is NeedStateClassifier.Result.Ambiguous
        return if (secondResolved) second else first
    }
}
