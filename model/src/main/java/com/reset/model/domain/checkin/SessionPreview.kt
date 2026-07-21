package com.reset.model.domain.checkin

/**
 * Just enough catalog content to render the offer screen — never the full session runtime
 * type. A second, thin seam alongside [SessionSelectorRepository]: the offer UI needs a
 * name, a duration, a warm line, and an evidence tag, but still must not import
 * `:feature:sessions`' impl-only catalog types to get them.
 */
data class SessionPreview(
    val scriptId: String,
    val displayName: String,
    val totalSec: Int,
    /** One warm, context-aware line — the same pool the session's own Arrival act draws
     *  from, so the offer's promise and the session's opening line agree. */
    val whyThisOne: String,
    /** A short, honest tag derived from the evidence grade (e.g. "backed by clinical
     *  trials") — never bespoke marketing copy per script. */
    val evidenceTag: String,
)

/** Cross-feature seam for offer-screen content — see [SessionPreview]. */
interface SessionPreviewRepository {
    suspend fun preview(scriptId: String): SessionPreview?
}
