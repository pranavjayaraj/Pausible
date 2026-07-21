package com.reset.feature.sessions.content

import com.reset.model.domain.checkin.NeedState
import com.reset.model.domain.checkin.SelectionContext
import com.reset.model.domain.checkin.SessionSelection
import com.reset.model.domain.checkin.SessionSelectorRepository
import javax.inject.Inject

/** [SessionSelectorRepository] backed by the catalog's pure [SessionSelector]. */
class SessionSelectorRepositoryImpl @Inject constructor() : SessionSelectorRepository {
    override fun select(needState: NeedState, context: SelectionContext): SessionSelection =
        SessionSelector.select(needState, context)
}
