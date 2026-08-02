package com.reset.model.domain.checkin

/**
 * Tier-2 semantic text embedding — the seam the Check In feature's cascade classifier escalates
 * to when its Tier-1 lexicon can't place a NeedState. Deliberately declared here, in `:model`,
 * rather than in `:feature:checkin`: the concrete implementation (a MediaPipe sentence encoder)
 * lives in its own library module (`:text-embed`) so the ~5.8MB model runtime isn't forced onto
 * every consumer of `:feature:checkin` — and a library module must not depend on a feature
 * module, so the shared contract has to sit one layer down, in the domain module both already
 * depend on.
 *
 * Privacy: every implementation must run inference entirely on-device. Text passed to [embed]
 * never leaves the device; only [EmbedModelManager]'s *model bytes* are network-fetched, never
 * user text.
 */
interface TextEmbedder {
    /** null = the model isn't ready yet (pack not downloaded, runtime unavailable, or
     *  inference failed) — callers treat this as "stay on Tier-1", never as an error. */
    suspend fun embed(text: String): FloatArray?
}
