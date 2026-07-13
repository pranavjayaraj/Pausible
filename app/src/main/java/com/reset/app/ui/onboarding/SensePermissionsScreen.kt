package com.reset.app.ui.onboarding

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.reset.app.R
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppDimens
import com.reset.core.designsystem.AppShapes
import com.reset.core.designsystem.AppType
import com.reset.sense.signals.SensePermissions

private val permsPaddingH = 26.dp
private val permsTitleSpacing = 48.dp
private val permsSubtitleSpacing = 10.dp
private val permsCardsSpacing = 36.dp
private val permsCardGap = 14.dp
private val permsCardPadding = 18.dp
private val permsFootnoteSpacing = 18.dp

/**
 * Onboarding step for the Sense engine's three permissions. Never blocks:
 * Continue is always available and each grant is optional — the engine
 * degrades per-permission (see SenseAccuracyTier). Statuses re-derive on
 * every resume, which also covers the round-trip to the usage-access
 * Settings screen (a special app-op with no result contract).
 */
@Composable
fun SensePermissionsScreen(
    onContinue: () -> Unit,
    onMotionGranted: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Bumped on resume; each bump re-reads all three statuses.
    var refresh by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationsGranted = remember(refresh) {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val usageGranted = remember(refresh) { SensePermissions.usageAccessGranted(context) }
    val motionGranted = remember(refresh) { SensePermissions.activityRecognitionGranted(context) }

    val notificationsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refresh++ }
    val motionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        refresh++
        if (granted) onMotionGranted() // re-arm transition updates immediately
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = permsPaddingH),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.sense_perms_title),
            style = AppType.welcomeTitle,
            color = AppColors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = permsTitleSpacing),
        )
        Text(
            text = stringResource(R.string.sense_perms_subtitle),
            style = AppType.body,
            color = AppColors.inkBody,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = permsSubtitleSpacing),
        )

        Column(
            Modifier.padding(top = permsCardsSpacing),
            verticalArrangement = Arrangement.spacedBy(permsCardGap),
        ) {
            PermissionCard(
                title = stringResource(R.string.sense_perms_notifications_title),
                why = stringResource(R.string.sense_perms_notifications_why),
                granted = notificationsGranted,
                onAllow = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            )
            PermissionCard(
                title = stringResource(R.string.sense_perms_usage_title),
                why = stringResource(R.string.sense_perms_usage_why),
                granted = usageGranted,
                onAllow = {
                    // Special app-op — grantable only from Settings; status
                    // is re-read on the resume that follows.
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                },
            )
            PermissionCard(
                title = stringResource(R.string.sense_perms_motion_title),
                why = stringResource(R.string.sense_perms_motion_why),
                granted = motionGranted,
                onAllow = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        motionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                    }
                },
            )
        }

        Text(
            text = stringResource(R.string.sense_perms_footnote),
            style = AppType.legal,
            color = AppColors.inkFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = permsFootnoteSpacing),
        )

        Spacer(Modifier.weight(1f))

        Row(
            Modifier
                .fillMaxWidth()
                .height(AppDimens.buttonHeight)
                .clip(AppShapes.button)
                .background(AppColors.teal)
                .clickable(onClick = onContinue),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.sense_perms_continue),
                style = AppType.button,
                color = AppColors.textOnDark,
            )
        }
        Spacer(Modifier.height(permsCardsSpacing))
    }
}

@Composable
private fun PermissionCard(
    title: String,
    why: String,
    granted: Boolean,
    onAllow: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(AppColors.surfaceWhite)
            .padding(permsCardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = AppType.breakCardTitle, color = AppColors.ink)
            Text(
                text = why,
                style = AppType.caption,
                color = AppColors.inkBody,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (granted) {
            Text(
                text = stringResource(R.string.sense_perms_granted),
                style = AppType.toggleLabel,
                color = AppColors.teal,
                modifier = Modifier.padding(start = permsCardPadding),
            )
        } else {
            Text(
                text = stringResource(R.string.sense_perms_allow),
                style = AppType.toggleLabel,
                color = AppColors.accentDark,
                modifier = Modifier
                    .padding(start = permsCardPadding)
                    .clickable(onClick = onAllow),
            )
        }
    }
}
