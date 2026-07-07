package com.reset.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * The dashboard's inner navigation host, when the graph is nested per the recommended
 * pattern: the root host owns full-screen destinations and its start destination is a
 * parent screen hosting the tabbed graph (and the bottom bar). [isTab] tells the
 * collector which destinations live in this inner host.
 */
class TabHost(
    val navController: NavController,
    val isTab: (Screen) -> Boolean,
)

/**
 * Hosts navigation for the app: collects [Navigator.events] while the host is at least
 * STARTED and applies each to the real controllers. This is the single place that knows
 * about a `NavController`; features stay unaware of it.
 *
 * With a nested graph, [navController] is the root host (full-screen destinations) and
 * [tabHost] the dashboard's inner host: tab events land on the inner graph after the root
 * is popped back to the dashboard, pops drain the root before the tabs, and everything
 * else applies to the root. A flat graph simply omits [tabHost].
 *
 * @param onExit invoked when there is nothing left to pop (back out of the app).
 */
@Composable
fun ObserveNavigation(
    navigator: Navigator,
    navController: NavController,
    onExit: () -> Unit,
    tabHost: TabHost? = null,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(navigator, navController, tabHost) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            navigator.events.collect { event ->
                when (event) {
                    is NavEvent.Navigate -> navController.navigate(event.screen) {
                        launchSingleTop = event.singleTop
                    }

                    is NavEvent.SwitchTab -> {
                        // A tab change always lands on the dashboard: clear any
                        // full-screen route off the root first, then apply the canonical
                        // bottom-bar behaviour to whichever host owns the tabs — one tab
                        // deep at a time, state preserved per tab, back always landing on
                        // the start destination.
                        val tabs = tabHost?.navController ?: navController
                        if (tabHost != null) popRootToDashboard(navController)
                        tabs.navigate(event.screen) {
                            popUpTo(tabs.graph.findStartDestination().id) {
                                    saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }

                    NavEvent.Pop -> {
                        // Drain the root (a full-screen route) before the tab stack.
                        val rootCanPop = navController.previousBackStackEntry != null
                        val popped = if (rootCanPop) {
                            navController.popBackStack()
                        } else {
                            tabHost?.navController?.popBackStack() == true
                        }
                        if (!popped) onExit()
                    }

                    is NavEvent.PopTo ->
                        if (tabHost != null && tabHost.isTab(event.screen)) {
                            popRootToDashboard(navController)
                            tabHost.navController.popBackStack(event.screen, inclusive = false)
                        } else {
                            navController.popBackStack(event.screen, inclusive = false)
                        }

                    NavEvent.Exit -> onExit()
                }
            }
        }
    }
}

/** Pops every full-screen route off the root, back to its dashboard start destination. */
private fun popRootToDashboard(root: NavController) {
    root.popBackStack(root.graph.findStartDestination().id, inclusive = false)
}
