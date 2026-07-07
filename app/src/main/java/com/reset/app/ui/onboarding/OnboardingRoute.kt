package com.reset.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/** How long the brand splash holds before advancing on its own, like the design. */
private const val SPLASH_HOLD_MS = 2_800L

/** The onboarding flow's two steps — an intra-route state change, not navigation. */
private enum class OnboardingStep { Splash, SignIn }

/**
 * App-owned onboarding: the brand splash that auto-advances (or taps through) to the
 * sign-in sheet. There is no real auth yet, so every sign-in choice — Apple, Google,
 * email, Skip, or the close glyph — resolves to [onDone]; the host pops the route away.
 */
@Composable
fun OnboardingRoute(onDone: () -> Unit) {
    var step by rememberSaveable { mutableStateOf(OnboardingStep.Splash) }

    LaunchedEffect(step) {
        if (step == OnboardingStep.Splash) {
            delay(SPLASH_HOLD_MS)
            step = OnboardingStep.SignIn
        }
    }

    // Back never re-opens the splash; it dismisses onboarding like the ✕ / Skip actions.
    BackHandler { onDone() }

    when (step) {
        OnboardingStep.Splash -> OnboardingSplashScreen(onTap = { step = OnboardingStep.SignIn })
        OnboardingStep.SignIn -> OnboardingSignInScreen(onSignIn = onDone)
    }
}
