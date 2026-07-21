package com.reset.feature.checkin

import com.reset.model.domain.checkin.GateOutcome
import com.reset.model.domain.checkin.NeedState
import com.reset.model.domain.checkin.SelectionContext
import com.reset.model.domain.checkin.SessionSelection
import com.reset.model.domain.checkin.SessionSelectorRepository

/** Deterministic [SessionSelectorRepository] for Check In ViewModel tests — echoes back a
 *  fixed primary/alternate per [NeedState] instead of running the real catalog logic
 *  (that logic is [com.reset.feature.sessions.content.SessionSelector]'s own test suite). */
class FakeSessionSelectorRepository : SessionSelectorRepository {

    val calls = mutableListOf<Pair<NeedState, SelectionContext>>()

    override fun select(needState: NeedState, context: SelectionContext): SessionSelection {
        calls += needState to context
        val primary = "primary_${needState.name.lowercase()}"
        val alternate = "alternate_${needState.name.lowercase()}"
        return SessionSelection(
            primaryScriptId = primary,
            alternateScriptId = alternate,
            shortlist = listOf(GateOutcome(primary, null), GateOutcome(alternate, null)),
        )
    }
}
