package com.aquaday.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aquaday.app.R
import com.aquaday.app.data.CalendarCell
import com.aquaday.app.data.HydrationMetrics
import com.aquaday.app.ui.components.AquaCard
import com.aquaday.app.ui.components.AquaArrowButton
import com.aquaday.app.ui.components.AquaCardHorizontalPadding
import com.aquaday.app.ui.components.AquaCardVerticalPadding
import com.aquaday.app.ui.components.AquaDayBar
import com.aquaday.app.ui.components.AquaDivider
import com.aquaday.app.ui.components.AquaSectionTitle
import com.itos.coui.theme.CouiTheme
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(state: AquaUiState, viewModel: AquaViewModel) {
    val metrics = state.metrics ?: return
    val month = state.month
    val today = LocalDate.now()
    val byDay = HydrationMetrics.dayRecords(metrics.snapshot.events, metrics.snapshot.goalMl)
    val monthDays = byDay.keys.filter { YearMonth.from(it) == month }
    val reachedCount = monthDays.count { byDay[it]?.reached == true }
    val monthTotal = monthDays.sumOf { byDay[it]?.totalMl ?: 0 }
    val elapsed = if (month == YearMonth.from(today)) today.dayOfMonth else month.lengthOfMonth()

    AquaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = month.asMonthTitle(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            AquaArrowButton(forward = false, contentDescription = stringResource(R.string.calendar_prev_month),
                onClick = { viewModel.shiftMonth(-1) })
            Spacer(Modifier.size(6.dp))
            AquaArrowButton(
                forward = true,
                contentDescription = stringResource(R.string.calendar_next_month),
                onClick = { viewModel.shiftMonth(1) },
                enabled = month < YearMonth.from(today),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.calendar_summary, reachedCount, elapsed, monthTotal),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        CalendarLegend()
    }

    Spacer(Modifier.height(16.dp))
    AquaSectionTitle(stringResource(R.string.calendar_title))
    AquaCard {
        Row(Modifier.fillMaxWidth()) {
            weekdays().forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        state.calendar.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                week.forEach { cell ->
                    CalendarCellView(
                        cell = cell,
                        selected = !cell.blank && cell.record?.date == state.selectedDate,
                        modifier = Modifier.weight(1f),
                        onClick = { cell.record?.date?.takeIf { !cell.isFuture }?.let(viewModel::selectDate) },
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    val selected = state.selectedDate
    val record = byDay[selected]
    AquaSectionTitle(selected.asDayLabel(today))
    AquaCard(contentPadding = false) {
        val events = metrics.snapshot.events.filter { it.at.toLocalDate() == selected }.sortedByDescending { it.at }
        if (events.isEmpty()) {
            Text(
                text = if (selected.isAfter(today)) stringResource(R.string.calendar_future_hint) else stringResource(R.string.calendar_day_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = AquaCardHorizontalPadding, vertical = 20.dp),
            )
        } else {
            Text(
                text = stringResource(R.string.calendar_day_title, selected.asDayLabel(today), record?.totalMl ?: 0),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = AquaCardHorizontalPadding,
                    end = AquaCardHorizontalPadding,
                    top = AquaCardVerticalPadding,
                    bottom = 6.dp,
                ),
            )
            events.forEachIndexed { index, event ->
                Row(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = AquaCardHorizontalPadding, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.WaterDrop, null, Modifier.size(14.dp), tint = CouiTheme.colorScheme.accent)
                    Spacer(Modifier.size(8.dp))
                    // 时间列固定宽度，让各行饮品名起点一致。
                    Text(
                        text = event.at.asClock(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(38.dp),
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = stringResource(R.string.home_log_entry, event.type.label, event.amountMl),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (index != events.lastIndex) {
                    AquaDivider(startIndent = AquaCardHorizontalPadding)
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun CalendarCellView(cell: CalendarCell, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    if (cell.blank) {
        Spacer(modifier)
        return
    }
    val accent = CouiTheme.colorScheme.accent
    val record = cell.record
    val contentColor = when {
        cell.blank -> Color.Transparent
        cell.isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        record?.reached == true -> accent
        record != null && record.totalMl > 0 -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }
    Column(
        modifier = modifier
            .padding(1.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    selected -> accent.copy(alpha = 0.16f)
                    cell.isToday -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    else -> Color.Transparent
                },
            )
            .then(if (record != null && !cell.isFuture) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = record?.date?.dayOfMonth?.toString() ?: " ",
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(3.dp))
        AquaDayBar(
            ratio = (record?.ratio ?: 0f).coerceIn(0f, 1f),
            modifier = Modifier.size(width = 14.dp, height = 22.dp),
            color = if (record?.reached == true) accent else accent.copy(alpha = 0.55f),
        )
    }
}

@Composable
private fun CalendarLegend() {
    val accent = CouiTheme.colorScheme.accent
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem(stringResource(R.string.calendar_legend_reached), accent.copy(alpha = 1f))
        LegendItem(stringResource(R.string.calendar_legend_partial), accent.copy(alpha = 0.5f))
        LegendItem(stringResource(R.string.calendar_legend_empty), MaterialTheme.colorScheme.surfaceVariant)
    }
}

@Composable
private fun LegendItem(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(
            Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
        Spacer(Modifier.size(5.dp))
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun weekdays(): List<String> = LocalContext.current.resources.getStringArray(R.array.aqua_week_days).toList()

