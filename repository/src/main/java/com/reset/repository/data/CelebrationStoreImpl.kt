package com.reset.repository.data

import com.reset.model.domain.CelebrationEvent
import com.reset.model.domain.CelebrationStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton [CelebrationStore]: the event is plain state (not a queued emission), so the
 * Sessions feature can dispatch it just before navigating and the Home collector picks it
 * up whenever it (re)subscribes — same delivery contract as `ReminderActionStoreImpl`.
 */
@Singleton
class CelebrationStoreImpl @Inject constructor() : CelebrationStore {

    private val _pending = MutableStateFlow<CelebrationEvent?>(null)
    override val pending: StateFlow<CelebrationEvent?> = _pending.asStateFlow()

    override fun dispatch(event: CelebrationEvent) {
        _pending.value = event
    }

    override fun consume(event: CelebrationEvent) {
        _pending.compareAndSet(event, null)
    }
}
