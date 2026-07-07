package com.reset.feature.profile

import org.orbitmvi.orbit.test.OrbitTestContext

/**
 * Awaits successive states until [predicate] holds, returning that state — lets tests
 * ignore intermediate emissions without depending on how many reductions occurred.
 */
suspend fun OrbitTestContext<ProfileState, *, *>.awaitUntil(
    predicate: (ProfileState) -> Boolean,
): ProfileState {
    var state = awaitState()
    while (!predicate(state)) {
        state = awaitState()
    }
    return state
}
