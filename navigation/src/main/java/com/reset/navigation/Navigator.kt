package com.reset.navigation

import kotlinx.coroutines.flow.Flow

/** One-shot navigation intents emitted by features and applied by the host. */
sealed interface NavEvent {
    data class Navigate(val screen: Screen, val singleTop: Boolean = true) : NavEvent

    /**
     * Bottom-bar tab change: shows [screen] as a top-level destination with the standard
     * dashboard semantics — the back stack is popped to the start destination (its state
     * saved), the tab is single-top, and a previously visited tab restores its state.
     */
    data class SwitchTab(val screen: Screen) : NavEvent
    data object Pop : NavEvent
    /** Pop everything above [screen]; a no-op when it is already on top. */
    data class PopTo(val screen: Screen) : NavEvent
    data object Exit : NavEvent
}

/**
 * The navigation seam. Features (ViewModels) depend on this interface only — never on a
 * `NavController` or on each other. They emit events; a single host observes the [events]
 * stream (see `ObserveNavigation`) and performs the actual navigation.
 */
interface Navigator {
    val events: Flow<NavEvent>

    fun navigate(screen: Screen)
    fun switchTab(screen: Screen)
    fun pop()
    fun popTo(screen: Screen)
    fun exit()
}
