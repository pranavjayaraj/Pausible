package com.reset.app.ui.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.reset.app.R
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppDimens
import com.reset.core.designsystem.AppShapes
import com.reset.core.designsystem.AppType
import com.reset.core.designsystem.AppleIcon
import com.reset.core.designsystem.CloseIcon
import com.reset.core.designsystem.SproutMascot

private val splashHaloSize = 170.dp
private val splashMascotSize = 160.dp
private val splashTitleSpacing = 26.dp
private val splashSubtitleSpacing = 6.dp

private val signInPaddingH = 26.dp
private val signInHeaderPaddingTop = 22.dp
private val signInMascotBadgeSize = 96.dp
private val signInMascotSize = 62.dp
private val signInHeroSpacing = 34.dp
private val signInTitleSpacing = 26.dp
private val signInSubtitleSpacing = 14.dp
private val signInTitleMaxWidth = 260.dp
private val signInSubtitleMaxWidth = 280.dp
private val signInButtonsSpacing = 40.dp
private val signInButtonGap = 12.dp
private val signInButtonIconGap = 9.dp
private val appleGlyphSize = 15.dp
private val googleBadgeSize = 20.dp
private val dividerGap = 12.dp
private val dividerPaddingV = 8.dp
private val hairlineHeight = 1.dp
private val legalPaddingTop = 18.dp
private val closeIconSize = 20.dp

private const val HALO_PERIOD_MS = 4_000
private const val HALO_SCALE_MAX = 1.25f
private const val HALO_ALPHA = 0.3f

/**
 * The brand splash: Sprout breathing inside a soft halo over the deep-teal backdrop.
 * Tapping anywhere advances; [OnboardingRoute] also auto-advances after a short hold.
 */
@Composable
fun OnboardingSplashScreen(onTap: () -> Unit, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "splashHalo")
    val haloScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = HALO_SCALE_MAX,
        animationSpec = infiniteRepeatable(tween(HALO_PERIOD_MS), RepeatMode.Restart),
        label = "haloScale",
    )
    val haloAlpha by transition.animateFloat(
        initialValue = HALO_ALPHA,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(HALO_PERIOD_MS), RepeatMode.Restart),
        label = "haloAlpha",
    )

    Box(
        modifier
            .fillMaxSize()
            .background(AppColors.teal)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(splashHaloSize)
                        .scale(haloScale)
                        .alpha(haloAlpha)
                        .background(AppColors.sproutBody, CircleShape),
                )
                SproutMascot(modifier = Modifier.size(splashMascotSize))
            }
            Text(
                text = stringResource(R.string.onboarding_splash_title),
                style = AppType.displayTitle,
                color = AppColors.textOnDark,
                modifier = Modifier.padding(top = splashTitleSpacing),
            )
            Text(
                text = stringResource(R.string.onboarding_splash_tagline),
                style = AppType.bodySmall,
                color = AppColors.textOnDarkMuted,
                modifier = Modifier.padding(top = splashSubtitleSpacing),
            )
        }
    }
}

/**
 * The sign-in sheet. Purely presentational: every choice funnels into [onSignIn] because
 * there is no auth backend yet — the copy and layout mirror the design exactly.
 */
@Composable
fun OnboardingSignInScreen(onSignIn: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .background(AppColors.surfaceWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = signInPaddingH),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = signInHeaderPaddingTop),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CloseIcon(
                modifier = Modifier
                    .size(closeIconSize)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSignIn,
                    ),
            )
            Text(
                text = stringResource(R.string.onboarding_skip),
                style = AppType.toggleLabel,
                color = AppColors.ink,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSignIn,
                ),
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = signInHeroSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(signInMascotBadgeSize)
                    .background(AppColors.sproutBody, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                SproutMascot(
                    modifier = Modifier.size(signInMascotSize),
                    bodyColor = AppColors.sproutBodyCream,
                    showCheeks = false,
                )
            }
            Text(
                text = stringResource(R.string.onboarding_welcome_title),
                style = AppType.welcomeTitle,
                color = AppColors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = signInTitleSpacing)
                    .widthIn(max = signInTitleMaxWidth),
            )
            Text(
                text = stringResource(R.string.onboarding_welcome_subtitle),
                style = AppType.body,
                color = AppColors.inkBody,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = signInSubtitleSpacing)
                    .widthIn(max = signInSubtitleMaxWidth),
            )
        }

        Column(
            Modifier.padding(top = signInButtonsSpacing),
            verticalArrangement = Arrangement.spacedBy(signInButtonGap),
        ) {
            SignInButton(
                background = AppColors.ink,
                contentColor = AppColors.textOnDark,
                label = stringResource(R.string.onboarding_sign_in_apple),
                onClick = onSignIn,
            ) {
                AppleIcon(Modifier.size(appleGlyphSize), tint = AppColors.textOnDark)
            }
            SignInButton(
                background = AppColors.googleButton,
                contentColor = AppColors.ink,
                label = stringResource(R.string.onboarding_sign_in_google),
                onClick = onSignIn,
            ) {
                Box(
                    Modifier
                        .size(googleBadgeSize)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_google_glyph),
                        style = AppType.buttonTiny,
                        color = AppColors.googleG,
                    )
                }
            }
            OrUseEmailDivider()
            SignInButton(
                background = AppColors.teal,
                contentColor = AppColors.textOnDark,
                label = stringResource(R.string.onboarding_sign_in_email),
                onClick = onSignIn,
            )
        }

        Spacer(Modifier.weight(1f))
        LegalFooter(Modifier.padding(top = legalPaddingTop, bottom = signInHeaderPaddingTop))
    }
}

@Composable
private fun SignInButton(
    background: Color,
    contentColor: Color,
    label: String,
    onClick: () -> Unit,
    icon: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(AppDimens.buttonHeight)
            .clip(AppShapes.button)
            .background(background)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(signInButtonIconGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.invoke()
        Text(text = label, style = AppType.button, color = contentColor)
    }
}

@Composable
private fun OrUseEmailDivider() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = dividerPaddingV),
        horizontalArrangement = Arrangement.spacedBy(dividerGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Hairline(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.onboarding_or_email),
            style = AppType.dividerLabel,
            color = AppColors.inkFaint,
        )
        Hairline(Modifier.weight(1f))
    }
}

@Composable
private fun Hairline(modifier: Modifier) {
    Box(
        modifier
            .height(hairlineHeight)
            .background(AppColors.divider),
    )
}

@Composable
private fun LegalFooter(modifier: Modifier = Modifier) {
    val emphasis = SpanStyle(color = AppColors.accentDark, fontWeight = FontWeight.ExtraBold)
    val text = buildAnnotatedString {
        append(stringResource(R.string.onboarding_legal_prefix))
        withStyle(emphasis) { append(stringResource(R.string.onboarding_legal_terms)) }
        append(stringResource(R.string.onboarding_legal_and))
        withStyle(emphasis) { append(stringResource(R.string.onboarding_legal_privacy)) }
        append(stringResource(R.string.onboarding_legal_suffix))
    }
    Text(
        text = text,
        style = AppType.legal,
        color = AppColors.inkFaint,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}
