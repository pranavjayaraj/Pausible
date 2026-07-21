package com.reset.feature.sessions

import com.reset.model.domain.checkin.CheckInPropensityLog
import com.reset.model.domain.checkin.InputMethod
import com.reset.model.domain.checkin.NeedState
import com.reset.model.domain.checkin.RecentCompletion
import com.reset.model.domain.checkin.SessionSelection

/** In-memory [CheckInPropensityLog] for Session ViewModel tests. */
class FakeCheckInPropensityLog : CheckInPropensityLog {

    val completions = mutableListOf<Triple<Long, String, Long>>()
    private var nextId = 1L

    override suspend fun logSelection(
        needState: NeedState,
        inputMethod: InputMethod,
        selection: SessionSelection,
        nowMs: Long,
    ): Long = nextId++

    override suspend fun recordAlternateTaken(rowId: Long) = Unit

    override suspend fun recordCompleted(rowId: Long, scriptId: String, nowMs: Long) {
        completions += Triple(rowId, scriptId, nowMs)
    }

    override suspend fun recentCompletions(sinceMs: Long): List<RecentCompletion> = emptyList()
}
