package com.aquaday.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aquaday.app.R
import com.aquaday.app.data.ThemeMode
import com.aquaday.app.ui.components.AquaCard
import com.aquaday.app.ui.components.AquaDivider
import com.aquaday.app.ui.components.AquaRow
import com.aquaday.app.ui.components.AquaSectionTitle
import com.aquaday.app.ui.components.AquaSliderRow
import com.itos.coui.dialog.CouiAlertDialog
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults

@Composable
fun MineScreen(
    state: AquaUiState,
    viewModel: AquaViewModel,
    onOpenReminder: () -> Unit,
    onOpenPermission: () -> Unit,
    onOpenAppearance: () -> Unit,
) {
    val metrics = state.metrics ?: return
    val snapshot = metrics.snapshot
    val settings = snapshot.settings
    var goalDraft by remember(snapshot.goalMl) { mutableFloatStateOf(snapshot.goalMl.toFloat()) }
    var cupDraft by remember(settings.defaultCupMl) { mutableFloatStateOf(settings.defaultCupMl.toFloat()) }
    var showReset by remember { mutableStateOf(false) }

    AquaSectionTitle(stringResource(R.string.mine_section_target))
    AquaCard {
        AquaSliderRow(
            title = stringResource(R.string.setting_goal),
            value = goalDraft,
            onValueChange = { goalDraft = it },
            onValueChangeFinished = { viewModel.setGoal(goalDraft.roundToStep(100)) },
            valueRange = 800f..5000f,
            steps = 41,
            valueText = "${goalDraft.roundToStep(100)} ml",
            summary = stringResource(R.string.setting_goal_summary, snapshot.goalMl),
        )
        AquaDivider()
        AquaSliderRow(
            title = stringResource(R.string.setting_default_cup),
            value = cupDraft,
            onValueChange = { cupDraft = it },
            onValueChangeFinished = { viewModel.updateSettings { it.copy(defaultCupMl = cupDraft.roundToStep(50)) } },
            valueRange = 100f..600f,
            steps = 9,
            valueText = "${cupDraft.roundToStep(50)} ml",
            summary = stringResource(R.string.setting_default_cup_summary),
        )
    }

    Spacer(Modifier.height(16.dp))
    AquaSectionTitle(stringResource(R.string.mine_section_settings))
    AquaCard {
        Column(Modifier.padding(vertical = 2.dp)) {
            AquaRow(
                title = stringResource(R.string.mine_subpage_reminder),
                summary = stringResource(
                    R.string.mine_reminder_summary,
                    settings.intervalMinutes,
                    hourLabel(settings.activeStartHour),
                    hourLabel(settings.activeEndHour),
                ),
                icon = Icons.Filled.Alarm,
                iconTint = TintReminder,
                onClick = onOpenReminder,
                trailing = { Chevron() },
            )
            AquaDivider()
            AquaRow(
                title = stringResource(R.string.mine_subpage_permission),
                summary = if (state.permissions.notifications && state.permissions.exactAlarms) {
                    stringResource(R.string.mine_permission_summary_ready)
                } else {
                    stringResource(
                        R.string.mine_permission_summary_missing,
                        listOf(state.permissions.notifications, state.permissions.exactAlarms).count { !it },
                    )
                },
                icon = Icons.Filled.NotificationsActive,
                iconTint = TintPermission,
                onClick = onOpenPermission,
                trailing = { Chevron() },
            )
            AquaDivider()
            AquaRow(
                title = stringResource(R.string.mine_subpage_appearance),
                summary = appearanceSummary(snapshot.themeMode, snapshot.dynamicColor, snapshot.backgroundImage != null),
                icon = Icons.Filled.DarkMode,
                iconTint = TintTheme,
                onClick = onOpenAppearance,
                trailing = { Chevron() },
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    AquaSectionTitle(stringResource(R.string.mine_section_data))
    AquaCard {
        Column(Modifier.padding(vertical = 2.dp)) {
            AquaRow(
                title = stringResource(R.string.setting_reset),
                summary = stringResource(R.string.setting_reset_summary),
                icon = Icons.Filled.RestartAlt,
                iconTint = TintReset,
                onClick = { showReset = true },
                trailing = {
                    Text(
                        text = stringResource(R.string.action_confirm),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                },
            )
            AquaDivider()
            AquaRow(
                title = stringResource(R.string.setting_about),
                summary = stringResource(R.string.setting_about_summary),
                icon = Icons.Filled.Info,
                iconTint = TintAbout,
            )
        }
    }
    Spacer(Modifier.height(20.dp))
    Text(
        text = stringResource(R.string.mine_footer, snapshot.goalMl, metrics.eventCount),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    if (showReset) {
        CouiAlertDialog(
            onDismissRequest = { showReset = false },
            title = stringResource(R.string.setting_reset),
            message = stringResource(R.string.setting_reset_confirm),
            confirmAction = CouiDialogAction(
                text = stringResource(R.string.action_confirm),
                onClick = {
                    showReset = false
                    viewModel.resetAll()
                },
                style = CouiDialogActionStyle.Primary,
                colors = CouiDialogDefaults.primaryActionColors(MaterialTheme.colorScheme.error),
            ),
            dismissAction = CouiDialogAction(text = stringResource(R.string.action_cancel), onClick = { showReset = false }),
        )
    }
}

@Composable
private fun Chevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    )
}

@Composable
private fun appearanceSummary(mode: ThemeMode, dynamicColor: Boolean, hasBackground: Boolean): String {
    val parts = buildList {
        add(
            stringResource(
                when (mode) {
                    ThemeMode.System -> R.string.setting_theme_system
                    ThemeMode.Light -> R.string.setting_theme_light
                    ThemeMode.Dark -> R.string.setting_theme_dark
                },
            ),
        )
        if (dynamicColor) add(stringResource(R.string.mine_appearance_dynamic))
        if (hasBackground) add(stringResource(R.string.mine_appearance_background))
    }
    return parts.joinToString(" · ")
}
