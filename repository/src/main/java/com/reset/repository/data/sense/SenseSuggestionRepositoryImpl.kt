package com.reset.repository.data.sense

import com.reset.model.domain.TimeProvider
import com.reset.model.domain.sense.SenseSuggestionRepository
import com.reset.sense.store.SenseDecisionLog
import javax.inject.Inject

/** [SenseSuggestionRepository] backed by the Sense decision log. */
class SenseSuggestionRepositoryImpl @Inject constructor(
    private val decisionLog: SenseDecisionLog,
    private val timeProvider: TimeProvider,
) : SenseSuggestionRepository {

    override suspend fun pendingSittingStretchDecisionId(): Long? =
        decisionLog.pendingSittingStretchDecisionId(timeProvider.nowMillis())

    override suspend fun dismissSittingStretchPrompt(decisionId: Long) {
        decisionLog.recordDismissed(decisionId, timeProvider.nowMillis())
    }
}
