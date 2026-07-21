package com.reset.feature.home

import com.reset.model.domain.sense.SenseSuggestionRepository

/** In-memory [SenseSuggestionRepository] for Home ViewModel tests. */
class FakeSenseSuggestionRepository(
    var pendingDecisionId: Long? = null,
) : SenseSuggestionRepository {

    val dismissed = mutableListOf<Long>()

    override suspend fun pendingSittingStretchDecisionId(): Long? = pendingDecisionId

    override suspend fun dismissSittingStretchPrompt(decisionId: Long) {
        dismissed += decisionId
        if (pendingDecisionId == decisionId) pendingDecisionId = null
    }
}
