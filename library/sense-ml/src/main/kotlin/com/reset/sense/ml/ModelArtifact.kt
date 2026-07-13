package com.reset.sense.ml

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * On-disk model artifact contract (`model.json`, produced by `ml/train.py`).
 * Weights ship as raw arrays — no TFLite. The metadata block is the schema
 * handshake: [SchemaGuard] validates it before any weight is touched.
 */
@Serializable
data class ModelMeta(
    @SerialName("schema_version") val schemaVersion: Int,
    @SerialName("feature_count") val featureCount: Int,
    @SerialName("window_size_min") val windowSizeMin: Int,
    @SerialName("model_task") val modelTask: String,
    @SerialName("feature_spec_hash") val featureSpecHash: String,
    @SerialName("trained_at") val trainedAt: String,
)

@Serializable
internal data class LayerDto(
    val activation: String, // "relu" | "sigmoid"
    /** Row-major [inputDim][outputDim] — matches the forward-pass loop. */
    val weights: List<List<Float>>,
    val bias: List<Float>,
)

@Serializable
internal data class ModelArtifactDto(
    @SerialName("schema_version") val schemaVersion: Int,
    @SerialName("feature_count") val featureCount: Int,
    @SerialName("window_size_min") val windowSizeMin: Int,
    @SerialName("model_task") val modelTask: String,
    @SerialName("feature_spec_hash") val featureSpecHash: String,
    @SerialName("trained_at") val trainedAt: String,
    val layers: List<LayerDto>,
) {
    fun meta() = ModelMeta(schemaVersion, featureCount, windowSizeMin, modelTask, featureSpecHash, trainedAt)
}

/** Thrown when a model artifact does not match this build's feature pipeline. */
class SchemaMismatchException(message: String) : IllegalStateException(message)

/**
 * Validates the artifact↔pipeline handshake. The feature pipeline is compiled
 * into the SDK; the weights file can arrive via OTA — these two can drift
 * independently, so every mismatch must be a loud, typed failure that the
 * caller turns into "fall back to bundled model, then rules engine".
 */
object SchemaGuard {

    fun validate(meta: ModelMeta) {
        check(meta.schemaVersion, FeatureSchema.SCHEMA_VERSION, "schema_version")
        check(meta.featureCount, FeatureSchema.FEATURE_COUNT, "feature_count")
        check(meta.windowSizeMin, FeatureSchema.WINDOW_MINUTES, "window_size_min")
        if (meta.modelTask != FeatureSchema.MODEL_TASK) {
            throw SchemaMismatchException(
                "model_task '${meta.modelTask}' != '${FeatureSchema.MODEL_TASK}' — " +
                    "wrong artifact loaded into the acceptance-model slot"
            )
        }
        val pipelineHash = FeatureSchema.specHash()
        if (meta.featureSpecHash != pipelineHash) {
            throw SchemaMismatchException(
                "feature_spec_hash mismatch: artifact=${meta.featureSpecHash.take(12)}… " +
                    "pipeline=${pipelineHash.take(12)}… — normalization or ordering drifted " +
                    "without a schema_version bump"
            )
        }
    }

    private fun check(actual: Int, expected: Int, field: String) {
        if (actual != expected) {
            throw SchemaMismatchException("$field mismatch: artifact=$actual pipeline=$expected")
        }
    }
}

internal val artifactJson = Json { ignoreUnknownKeys = true }
