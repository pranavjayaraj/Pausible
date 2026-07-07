package com.reset.feature.home

import com.reset.model.domain.CelebrationEvent
import com.reset.model.domain.CelebrationStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** State-backed like the real store, so tests can dispatch before the ViewModel collects. */
class FakeCelebrationStore : CelebrationStore {
    private val _pending = MutableStateFlow<CelebrationEvent?>(null)
    override val pending: StateFlow<CelebrationEvent?> = _pending.asStateFlow()

    val consumed = mutableListOf<CelebrationEvent>()

    override fun dispatch(event: CelebrationEvent) {
        _pending.value = event
    }

    override fun consume(event: CelebrationEvent) {
        consumed += event
        _pending.compareAndSet(event, null)
    }
}
