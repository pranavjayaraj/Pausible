package com.reset.feature.checkin.classify

import com.reset.feature.checkin.classify.NeedStateClassifier.Result
import com.reset.model.domain.checkin.NeedState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbeddingNeedStateClassifierTest {

    // Orthogonal basis vectors — cosine similarity to each is exact and easy to reason about.
    private val bodyTension = floatArrayOf(1f, 0f, 0f)
    private val woundUp = floatArrayOf(0f, 1f, 0f)
    private val drained = floatArrayOf(0f, 0f, 1f)

    private val prototypes: Map<NeedState, List<FloatArray>> = mapOf(
        NeedState.BODY_TENSION to listOf(bodyTension),
        NeedState.WOUND_UP to listOf(woundUp),
        NeedState.DRAINED to listOf(drained),
    )

    private fun classifierFor(embedder: FakeTextEmbedder) =
        EmbeddingNeedStateClassifier(embedder, prototypeVectors = { prototypes })

    @Test
    fun `nearest prototype above the floor wins as Confident`() = runTest {
        val embedder = FakeTextEmbedder(mapOf("my neck hurts" to bodyTension))
        val result = classifierFor(embedder).classify("my neck hurts")
        assertEquals(Result.Confident(NeedState.BODY_TENSION), result)
    }

    @Test
    fun `a vector far from every prototype is NoMatch`() = runTest {
        val embedder = FakeTextEmbedder(mapOf("unrelated" to floatArrayOf(-1f, -1f, -1f)))
        assertEquals(Result.NoMatch, classifierFor(embedder).classify("unrelated"))
    }

    @Test
    fun `two equally-close prototypes return Ambiguous, ordinal-ordered`() = runTest {
        // Equidistant from BODY_TENSION and WOUND_UP, far from DRAINED.
        val embedder = FakeTextEmbedder(mapOf("torn between two" to floatArrayOf(1f, 1f, 0f)))
        val result = classifierFor(embedder).classify("torn between two")
        assertTrue(result is Result.Ambiguous)
        result as Result.Ambiguous
        assertEquals(NeedState.BODY_TENSION, result.top) // lower ordinal wins the tiebreak
        assertEquals(NeedState.WOUND_UP, result.second)
    }

    @Test
    fun `a null embedding (model not ready) is NoMatch, not an error`() = runTest {
        val embedder = FakeTextEmbedder(alwaysFails = true)
        assertEquals(Result.NoMatch, classifierFor(embedder).classify("anything"))
    }

    @Test
    fun `empty or null prototype vectors degrade to NoMatch`() = runTest {
        val embedder = FakeTextEmbedder(mapOf("text" to bodyTension))
        val emptyPrototypes = EmbeddingNeedStateClassifier(embedder) { emptyMap() }
        assertEquals(Result.NoMatch, emptyPrototypes.classify("text"))

        val nullPrototypes = EmbeddingNeedStateClassifier(embedder) { null }
        assertEquals(Result.NoMatch, nullPrototypes.classify("text"))
    }

    @Test
    fun `a throwing prototype source never crashes the caller`() = runTest {
        val embedder = FakeTextEmbedder(mapOf("text" to bodyTension))
        val throwing = EmbeddingNeedStateClassifier(embedder) { error("boom") }
        assertEquals(Result.NoMatch, throwing.classify("text"))
    }

    @Test
    fun `mismatched vector dimensions never crash, just score zero`() = runTest {
        val embedder = FakeTextEmbedder(mapOf("odd" to floatArrayOf(1f, 0f))) // wrong size
        assertEquals(Result.NoMatch, classifierFor(embedder).classify("odd"))
    }
}
