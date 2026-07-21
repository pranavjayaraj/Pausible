package com.reset.repository.data.checkin

import com.reset.model.domain.checkin.CheckInPropensityLog
import com.reset.model.domain.checkin.InputMethod
import com.reset.model.domain.checkin.NeedState
import com.reset.model.domain.checkin.RecentCompletion
import com.reset.model.domain.checkin.SessionSelection
import javax.inject.Inject

/** [CheckInPropensityLog] backed by Room. */
class CheckInPropensityLogImpl @Inject constructor(
    private val dao: CheckInPropensityDao,
) : CheckInPropensityLog {

    override suspend fun logSelection(
        needState: NeedState,
        inputMethod: InputMethod,
        selection: SessionSelection,
        nowMs: Long,
    ): Long = dao.insert(
        CheckInPropensityEntity(
            timestampMs = nowMs,
            needState = needState.name,
            inputMethod = inputMethod.name,
            offeredScriptId = selection.primaryScriptId,
            alternateScriptId = selection.alternateScriptId,
            shortlistTrace = selection.shortlist.joinToString(";") { outcome ->
                "${outcome.scriptId}:${outcome.reason?.name ?: "OK"}"
            },
        ),
    )

    override suspend fun recordAlternateTaken(rowId: Long) {
        dao.markAlternateTaken(rowId)
    }

    override suspend fun recordCompleted(rowId: Long, scriptId: String, nowMs: Long) {
        dao.markCompleted(rowId, scriptId, nowMs)
    }

    override suspend fun recentCompletions(sinceMs: Long): List<RecentCompletion> =
        dao.completedSince(sinceMs).mapNotNull { entity ->
            val scriptId = entity.completedScriptId ?: return@mapNotNull null
            val completedAtMs = entity.completedAtMs ?: return@mapNotNull null
            val needState = runCatching { NeedState.valueOf(entity.needState) }.getOrNull()
                ?: return@mapNotNull null
            RecentCompletion(scriptId, needState, completedAtMs)
        }
}
