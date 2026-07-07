package com.reset.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.reset.app.ui.DashboardDestination
import com.reset.app.ui.DashboardScreen
import com.reset.app.ui.onboarding.OnboardingDestination
import com.reset.app.ui.onboarding.OnboardingRoute
import com.reset.core.designsystem.AppBackground
import com.reset.feature.builder.api.BuilderDestination
import com.reset.feature.builder.ui.BuilderRoute
import com.reset.feature.home.api.HomeDestination
import com.reset.feature.profile.api.ProfileDestination
import com.reset.feature.sessions.api.SessionDestination
import com.reset.feature.sessions.api.SessionsDestination
import com.reset.feature.sessions.ui.SessionRoute
import com.reset.model.domain.HomeRepository
import com.reset.model.domain.SoundController
import com.reset.model.domain.StartupState
import com.reset.navigation.Navigator
import com.reset.navigation.ObserveNavigation
import com.reset.navigation.Screen
import com.reset.navigation.TabHost
import com.reset.repository.notification.NotificationConstants
import com.reset.repository.notification.ReminderNotificationUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The single host Activity, structured as the recommended nested graph: the root [NavHost]
 * owns the full-screen destinations (session, onboarding, builder) and starts on
 * [DashboardDestination] — the parent screen hosting the tabbed graph and the bottom bar.
 * The bar is therefore never conditionally hidden; full-screen routes simply cover the
 * dashboard. Features emit events through the injected [Navigator] and [ObserveNavigation]
 * applies them here, routing tab events onto the dashboard's inner host. [SoundController]
 * is handed to the Session route so the feature can play its chimes without owning the
 * audio wiring.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var soundController: SoundController

    @Inject
    lateinit var reminderNotificationUtil: ReminderNotificationUtil

    @Inject
    lateinit var startupState: StartupState

    @Inject
    lateinit var homeRepository: HomeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must precede super.onCreate(): swaps Theme.ResetApp.Starting for the real theme.
        // The splash then stays up until the first feature screen has content (or errored),
        // so the user never sees the empty backdrop while DataStore loads.
        installSplashScreen().setKeepOnScreenCondition { !startupState.contentReady.value }
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // A reminder tap should land on its reset, not on the sign-in sheet; read before
        // handleReminderAction consumes the action.
        val launchedFromReminder = intent?.action == NotificationConstants.ACTION_START_RESET
        if (savedInstanceState == null) handleReminderAction(intent)
        setContent {
            MaterialTheme {
                val rootNavController = rememberNavController()
                // Hoisted here so the tab back stack survives while a full-screen route
                // covers the dashboard.
                val tabsNavController = rememberNavController()
                val tabHost = remember(tabsNavController) {
                    TabHost(tabsNavController, ::isDashboardTab)
                }
                ObserveNavigation(
                    navigator = navigator,
                    navController = rootNavController,
                    onExit = { finish() },
                    tabHost = tabHost,
                )

                // The onboarding (splash → sign-in) rides on top of the dashboard once per
                // fresh launch; rememberSaveable keeps rotation/process-death from
                // replaying it. Pushed through the Navigator so it shares the pipeline.
                var onboardingShown by rememberSaveable { mutableStateOf(launchedFromReminder) }
                LaunchedEffect(Unit) {
                    if (!onboardingShown) {
                        onboardingShown = true
                        navigator.navigate(OnboardingDestination)
                    }
                }

                AppBackground {
                    NavHost(
                        navController = rootNavController,
                        startDestination = DashboardDestination,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        composable<DashboardDestination> {
                            DashboardScreen(
                                tabsNavController = tabsNavController,
                                onSelectTab = navigator::switchTab,
                            )
                        }

                        composable<SessionDestination> { SessionRoute(soundController) }
                        
                        composable<BuilderDestination> { BuilderRoute() }

                        composable<OnboardingDestination> {
                            // On a fresh launch onboarding covers the dashboard before
                            // Home's load ever runs, so it owns the readiness signal:
                            // release the system splash onto the brand splash.
                            LaunchedEffect(Unit) { startupState.markContentReady() }
                            OnboardingRoute(onDone = navigator::pop)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleReminderAction(intent)
    }

    /**
     * Translates the reminder's "Reset" action into a deep-link navigation sequence.
     * The notification is cleared and the navigator is commanded to switch to the Home tab
     * and start a focus session with the saved duration preference.
     *
     * Guarded against stale re-delivery: recents relaunches are skipped, and the action is
     * consumed (nulled) after dispatch so the retained intent can never replay it.
     */
    private fun handleReminderAction(intent: Intent?) {
        if (intent == null) return
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return
        if (intent.action != NotificationConstants.ACTION_START_RESET) return
        
        reminderNotificationUtil.cancelReminderNotification()
        
        lifecycleScope.launch {
            // Guarantee the underlying tab is Home before pushing a full screen on top
            navigator.switchTab(HomeDestination)
            val prefs = homeRepository.preferences.first()
            navigator.navigate(
                SessionDestination(
                    mode = SessionDestination.MODE_FOCUS,
                    durationMin = prefs.durationMin,
                )
            )
        }
        
        intent.action = null
    }
}

/** The destinations living in the dashboard's inner tabbed graph. */
private fun isDashboardTab(screen: Screen): Boolean =
    screen is HomeDestination || screen is SessionsDestination || screen is ProfileDestination
