package com.aquaday.app.ui

import android.app.Activity
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.aquaday.app.MedalDetailActivity
import com.aquaday.app.R
import com.aquaday.app.data.AchievementProgress
import com.aquaday.app.data.AchievementTier
import com.aquaday.app.data.Achievements
import com.aquaday.app.ui.components.AquaCard
import com.aquaday.app.ui.components.AquaChip
import com.aquaday.app.ui.components.AquaDivider
import com.aquaday.app.ui.components.AquaLinearProgress
import com.aquaday.app.ui.components.AquaSectionTitle
import com.aquaday.app.ui.components.AquaTileShape
import com.aquaday.app.ui.components.bleedHorizontal
import com.itos.coui.backdrop.CouiBackdrop
import com.itos.coui.chip.ChipTabButton
import com.itos.coui.chip.ChipTabRow
import com.itos.coui.chip.CouiChipTabItem
import com.itos.coui.menu.CouiMenuItem
import com.itos.coui.menu.Menu
import com.itos.coui.menu.couiMenuAnchor
import com.itos.coui.menu.rememberCouiMenuAnchor
import com.itos.coui.theme.CouiTheme

@Composable
fun MedalsScreen(
    state: AquaUiState,
    backdrop: CouiBackdrop,
    dark: Boolean,
    filter: Int,
    onFilterChange: (Int) -> Unit,
    onOpenDetail: (Int) -> Unit,
) {
    val metrics = state.metrics ?: return
    val all = state.achievements
    val unlockedCount = all.count { it.unlocked }
    // 一镜到底需要源卡片的实时位置与画面，素材直接从 Compose 根视图上截取。
    val root = LocalView.current
    val activity = LocalContext.current.activityOrNull()
    val fillColor = MaterialTheme.colorScheme.background.toArgb()
    // 前两枚是「全部」「已获得」，之后才是饮品分类。
    val tabs = listOf(
        stringResource(R.string.medal_filter_all),
        stringResource(R.string.medal_filter_unlocked),
    ) + Achievements.categories
    val visible = when (filter) {
        0 -> all
        1 -> all.filter { it.unlocked }
        else -> all.filter { it.achievement.category == Achievements.categories[filter - 2] }
    }

    AquaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.medal_progress, unlockedCount, all.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.medal_complete_rate, unlockedCount * 100 / all.size.coerceAtLeast(1)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                AquaLinearProgress(ratio = if (all.isEmpty()) 0f else unlockedCount.toFloat() / all.size)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MedalSummary(Modifier.weight(1f), stringResource(R.string.medal_best_streak), metrics.bestStreakDays,
                stringResource(R.string.medal_unit_days))
            MedalSummary(Modifier.weight(1f), stringResource(R.string.medal_total_volume_title),
                metrics.cumulativeEffectiveMl / 1000, stringResource(R.string.medal_unit_liters))
            MedalSummary(Modifier.weight(1f), stringResource(R.string.medal_cups_title), metrics.eventCount,
                stringResource(R.string.medal_unit_cups))
        }
    }

    Spacer(Modifier.height(16.dp))
    AquaSectionTitle(stringResource(R.string.medal_section_catalog))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        CouiCategoryTabs(
            labels = tabs,
            selected = filter,
            onSelect = onFilterChange,
            backdrop = backdrop,
            dark = dark,
        )
    } else {
        FlowRow(
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tabs.forEachIndexed { index, label ->
                AquaChip(text = label, selected = index == filter, onClick = { onFilterChange(index) })
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    AquaCard {
        if (visible.isEmpty()) {
            Text(
                text = stringResource(R.string.medal_empty_unlocked),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val ids = remember(visible) { visible.map { it.achievement.id } }
        visible.forEachIndexed { index, progress ->
            MedalRow(
                progress = progress,
                onOpen = { source ->
                    if (!openSeamlessly(activity, root, ids, index, source, fillColor)) onOpenDetail(index)
                },
            )
            if (index != visible.lastIndex) {
                Spacer(Modifier.height(14.dp))
                AquaDivider()
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@RequiresApi(34)
@Composable
private fun CouiCategoryTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    backdrop: CouiBackdrop,
    dark: Boolean,
) {
    val items = remember(labels) {
        labels.mapIndexed { index, label -> CouiChipTabItem(key = index, label = label) }
    }
    var menuOpen by remember { mutableStateOf(false) }
    val anchor = rememberCouiMenuAnchor()
    val menuItems = remember(labels, selected) {
        labels.mapIndexed { index, label -> CouiMenuItem(index, label, selected = index == selected) }
    }
    Menu(
        expanded = menuOpen,
        onDismissRequest = { menuOpen = false },
        items = menuItems,
        onItemClick = { item ->
            onSelect(item.value)
            menuOpen = false
        },
        anchor = anchor,
    )
    ChipTabRow(
        modifier = Modifier.fillMaxWidth().bleedHorizontal(),
        items = items,
        selectedKey = selected,
        onSelectedKeyChange = { onSelect(it as Int) },
        backdrop = backdrop,
        dark = dark,
        itemSpacing = 6.dp,
        // 分类多到需要横向滚动时，用前置按钮直接展开全部分类。
        leadingAction = {
            ChipTabButton(
                modifier = Modifier.couiMenuAnchor(anchor),
                onClick = { anchor.open { menuOpen = true } },
                contentDescription = stringResource(R.string.medal_filter_menu),
                backdrop = backdrop,
                dark = dark,
            ) {
                Icon(
                    imageVector = Icons.Filled.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
    )
}

@Composable
private fun MedalSummary(modifier: Modifier, label: String, value: Int, unit: String) {
    Column(
        modifier
            .clip(AquaTileShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "$value $unit",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CouiTheme.colorScheme.accent,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun MedalRow(progress: AchievementProgress, onOpen: (IntRect) -> Unit) {
    val achievement = progress.achievement
    val tierColor = tierColor(achievement.tier)
    val accent = CouiTheme.colorScheme.accent
    val dimmed = !progress.unlocked
    var medalBox by remember { mutableStateOf(IntRect.Zero) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(AquaTileShape)
            .clickable { onOpen(medalBox) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .onGloballyPositioned {
                    val bounds = it.boundsInRoot()
                    medalBox = IntRect(
                        bounds.left.toInt(),
                        bounds.top.toInt(),
                        bounds.right.toInt(),
                        bounds.bottom.toInt(),
                    )
                }
                .clip(RoundedCornerShape(percent = 32))
                .background(if (progress.unlocked) tierColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = achievement.icon,
                contentDescription = achievement.title,
                modifier = Modifier.size(22.dp),
                tint = if (progress.unlocked) tierColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = achievement.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (progress.unlocked) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = achievement.tier.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (progress.unlocked) tierColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
            Text(text = achievement.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(5.dp))
            AquaLinearProgress(
                ratio = progress.ratio,
                color = if (progress.unlocked) accent else accent.copy(alpha = 0.4f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (progress.unlocked) {
                    progress.unlockedAt?.let { stringResource(R.string.medal_unlocked_time, it.asDateLabel()) }
                        ?: stringResource(R.string.medal_unlocked_now)
                } else {
                    stringResource(R.string.medal_locked, progress.detail)
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (progress.unlocked) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(10.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
        )
    }
}

@Composable
internal fun tierColor(tier: AchievementTier): Color = when (tier) {
    AchievementTier.Bronze -> Color(0xFFCD8C52)
    AchievementTier.Silver -> Color(0xFF9FB0C0)
    AchievementTier.Gold -> Color(0xFFE0A63C)
    AchievementTier.Legend -> Color(0xFF8A6BFF)
}

/**
 * 用一镜到底打开徽章详情：源是这枚缩略图，落点是它在列表里的位置。
 * 设备未声明支持、截图失败或 Kit 没能构造出动画参数时返回 false，由调用方走应用内过场。
 */
private fun openSeamlessly(
    activity: Activity?,
    root: View,
    ids: List<String>,
    index: Int,
    source: IntRect,
    colorArgb: Int,
): Boolean {
    if (activity == null || source.isEmpty || !Seamless.isSupported()) return false
    val bounds = Seamless.boundsOf(root, source)
    val thumbnail = Seamless.capture(root, source) ?: return false
    // 缩略图用 percent=32 的圆角，Kit 只认 px，按截取尺寸换算同一比例。
    val radiusPx = 0.32f * minOf(bounds.width(), bounds.height())
    return Seamless.open(
        activity,
        MedalDetailActivity.intentTo(activity, ids, index, bounds, radiusPx, colorArgb),
        thumbnail,
        bounds,
        radiusPx,
        colorArgb,
    )
}
