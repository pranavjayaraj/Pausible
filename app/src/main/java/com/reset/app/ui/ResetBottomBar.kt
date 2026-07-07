package com.reset.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import com.reset.app.R
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppDimens
import com.reset.core.designsystem.AppType
import com.reset.core.designsystem.HomeIcon
import com.reset.core.designsystem.StatsIcon
import com.reset.core.designsystem.TimerIcon
import com.reset.feature.home.api.HomeDestination
import com.reset.feature.profile.api.ProfileDestination
import com.reset.feature.sessions.api.SessionsDestination
import com.reset.navigation.Screen

/**
 * The dashboard's three tabs. App-owned chrome: only the host knows the graph, so only it
 * may enumerate top-level destinations. Ordering here is the on-screen ordering.
 */
private enum class DashboardTab(@StringRes val labelRes: Int) {
    Home(R.string.tab_home),
    Sessions(R.string.tab_sessions),
    Profile(R.string.tab_profile);

    val destination: Screen
        get() = when (this) {
            Home -> HomeDestination
            Sessions -> SessionsDestination
            Profile -> ProfileDestination
        }
}

private val barTopBorder = 1.5.dp
private val barPaddingTop = 10.dp
private val barPaddingBottom = 16.dp
private val barItemIconSize = 22.dp
private val barLabelSpacing = 4.dp
private val activePillWidth = 34.dp
private val activePillHeight = 26.dp
private val activePillShape = RoundedCornerShape(13.dp)
private val timerPillWidth = 64.dp
private val timerPillHeight = 46.dp
private val timerPillShape = RoundedCornerShape(23.dp)
private val timerPillLift = 16.dp
private val timerPillElevation = 8.dp

/**
 * White dashboard bar hosting the app's three top-level tabs, per the design: Home and
 * Profile are icon-over-label buttons whose icon gains a soft teal pill when selected,
 * while the middle Sessions tab is the raised brown timer capsule floating over the bar.
 * Selection is derived from the [navController]'s current entry; taps flow through
 * [onSelectTab] into the Navigator seam so tab changes ride the same event stream as
 * every other navigation (single pipeline, loggable in ObserveNavigation).
 */
@Composable
fun ResetBottomBar(
    navController: NavController,
    onSelectTab: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val selected = when {
        backStackEntry?.destination?.hasRoute<SessionsDestination>() == true -> DashboardTab.Sessions
        backStackEntry?.destination?.hasRoute<ProfileDestination>() == true -> DashboardTab.Profile
        else -> DashboardTab.Home
    }

    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(barTopBorder)
                .background(AppColors.hairline),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .background(AppColors.surfaceWhite)
                .navigationBarsPadding()
                .padding(top = barPaddingTop, bottom = barPaddingBottom),
            verticalAlignment = Alignment.Bottom,
        ) {
            DashboardTab.entries.forEach { tab ->
                BarItem(
                    tab = tab,
                    isSelected = tab == selected,
                    onClick = { if (tab != selected) onSelectTab(tab.destination) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BarItem(
    tab: DashboardTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (isSelected) AppColors.statsTabInk else AppColors.inkFaint
    Column(
        modifier
            .heightIn(min = AppDimens.touchTargetMin)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(barLabelSpacing, Alignment.Bottom),
    ) {
        TabIcon(tab, tint, isSelected)
        Text(
            text = stringResource(tab.labelRes),
            style = AppType.tab,
            color = if (tab == DashboardTab.Sessions) AppColors.accentDark else tint,
        )
    }
}


@Composable
private fun TabIcon(tab: DashboardTab, tint: Color, isSelected: Boolean) {
    val iconModifier = Modifier.size(barItemIconSize)
    Box(
        Modifier
            .width(activePillWidth)
            .height(activePillHeight)
            .background(
                color = if (isSelected) AppColors.statsTabPill else Color.Transparent,
                shape = activePillShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (tab) {
            DashboardTab.Home -> HomeIcon(iconModifier, tint)
            DashboardTab.Profile -> StatsIcon(iconModifier, tint)
            DashboardTab.Sessions -> StatsIcon(iconModifier, tint)
        }
    }
}
