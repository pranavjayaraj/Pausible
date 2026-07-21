package com.reset.feature.checkin

import org.orbitmvi.orbit.test.Item
import org.orbitmvi.orbit.test.OrbitTestContext

/** Awaits successive items until a state matching [predicate] arrives. */
suspend fun <STATE : Any, SIDE_EFFECT : Any> OrbitTestContext<STATE, SIDE_EFFECT, *>.awaitUntil(
    predicate: (STATE) -> Boolean,
): STATE {
    while (true) {
        val item = awaitItem()
        if (item is Item.StateItem && predicate(item.value)) return item.value
    }
}
