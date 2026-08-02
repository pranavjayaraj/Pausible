package com.reset.feature.checkin.classify

import com.reset.model.domain.checkin.NeedState
import com.reset.model.domain.checkin.TextEmbedder
import kotlinx.coroutines.CancellationException
import kotlin.math.sqrt

/**
 * Tier-2: embed the input, cosine-similarity it against [NeedPrototypes]' cached vectors,
 * nearest-above-threshold wins. Pure given its inputs — no Android types, no PAD — so it's
 * fully unit-testable with a fake [TextEmbedder] and an in-memory prototype map.
 *
 * Never crashes into the caller: a missing model, a failed embed, or a bad vector all degrade
 * to [NeedStateClassifier.Result.NoMatch] — the same "stay on Tier-1" signal as "not ready".
 */
class EmbeddingNeedStateClassifier(
    private val embedder: TextEmbedder,
    /** Suspends because the real implementation embeds ~50 fixed phrases once and caches the
     *  result to disk; null means the model isn't ready to compute them yet. */
    private val prototypeVectors: suspend () -> Map<NeedState, List<FloatArray>>?,
) : NeedStateClassifier {

    override suspend fun classify(text: String): NeedStateClassifier.Result {
        try {
            val vector = embedder.embed(text) ?: return NeedStateClassifier.Result.NoMatch
            val prototypes = prototypeVectors()
            if (prototypes.isNullOrEmpty()) return NeedStateClassifier.Result.NoMatch

            val scores = prototypes.mapValues { (_, vectors) -> vectors.maxOf { cosineSimilarity(vector, it) } }
            val ranked = scores.entries.sortedWith(
                compareByDescending<Map.Entry<NeedState, Float>> { it.value }.thenBy { it.key.ordinal },
            )
            val top = ranked.first()
            val second = ranked.getOrNull(1)

            return when {
                top.value < CONFIDENCE_FLOOR -> NeedStateClassifier.Result.NoMatch
                second != null && second.value >= CONFIDENCE_FLOOR && (top.value - second.value) < AMBIGUITY_MARGIN ->
                    NeedStateClassifier.Result.Ambiguous(top.key, second.key)
                else -> NeedStateClassifier.Result.Confident(top.key)
            }
        } catch (e: CancellationException) {
            throw e // never swallow cancellation — only inference/IO failures degrade to NoMatch.
        } catch (e: Exception) {
            return NeedStateClassifier.Result.NoMatch
        }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.isEmpty() || a.size != b.size) return 0f
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0f) 0f else dot / denom
    }

    companion object {
        /** Below this cosine similarity, the nearest prototype is too far to trust. */
        const val CONFIDENCE_FLOOR = 0.55f
        /** Two needs within this of each other are a tie → Ambiguous. */
        const val AMBIGUITY_MARGIN = 0.05f
    }
}
