package com.reset.sense.ml

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Cross-language parity: replays golden vectors produced by ml/train.py
 * through the pure-Kotlin forward pass. If this passes, Python training and
 * Kotlin inference are numerically the same model.
 */
class ModelParityTest {

    @Serializable
    private data class GoldenCase(val features: List<Float>, val expected: Float)

    @Serializable
    private data class GoldenFile(
        val schema_version: Int,
        val tolerance: Float,
        val cases: List<GoldenCase>,
    )

    private fun resource(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "missing test resource $name" }
            .bufferedReader().readText()

    private val model by lazy { AcceptanceModel.fromJson(resource("model.json")) }

    @Test
    fun `golden vectors match python outputs`() {
        val golden = Json { ignoreUnknownKeys = true }
            .decodeFromString(GoldenFile.serializer(), resource("golden_vectors.json"))
        assertEquals(FeatureSchema.SCHEMA_VERSION, golden.schema_version)
        assertTrue("golden file should carry probe cases", golden.cases.size >= 10)

        for ((i, case) in golden.cases.withIndex()) {
            val p = model.predict(case.features.toFloatArray())
            assertTrue(
                "case $i: kotlin=$p python=${case.expected} diff=${abs(p - case.expected)}",
                abs(p - case.expected) <= golden.tolerance,
            )
        }
    }

    @Test
    fun `artifact spec hash matches this build's feature pipeline`() {
        // The strongest drift check: the hash stamped at training time equals
        // the hash of the Kotlin canonical spec string.
        assertEquals(model.meta.featureSpecHash, FeatureSchema.specHash())
    }

    @Test
    fun `predictions are valid probabilities across random inputs`() {
        val rng = java.util.Random(7)
        repeat(500) {
            val f = FloatArray(FeatureSchema.FEATURE_COUNT) { rng.nextFloat() }
            val p = model.predict(f)
            assertTrue("p=$p out of range", p in 0f..1f)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `wrong feature count is rejected`() {
        model.predict(FloatArray(10))
    }
}
