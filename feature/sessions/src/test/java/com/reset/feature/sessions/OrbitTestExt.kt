package com.reset.feature.sessions

import org.orbitmvi.orbit.test.Item
import org.orbitmvi.orbit.test.OrbitTestContext

/**
 * Awaits successive items until a state matching [predicate] arrives, returning that
 * state — lets tests ignore intermediate emissions (including interleaved side effects)
 * without depending on how many reductions occurred.
 */
suspend fun <STATE : Any, SIDE_EFFECT : Any> OrbitTestContext<STATE, SIDE_EFFECT, *>.awaitUntil(
    predicate: (STATE) -> Boolean,
): STATE {
    while (true) {
        val item = awaitItem()
        if (item is Item.StateItem && predicate(item.value)) return item.value
    }
}

/** Awaits the next side effect, skipping over interleaved state emissions. */
suspend fun <STATE : Any, SIDE_EFFECT : Any> OrbitTestContext<STATE, SIDE_EFFECT, *>.awaitNextSideEffect(): SIDE_EFFECT {
    while (true) {
        val item = awaitItem()
        if (item is Item.SideEffectItem) return item.value
    }
}
