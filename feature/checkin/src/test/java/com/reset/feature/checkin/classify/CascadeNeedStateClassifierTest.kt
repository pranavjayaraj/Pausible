package com.reset.feature.checkin.classify

import com.reset.feature.checkin.classify.NeedStateClassifier.Result
import com.reset.model.domain.checkin.NeedState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exercises the real [LexiconNeedStateClassifier] as Tier-1 (it's pure and dependency-free,
 * so faking it would only hide real wiring bugs) and a fake-embedder-backed Tier-2, matching
 * how [com.reset.feature.checkin.di.CheckInModule] actually composes them.
 */
class CascadeNeedStateClassifierTest {

    private val bodyTension = floatArrayOf(1f, 0f, 0f)
    private val woundUp = floatArrayOf(0f, 1f, 0f)
    private val drained = floatArrayOf(0f, 0f, 1f)
    private val prototypes: Map<NeedState, List<FloatArray>> = mapOf(
        NeedState.BODY_TENSION to listOf(bodyTension),
        NeedState.WOUND_UP to listOf(woundUp),
        NeedState.DRAINED to listOf(drained),
    )

    private fun cascadeWith(embedder: FakeTextEmbedder) = CascadeNeedStateClassifier(
        tier1 = LexiconNeedStateClassifier(),
        tier2 = EmbeddingNeedStateClassifier(embedder, prototypeVectors = { prototypes }),
    )

    @Test
    fun `Tier-1 confident short-circuits — Tier-2 is never consulted`() = runTest {
        val embedder = FakeTextEmbedder()
        val result = cascadeWith(embedder).classify("my neck is killing me")

        assertEquals(Result.Confident(NeedState.BODY_TENSION), result)
        assertEquals(0, embedder.callCount)
    }

    @Test
    fun `crisis short-circuits before any tier scores a need`() = runTest {
        val embedder = FakeTextEmbedder()
        val result = cascadeWith(embedder).classify("I want to disappear")

        assertEquals(Result.Crisis, result)
        assertEquals(0, embedder.callCount)
    }

    @Test
    fun `Tier-1 NoMatch escalates to a ready, confident Tier-2`() = runTest {
        // "asdfghjkl" is a confirmed Tier-1 NoMatch (see LexiconNeedStateClassifierTest).
        val embedder = FakeTextEmbedder(mapOf("asdfghjkl" to drained))
        val result = cascadeWith(embedder).classify("asdfghjkl")

        assertEquals(Result.Confident(NeedState.DRAINED), result)
        assertEquals(1, embedder.callCount)
    }

    @Test
    fun `Tier-1 NoMatch with Tier-2 not ready falls back to Tier-1's NoMatch`() = runTest {
        val embedder = FakeTextEmbedder(alwaysFails = true) // simulates "model not downloaded"
        val result = cascadeWith(embedder).classify("asdfghjkl")

        assertEquals(Result.NoMatch, result)
        assertEquals(1, embedder.callCount) // it was consulted — it just wasn't ready
    }

    @Test
    fun `Tier-1 Ambiguous is resolved by a confident Tier-2`() = runTest {
        // "stressed and wiped out" is a confirmed Tier-1 Ambiguous(WOUND_UP, DRAINED).
        val embedder = FakeTextEmbedder(mapOf("stressed and wiped out" to woundUp))
        val result = cascadeWith(embedder).classify("stressed and wiped out")

        assertEquals(Result.Confident(NeedState.WOUND_UP), result)
    }

    @Test
    fun `Tier-1 Ambiguous stands when Tier-2 is also unsure — never silently downgraded`() = runTest {
        val embedder = FakeTextEmbedder(mapOf("stressed and wiped out" to floatArrayOf(-1f, -1f, -1f)))
        val result = cascadeWith(embedder).classify("stressed and wiped out")

        assertTrue(result is Result.Ambiguous)
        result as Result.Ambiguous
        assertEquals(setOf(NeedState.WOUND_UP, NeedState.DRAINED), setOf(result.top, result.second))
    }
}
