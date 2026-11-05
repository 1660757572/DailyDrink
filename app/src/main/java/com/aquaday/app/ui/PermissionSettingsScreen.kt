package com.aquaday.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aquaday.app.R
import com.aquaday.app.ui.components.AquaCard
import com.aquaday.app.ui.components.AquaDivider
import com.aquaday.app.ui.components.AquaRow
import com.itos.coui.theme.CouiTheme

@Composable
fun PermissionSettingsScreen(state: AquaUiState, viewModel: AquaViewModel) {
    val context = LocalContext.current
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.refreshPermissions()
    }
    val systemSettingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refreshPermissions()
    }

    val notifications = state.permissions.notifications
    val exactAlarms = state.permissions.exactAlarms
    AquaCard {
        Column(Modifier.padding(vertical = 2.dp)) {
            AquaRow(
                title = stringResource(
                    if (notifications) R.string.setting_notification_granted else R.string.setting_notification_missing,
                ),
                summary = stringResource(
                    if (notifications) R.string.setting_notification_granted_summary else R.string.setting_notification_missing_summary,
                ),
                icon = Icons.Filled.NotificationsActive,
                iconTint = TintPermission,
                onClick = if (notifications) null else { {
                    if (Build.VERSION.SDK_INT >= 33) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        systemSettingsLauncher.launch(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                        )
                    }
                } },
                trailing = { PermissionActionText(notifications) },
            )
            AquaDivider()
            AquaRow(
                title = stringResource(
                    if (exactAlarms) R.string.setting_exact_alarm_granted else R.string.setting_exact_alarm_missing,
                ),
                summary = stringResource(
                    if (exactAlarms) R.string.setting_exact_alarm_granted_summary else R.string.setting_exact_alarm_missing_summary,
                ),
                icon = Icons.Filled.Tune,
                iconTint = TintAlarmPermission,
                onClick = if (exactAlarms) null else { {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        .setData(Uri.fromParts("package", context.packageName, null))
                    runCatching { systemSettingsLauncher.launch(intent) }.onFailure {
                        systemSettingsLauncher.launch(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.fromParts("package", context.packageName, null)),
                        )
                    }
                } },
                trailing = { PermissionActionText(exactAlarms) },
            )
        }
    }
}

@Composable
private fun PermissionActionText(granted: Boolean) {
    Text(
        text = stringResource(if (granted) R.string.permission_granted else R.string.action_grant),
        style = MaterialTheme.typography.labelLarge,
        color = if (granted) MaterialTheme.colorScheme.onSurfaceVariant else CouiTheme.colorScheme.accent,
    )
}
