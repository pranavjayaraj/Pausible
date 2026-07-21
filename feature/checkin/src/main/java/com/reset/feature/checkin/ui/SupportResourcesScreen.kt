package com.reset.feature.checkin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppDimens
import com.reset.core.designsystem.AppShapes
import com.reset.core.designsystem.AppType
import com.reset.core.designsystem.BackIcon
import com.reset.feature.checkin.R
import com.reset.feature.checkin.navigation.CheckInIntent

/**
 * The quiet, always-available "Need support now?" resources screen — Deliverable 7. This
 * app is never positioned as treatment or a crisis service; it only points toward one.
 */
@Composable
fun SupportResourcesScreen(
    onIntent: (CheckInIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(AppColors.surface)
            .statusBarsPadding()
            .padding(horizontal = AppDimens.screenPaddingH)
            .padding(top = 38.dp, bottom = 30.dp),
    ) {
        BackIcon(
            modifier = Modifier
                .size(20.dp)
                .clickable { onIntent(CheckInIntent.HandleBackPress) },
            tint = AppColors.inkMuted,
        )

        Text(
            text = stringResource(R.string.checkin_support_title),
            style = AppType.welcomeTitle,
            color = AppColors.ink,
            modifier = Modifier.padding(top = 26.dp),
        )
        Text(
            text = stringResource(R.string.checkin_support_body),
            style = AppType.body,
            color = AppColors.inkBody,
            modifier = Modifier.padding(top = 10.dp),
        )

        ResourceCard(
            title = stringResource(R.string.checkin_support_lifeline_title),
            body = stringResource(R.string.checkin_support_lifeline_body),
            modifier = Modifier.padding(top = 26.dp),
        )
        ResourceCard(
            title = stringResource(R.string.checkin_support_elsewhere_title),
            body = stringResource(R.string.checkin_support_elsewhere_body),
            modifier = Modifier.padding(top = 14.dp),
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = stringResource(R.string.checkin_support_back),
            style = AppType.skipLabel,
            color = AppColors.teal,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onIntent(CheckInIntent.HandleBackPress) },
        )
    }
}

@Composable
private fun ResourceCard(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(AppColors.surfaceWhite)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(text = title, style = AppType.sectionTitle, color = AppColors.ink)
        Text(text = body, style = AppType.body, color = AppColors.inkBody, modifier = Modifier.padding(top = 6.dp))
    }
}
