package com.reset.feature.checkin.classify

import com.reset.model.domain.checkin.TextEmbedder

/** Deterministic [TextEmbedder] for classifier tests: [vectors] maps exact input strings to
 *  pre-baked embeddings; anything else (or every call, if [alwaysFails] is set) returns null,
 *  matching the real "model not ready" contract. */
class FakeTextEmbedder(
    private val vectors: Map<String, FloatArray> = emptyMap(),
    private val alwaysFails: Boolean = false,
) : TextEmbedder {
    var callCount = 0
        private set

    override suspend fun embed(text: String): FloatArray? {
        callCount++
        if (alwaysFails) return null
        return vectors[text]
    }
}
