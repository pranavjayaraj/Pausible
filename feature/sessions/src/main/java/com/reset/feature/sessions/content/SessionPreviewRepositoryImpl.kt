package com.reset.feature.sessions.content

import com.reset.feature.sessions.utils.SessionCopySeed
import com.reset.model.domain.checkin.SessionPreview
import com.reset.model.domain.checkin.SessionPreviewRepository
import com.reset.model.domain.stats.StatsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** [SessionPreviewRepository] backed by the catalog — the same copy-rotation inputs the
 *  session runtime itself uses, so the offer's line and the session's opening line agree. */
class SessionPreviewRepositoryImpl @Inject constructor(
    private val statsRepository: StatsRepository,
) : SessionPreviewRepository {

    override suspend fun preview(scriptId: String): SessionPreview? {
        val script = SessionScripts.byId(scriptId) ?: return null
        val seed = SessionCopySeed.today()
        val completedCount = statsRepository.stats.first().breaksTaken
        return SessionPreview(
            scriptId = script.id,
            displayName = script.displayName,
            totalSec = script.totalSec,
            whyThisOne = script.arrivalDefault.pick(seed, completedCount),
            evidenceTag = evidenceTagFor(script.evidence.grade),
        )
    }

    private fun evidenceTagFor(grade: Int): String = when {
        grade >= 5 -> "backed by clinical trials"
        grade == 4 -> "backed by research"
        grade == 3 -> "grounded in the science"
        else -> "worth trying"
    }
}
