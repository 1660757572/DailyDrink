package com.aquaday.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.Dialog
import com.itos.coui.theme.CouiTheme
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

private val HomeQuickAmounts = listOf(150, 200, 300, 500)

@Composable
fun HomeScreen(
    state: AquaUiState,
    viewModel: AquaViewModel,
    backdrop: CouiBackdrop,
    dialogBackdrop: CouiBackdrop,
    dark: Boolean,
) {
    val metrics = state.metrics ?: return
    val today = metrics.today
    val goal = metrics.snapshot.goalMl
    val events = metrics.snapshot.events
        .filter { it.at.toLocalDate() == today.date }
        .sortedByDescending { it.at }
    // 圆环跟随每次记录平滑推进，冷启动时也从 0 长到当前进度。
    val animatedRatio by animateFloatAsState(
        targetValue = today.ratio,
        animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing),
        label = "home-ring",
    )
    var pendingDelete by remember { mutableStateOf<DrinkEvent?>(null) }
    // 冷启动时今日已达标也要看到打勾动画，所以初始为 false 等圆环走满后再弹出。
    val checkState = remember { MutableTransitionState(false) }
    LaunchedEffect(today.reached, animatedRatio) {
        checkState.targetState = today.reached && animatedRatio >= 0.995f
    }

    AquaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AquaProgressRing(
                ratio = animatedRatio,
                modifier = Modifier.size(120.dp),
                strokeWidth = 11f,
            ) {
                AnimatedVisibility(
                    visibleState = checkState,
                    enter = fadeIn(tween(160)) + scaleIn(
                        initialScale = 0.2f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    ),
                    exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.2f, animationSpec = tween(120)),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = stringResource(R.string.home_reached_flag),
                        modifier = Modifier.size(42.dp),
                        tint = CouiTheme.colorScheme.accent,
                    )
                }
            }
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
                        (animatedRatio * 100).roundToInt().coerceAtMost(999),
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
                    text = "+ $amount",
                    selected = false,
                    onClick = { viewModel.addDrink(amount) },
                    modifier = Modifier.weight(1f),
                    // 等分宽度下内边距收窄并强制单行，避免比例字体把「+ 200」挤成两行。
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 9.dp),
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        AquaLongButton(
            text = stringResource(R.string.action_add),
            icon = Icons.Filled.LocalDrink,
            // 大按钮一键记录「默认一杯容量」，需要改容量或饮品时走右下角加号。
            onClick = { viewModel.addDrink(metrics.snapshot.settings.defaultCupMl) },
            backdrop = backdrop,
            dark = dark,
        )
        // 卡片内边距只有 16dp，按钮下方要多留一点，视觉重心才不会被底边挤住。
        Spacer(Modifier.height(6.dp))
    }

    Spacer(Modifier.height(18.dp))
    AquaSectionTitle(stringResource(R.string.home_reminder_status))
    AquaCard {
        val settings = metrics.snapshot.settings
        val anchor = metrics.snapshot.reminderAnchor
        val idleMinutes = if (anchor == null) 0L else ChronoUnit.MINUTES.between(anchor, state.now).coerceAtLeast(0L)
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
                    HomeLogRow(event) { pendingDelete = event }
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

    pendingDelete?.let { event ->
        Dialog(
            onDismissRequest = { pendingDelete = null },
            dark = dark,
            title = stringResource(R.string.home_delete_title),
            message = stringResource(R.string.home_delete_confirm, event.at.asClock(), event.amountMl),
            confirmAction = CouiDialogAction(
                text = stringResource(R.string.action_delete),
                onClick = {
                    viewModel.deleteEvent(event.id)
                    pendingDelete = null
                },
                style = CouiDialogActionStyle.Primary,
                colors = CouiDialogDefaults.primaryActionColors(AquaDanger),
            ),
            dismissAction = CouiDialogAction(
                text = stringResource(R.string.action_cancel),
                onClick = { pendingDelete = null },
            ),
            material = if (CouiDialogDefaults.isMaterialSupported()) CouiDialogDefaults.material(dark) else null,
            backdrop = dialogBackdrop,
        )
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
        // 时间列固定宽度，避免比例字体下各时刻宽度不同把后面的饮品名挤得参差不齐。
        Text(
            text = event.at.asClock(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(44.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.home_log_entry, event.type.label, event.amountMl),
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
