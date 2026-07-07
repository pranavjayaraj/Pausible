package com.reset.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.reset.core.designsystem.AppColors
import com.reset.core.designsystem.AppType
import com.reset.feature.home.R

private val errorSpacing = 10.dp

/** Shown when the initial prefs/stats load fails; retry re-runs it. */
@Composable
fun ErrorScreen(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(errorSpacing),
        ) {
            Text(
                text = stringResource(R.string.home_error_title),
                style = AppType.errorTitle,
                color = AppColors.ink,
            )
            TextButton(onClick = onRetry) {
                Text(
                    text = stringResource(R.string.home_retry),
                    style = AppType.button,
                    color = AppColors.accentDark,
                )
            }
        }
    }
}
