package com.reset.feature.checkin

import com.reset.navigation.NavEvent
import com.reset.navigation.Navigator
import com.reset.navigation.Screen
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/** Records navigation calls so ViewModel tests can assert navigate/pop/switchTab. */
class FakeNavigator : Navigator {
    private val _events = Channel<NavEvent>(Channel.BUFFERED)
    override val events: Flow<NavEvent> = _events.receiveAsFlow()

    val navigated = mutableListOf<Screen>()
    val switchedTabs = mutableListOf<Screen>()
    var popped = false
        private set

    override fun navigate(screen: Screen) {
        navigated += screen
        _events.trySend(NavEvent.Navigate(screen))
    }

    override fun switchTab(screen: Screen) {
        switchedTabs += screen
        _events.trySend(NavEvent.SwitchTab(screen))
    }

    override fun pop() {
        popped = true
        _events.trySend(NavEvent.Pop)
    }

    override fun popTo(screen: Screen) {
        _events.trySend(NavEvent.PopTo(screen))
    }

    override fun exit() {
        _events.trySend(NavEvent.Exit)
    }
}
