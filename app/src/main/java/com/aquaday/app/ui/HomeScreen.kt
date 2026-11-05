package com.aquaday.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aquaday.app.R
import com.aquaday.app.data.DrinkEvent
import com.aquaday.app.ui.components.AquaCard
import com.aquaday.app.ui.components.AquaCardHorizontalPadding
import com.aquaday.app.ui.components.AquaChip
import com.aquaday.app.ui.components.AquaLongButton
import com.aquaday.app.ui.components.AquaProgressRing
import com.aquaday.app.ui.components.AquaRow
import com.aquaday.app.ui.components.AquaSectionTitle
import com.itos.coui.backdrop.CouiBackdrop
import com.itos.coui.theme.CouiTheme
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

private val HomeQuickAmounts = listOf(150, 200, 300, 500)

@Composable
fun HomeScreen(
    state: AquaUiState,
    viewModel: AquaViewModel,
    onAdd: () -> Unit,
    backdrop: CouiBackdrop,
    dark: Boolean,
) {
    val metrics = state.metrics ?: return
    val today = metrics.today
    val goal = metrics.snapshot.goalMl
    val events = metrics.snapshot.events
        .filter { it.at.toLocalDate() == today.date }
        .sortedByDescending { it.at }

    AquaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AquaProgressRing(
                ratio = today.ratio,
                modifier = Modifier.size(120.dp),
                strokeWidth = 11f,
            )
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_drunk),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${today.totalMl}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = " ml",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.home_goal_line,
                        goal,
                        (today.ratio * 100).roundToInt().coerceAtMost(999),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                HomeStat(
                    icon = Icons.Filled.ContentPaste,
                    label = stringResource(R.string.home_remaining_prefix),
                    value = if (today.reached) {
                        stringResource(R.string.home_reached_flag)
                    } else {
                        "${metrics.remainingMl} ml"
                    },
                )
                HomeStat(
                    icon = Icons.Filled.Schedule,
                    label = stringResource(R.string.home_streak_prefix),
                    value = stringResource(R.string.home_streak_value, metrics.streakDays),
                )
            }
        }
        if (today.reached) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.home_reached_banner),
                style = MaterialTheme.typography.bodySmall,
                color = CouiTheme.colorScheme.accent,
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HomeQuickAmounts.forEach { amount ->
                AquaChip(
                    text = "+$amount",
                    selected = false,
                    onClick = { viewModel.addDrink(amount) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        AquaLongButton(
            text = stringResource(R.string.action_add),
            icon = Icons.Filled.LocalDrink,
            onClick = onAdd,
            backdrop = backdrop,
            dark = dark,
        )
    }

    Spacer(Modifier.height(18.dp))
    AquaSectionTitle(stringResource(R.string.home_reminder_status))
    AquaCard {
        val settings = metrics.snapshot.settings
        val anchor = metrics.snapshot.reminderAnchor
        val idleMinutes = if (anchor == null) 0L else ChronoUnit.MINUTES.between(anchor, LocalDateTime.now()).coerceAtLeast(0L)
        AquaRow(
            title = if (settings.enabled) {
                stringResource(R.string.home_next_reminder, state.nextReminderAt?.asClock() ?: "--:--")
            } else {
                stringResource(R.string.home_reminder_paused)
            },
            summary = stringResource(R.string.home_idle_summary, idleText(idleMinutes)),
            icon = Icons.Filled.Schedule,
            trailing = {
                Text(
                    text = stringResource(R.string.setting_interval_summary, settings.intervalMinutes),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
    }

    Spacer(Modifier.height(18.dp))
    AquaSectionTitle(stringResource(R.string.home_today_log))
    AquaCard(contentPadding = false) {
        if (events.isEmpty()) {
            Text(
                text = stringResource(R.string.home_empty_log),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = AquaCardHorizontalPadding, vertical = 20.dp),
            )
        } else {
            events.forEachIndexed { index, event ->
                Column {
                    HomeLogRow(event) { viewModel.deleteEvent(event.id) }
                    if (index != events.lastIndex) {
                        HorizontalDivider(
                            thickness = 0.6.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = AquaCardHorizontalPadding),
                        )
                    }
                }
            }
            Text(
                text = stringResource(
                    R.string.home_today_total,
                    events.size,
                    events.sumOf { it.amountMl },
                    events.sumOf { it.effectiveMl },
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
                    .padding(start = AquaCardHorizontalPadding, end = AquaCardHorizontalPadding, top = 10.dp, bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun HomeStat(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(16.dp), tint = CouiTheme.colorScheme.accent)
        Spacer(Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun HomeLogRow(event: DrinkEvent, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .padding(start = AquaCardHorizontalPadding, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = event.at.asClock(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.home_log_entry, event.type.label, event.amountMl, event.effectiveMl),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.home_delete_entry),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
