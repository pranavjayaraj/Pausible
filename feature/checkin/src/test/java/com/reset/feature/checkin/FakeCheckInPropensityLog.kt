package com.reset.feature.checkin

import com.reset.model.domain.checkin.CheckInPropensityLog
import com.reset.model.domain.checkin.InputMethod
import com.reset.model.domain.checkin.NeedState
import com.reset.model.domain.checkin.RecentCompletion
import com.reset.model.domain.checkin.SessionSelection

/** In-memory [CheckInPropensityLog] for Check In ViewModel tests. */
class FakeCheckInPropensityLog(
    private val recentCompletionsToReturn: List<RecentCompletion> = emptyList(),
) : CheckInPropensityLog {

    data class LoggedSelection(val needState: NeedState, val inputMethod: InputMethod, val selection: SessionSelection, val nowMs: Long)

    val logged = mutableListOf<LoggedSelection>()
    val alternateTaken = mutableListOf<Long>()
    val completed = mutableListOf<Triple<Long, String, Long>>()
    private var nextId = 100L

    override suspend fun logSelection(
        needState: NeedState,
        inputMethod: InputMethod,
        selection: SessionSelection,
        nowMs: Long,
    ): Long {
        val id = nextId++
        logged += LoggedSelection(needState, inputMethod, selection, nowMs)
        return id
    }

    override suspend fun recordAlternateTaken(rowId: Long) {
        alternateTaken += rowId
    }

    override suspend fun recordCompleted(rowId: Long, scriptId: String, nowMs: Long) {
        completed += Triple(rowId, scriptId, nowMs)
    }

    override suspend fun recentCompletions(sinceMs: Long): List<RecentCompletion> = recentCompletionsToReturn
}
