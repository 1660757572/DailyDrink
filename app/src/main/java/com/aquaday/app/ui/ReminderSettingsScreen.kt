package com.aquaday.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aquaday.app.R
import com.aquaday.app.ui.components.AquaCard
import com.aquaday.app.ui.components.AquaDivider
import com.aquaday.app.ui.components.AquaSliderRow
import com.aquaday.app.ui.components.AquaStepperRow
import com.aquaday.app.ui.components.AquaSwitchRow
import java.util.Locale

@Composable
fun ReminderSettingsScreen(state: AquaUiState, viewModel: AquaViewModel) {
    val settings = state.metrics?.snapshot?.settings ?: return
    var intervalDraft by remember(settings.intervalMinutes) {
        mutableFloatStateOf(settings.intervalMinutes.toFloat())
    }

    AquaCard {
        Column(Modifier.padding(vertical = 2.dp)) {
            AquaSwitchRow(
                title = stringResource(R.string.setting_reminder_on),
                summary = stringResource(R.string.setting_reminder_summary),
                checked = settings.enabled,
                icon = Icons.Filled.Alarm,
                iconTint = TintReminder,
                onCheckedChange = { enabled -> viewModel.updateSettings { it.copy(enabled = enabled) } },
            )
            AquaDivider()
            AquaSliderRow(
                title = stringResource(R.string.setting_interval),
                value = intervalDraft,
                onValueChange = { intervalDraft = it },
                onValueChangeFinished = {
                    viewModel.updateSettings { it.copy(intervalMinutes = intervalDraft.roundToStep(5)) }
                },
                valueRange = 15f..240f,
                steps = 44,
                valueText = stringResource(R.string.setting_interval_summary, intervalDraft.roundToStep(5)),
            )
            AquaDivider()
            AquaStepperRow(
                title = stringResource(R.string.setting_window_start),
                value = hourLabel(settings.activeStartHour),
                onDecrease = {
                    viewModel.updateSettings { it.copy(activeStartHour = (it.activeStartHour - 1).coerceAtLeast(0)) }
                },
                onIncrease = {
                    viewModel.updateSettings { it.copy(activeStartHour = (it.activeStartHour + 1).coerceAtMost(23)) }
                },
                summary = stringResource(R.string.setting_window_summary, settings.activeStartHour, settings.activeEndHour),
                icon = Icons.Filled.Bedtime,
                iconTint = TintWindow,
            )
            AquaDivider()
            AquaStepperRow(
                title = stringResource(R.string.setting_window_end),
                value = hourLabel(settings.activeEndHour),
                onDecrease = {
                    viewModel.updateSettings {
                        it.copy(activeEndHour = (it.activeEndHour - 1).coerceAtLeast(it.activeStartHour + 1))
                    }
                },
                onIncrease = {
                    viewModel.updateSettings { it.copy(activeEndHour = (it.activeEndHour + 1).coerceAtMost(24)) }
                },
                icon = Icons.Filled.Schedule,
                iconTint = TintSchedule,
            )
            AquaDivider()
            AquaSwitchRow(
                title = stringResource(R.string.setting_live_on),
                summary = stringResource(R.string.setting_live_summary),
                checked = settings.liveActivityEnabled,
                icon = Icons.Filled.WaterDrop,
                iconTint = TintLive,
                onCheckedChange = { enabled -> viewModel.updateSettings { it.copy(liveActivityEnabled = enabled) } },
            )
            Text(
                text = stringResource(R.string.setting_live_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 36.dp, end = 16.dp, bottom = 10.dp),
            )
            AquaDivider()
            AquaSwitchRow(
                title = stringResource(R.string.setting_sound),
                summary = stringResource(R.string.setting_sound_summary),
                checked = settings.soundEnabled,
                icon = Icons.Filled.VolumeUp,
                iconTint = TintSound,
                onCheckedChange = { enabled -> viewModel.updateSettings { it.copy(soundEnabled = enabled) } },
            )
            AquaDivider()
            AquaSwitchRow(
                title = stringResource(R.string.setting_vibrate),
                checked = settings.vibrateEnabled,
                icon = Icons.Filled.Vibration,
                iconTint = TintVibrate,
                onCheckedChange = { enabled -> viewModel.updateSettings { it.copy(vibrateEnabled = enabled) } },
            )
        }
    }
}

internal fun hourLabel(hour: Int): String = String.format(Locale.getDefault(), "%02d:00", hour)

internal fun Float.roundToStep(step: Int): Int = (this / step).let { (it + 0.5f).toInt() * step }
