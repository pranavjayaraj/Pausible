package com.reset.feature.mood

import org.orbitmvi.orbit.test.OrbitTestContext

/**
 * Awaits successive states until [predicate] holds, returning that state — lets tests
 * ignore intermediate emissions without depending on how many reductions occurred.
 */
suspend fun OrbitTestContext<MoodState, *, *>.awaitUntil(
    predicate: (MoodState) -> Boolean,
): MoodState {
    var state = awaitState()
    while (!predicate(state)) {
        state = awaitState()
    }
    return state
}
