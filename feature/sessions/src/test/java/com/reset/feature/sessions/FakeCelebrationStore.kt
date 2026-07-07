package com.reset.feature.sessions

import com.reset.model.domain.CelebrationEvent
import com.reset.model.domain.CelebrationStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** State-backed like the real store, recording dispatches for assertions. */
class FakeCelebrationStore : CelebrationStore {
    private val _pending = MutableStateFlow<CelebrationEvent?>(null)
    override val pending: StateFlow<CelebrationEvent?> = _pending.asStateFlow()

    val dispatched = mutableListOf<CelebrationEvent>()

    override fun dispatch(event: CelebrationEvent) {
        dispatched += event
        _pending.value = event
    }

    override fun consume(event: CelebrationEvent) {
        _pending.compareAndSet(event, null)
    }
}
