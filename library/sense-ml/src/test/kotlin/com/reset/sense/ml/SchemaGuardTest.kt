package com.reset.sense.ml

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SchemaGuardTest {

    private fun validMeta() = ModelMeta(
        schemaVersion = FeatureSchema.SCHEMA_VERSION,
        featureCount = FeatureSchema.FEATURE_COUNT,
        windowSizeMin = FeatureSchema.WINDOW_MINUTES,
        modelTask = FeatureSchema.MODEL_TASK,
        featureSpecHash = FeatureSchema.specHash(),
        trainedAt = "2026-07-01T00:00:00Z",
    )

    @Test
    fun `valid meta passes`() {
        SchemaGuard.validate(validMeta())
    }

    @Test
    fun `stale schema version is rejected`() {
        expectMismatch(validMeta().copy(schemaVersion = 2), "schema_version")
    }

    @Test
    fun `wrong model task is rejected`() {
        // The break-type bandit artifact must never load into the acceptance slot.
        expectMismatch(validMeta().copy(modelTask = "break_type_bandit"), "model_task")
    }

    @Test
    fun `drifted spec hash is rejected`() {
        expectMismatch(validMeta().copy(featureSpecHash = "deadbeef"), "feature_spec_hash")
    }

    @Test
    fun `wrong feature count is rejected`() {
        expectMismatch(validMeta().copy(featureCount = 35), "feature_count")
    }

    private fun expectMismatch(meta: ModelMeta, expectedInMessage: String) {
        try {
            SchemaGuard.validate(meta)
            fail("expected SchemaMismatchException")
        } catch (e: SchemaMismatchException) {
            assertTrue("message should mention $expectedInMessage: ${e.message}",
                e.message!!.contains(expectedInMessage))
        }
    }
}
