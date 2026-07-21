package com.reset.model.domain.checkin

/**
 * Cross-feature seam onto the session catalog's selector: callers outside `:feature:sessions`
 * (the Check In feature) get a [NeedState] in, a [SessionSelection] out — never the catalog's
 * own [com.reset.feature.sessions.content.SessionScript] types, so the impl module's content
 * package stays private to that module.
 */
interface SessionSelectorRepository {
    fun select(needState: NeedState, context: SelectionContext): SessionSelection
}
