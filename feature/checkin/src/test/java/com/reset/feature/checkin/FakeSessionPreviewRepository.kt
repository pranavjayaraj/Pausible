package com.reset.feature.checkin

import com.reset.model.domain.checkin.SessionPreview
import com.reset.model.domain.checkin.SessionPreviewRepository

/** Synthesizes a plausible preview for any scriptId — Check In ViewModel tests only need the
 *  round trip to work, not real catalog content. */
class FakeSessionPreviewRepository : SessionPreviewRepository {
    override suspend fun preview(scriptId: String): SessionPreview = SessionPreview(
        scriptId = scriptId,
        displayName = scriptId.replaceFirstChar { it.uppercase() },
        totalSec = 60,
        whyThisOne = "why $scriptId",
        evidenceTag = "backed by research",
    )
}
