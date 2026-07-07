package com.reset.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.reset.feature.home.api.HomeDestination
import com.reset.feature.home.ui.HomeRoute
import com.reset.feature.profile.api.ProfileDestination
import com.reset.feature.profile.ui.ProfileRoute
import com.reset.feature.sessions.api.SessionsDestination
import com.reset.feature.sessions.ui.SessionsRoute
import com.reset.navigation.Screen

/**
 * The parent screen hosting the tabbed destinations, per the nested-graph pattern: the
 * bottom bar lives here and only here, so it never needs conditional hiding — full-screen
 * routes (session, onboarding, builder) sit on the root host *above* this screen and simply cover
 * it. [tabsNavController] is hoisted to the Activity so the tab back stack survives while
 * a full-screen route is on top.
 */
@Composable
fun DashboardScreen(
    tabsNavController: NavHostController,
    onSelectTab: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        NavHost(
            navController = tabsNavController,
            startDestination = HomeDestination,
            modifier = Modifier.weight(1f),
        ) {
            composable<HomeDestination> { HomeRoute() }

            composable<SessionsDestination> { SessionsRoute() }

            composable<ProfileDestination> { ProfileRoute() }
        }

        // Taps enter the Navigator seam as SwitchTab events; ObserveNavigation applies
        // the tab semantics onto this screen's inner graph.
        ResetBottomBar(
            navController = tabsNavController,
            onSelectTab = onSelectTab,
        )
    }
}
