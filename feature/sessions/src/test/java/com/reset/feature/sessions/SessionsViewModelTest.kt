package com.reset.feature.sessions

import androidx.lifecycle.SavedStateHandle
import com.reset.feature.home.api.HomeDestination
import com.reset.feature.sessions.api.SessionDestination
import com.reset.feature.sessions.navigation.SessionsIntent
import com.reset.navigation.NavEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.orbitmvi.orbit.test.test

class SessionsViewModelTest {

    private fun viewModel(navigator: FakeNavigator = FakeNavigator()) =
        SessionsViewModel(SavedStateHandle(), navigator)

    @Test
    fun `picking a break opens the session experience with its kind`() = runTest {
        val navigator = FakeNavigator()

        viewModel(navigator).test(this) {
            expectInitialState()
            containerHost.handleSessionsIntent(
                SessionsIntent.PickBreak(SessionDestination.KIND_STRETCH),
            )

            val event = navigator.events.first() as NavEvent.Navigate
            val destination = event.screen as SessionDestination
            assertEquals(SessionDestination.MODE_BREAK, destination.mode)
            assertEquals(SessionDestination.KIND_STRETCH, destination.breakKind)

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `skipping the break returns to the home tab`() = runTest {
        val navigator = FakeNavigator()

        viewModel(navigator).test(this) {
            expectInitialState()
            containerHost.handleSessionsIntent(SessionsIntent.SkipBreak)

            assertEquals(NavEvent.SwitchTab(HomeDestination), navigator.events.first())

            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun `back press pops via the navigator`() = runTest {
        val navigator = FakeNavigator()

        viewModel(navigator).test(this) {
            expectInitialState()
            containerHost.handleSessionsIntent(SessionsIntent.HandleBackPress)

            assertEquals(NavEvent.Pop, navigator.events.first())

            cancelAndIgnoreRemainingItems()
        }
    }
}
