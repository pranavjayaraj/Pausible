package com.reset.sense.ml

import kotlin.math.exp
import kotlin.math.max

/**
 * Pure-Kotlin inference for the acceptance MLP (34 → 32 → 16 → 1).
 *
 * Deliberately no TFLite: at ~1.5K parameters the forward pass is three
 * matrix-vector products — microseconds on any device, zero native libs,
 * zero dependency conflicts with host apps, and fully debuggable. If a future
 * model genuinely needs an interpreter (sequence models, >100K params), swap
 * the internals here; the [predict] contract is the module boundary.
 *
 * Accumulation is done in Double to keep parity with the float64 training
 * pipeline inside the golden-vector tolerance.
 */
class AcceptanceModel private constructor(
    private val layers: List<Layer>,
    val meta: ModelMeta,
) {

    /** @return P(user accepts a prompt now) in 0..1. */
    fun predict(features: FloatArray): Float {
        require(features.size == meta.featureCount) {
            "expected ${meta.featureCount} features, got ${features.size}"
        }
        var h = DoubleArray(features.size) { features[it].toDouble() }
        for (layer in layers) h = layer.forward(h)
        val p = h[0]
        check(!p.isNaN() && p in 0.0..1.0) { "inference produced invalid probability: $p" }
        return p.toFloat()
    }

    private class Layer(
        val weights: Array<DoubleArray>, // [in][out]
        val bias: DoubleArray,
        val relu: Boolean,
    ) {
        fun forward(x: DoubleArray): DoubleArray {
            val out = DoubleArray(bias.size)
            for (j in bias.indices) {
                var sum = bias[j]
                for (i in x.indices) sum += x[i] * weights[i][j]
                out[j] = if (relu) max(0.0, sum) else 1.0 / (1.0 + exp(-sum))
            }
            return out
        }
    }

    companion object {

        /**
         * Parses and validates a model.json. Throws [SchemaMismatchException]
         * on pipeline drift and [IllegalStateException] on malformed weights —
         * callers (the decision engine) degrade to bundled model → rules.
         */
        fun fromJson(json: String): AcceptanceModel {
            val dto = artifactJson.decodeFromString(ModelArtifactDto.serializer(), json)
            SchemaGuard.validate(dto.meta())

            var inputDim = dto.featureCount
            val layers = dto.layers.mapIndexed { index, layer ->
                check(layer.weights.size == inputDim) {
                    "layer $index: expected $inputDim input rows, got ${layer.weights.size}"
                }
                val outDim = layer.bias.size
                check(layer.weights.all { it.size == outDim }) { "layer $index: ragged weight matrix" }
                val isLast = index == dto.layers.lastIndex
                check(layer.activation == if (isLast) "sigmoid" else "relu") {
                    "layer $index: unexpected activation '${layer.activation}'"
                }
                inputDim = outDim
                Layer(
                    weights = Array(layer.weights.size) { r ->
                        DoubleArray(outDim) { c -> layer.weights[r][c].toDouble() }
                    },
                    bias = DoubleArray(outDim) { layer.bias[it].toDouble() },
                    relu = !isLast,
                )
            }
            check(inputDim == 1) { "final layer must output a single probability" }
            return AcceptanceModel(layers, dto.meta())
        }
    }
}
