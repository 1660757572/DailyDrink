package com.aquaday.app.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.aquaday.app.ui.components.MenuBackdropWhileOpen
import com.aquaday.app.ui.components.bleedHorizontal
import com.itos.coui.icons.CouiIcons
import com.itos.coui.backdrop.CouiBackdrop
import com.itos.coui.chip.ChipTabButton
import com.itos.coui.chip.ChipTabRow
import com.itos.coui.chip.CouiChipTabItem
import com.itos.coui.menu.CouiMenuItem
import com.itos.coui.menu.Menu
import com.itos.coui.menu.couiMenuAnchor
import com.itos.coui.menu.rememberCouiMenuAnchor
import com.itos.coui.theme.CouiTheme

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MedalsScreen(
    state: AquaUiState,
    backdrop: CouiBackdrop,
    dark: Boolean,
    filter: Int,
    onFilterChange: (Int) -> Unit,
    grid: Boolean,
    onOpenDetail: (Int) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val metrics = state.metrics ?: return
    val all = state.achievements
    val unlockedCount = all.count { it.unlocked }
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
    // 列表↔网格切换时让整面徽章墙渐显，避免硬切；首次进入页面不做淡入。
    val wallAlpha = remember { Animatable(1f) }
    var wallInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(grid) {
        if (!wallInitialized) {
            wallInitialized = true
            return@LaunchedEffect
        }
        wallAlpha.snapTo(0f)
        wallAlpha.animateTo(1f, tween(durationMillis = 500, easing = FastOutSlowInEasing))
    }
    AquaCard {
        Column(Modifier.fillMaxWidth().graphicsLayer { alpha = wallAlpha.value }) {
            if (visible.isEmpty()) {
                Text(
                    text = stringResource(R.string.medal_empty_unlocked),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (grid) {
                MedalGrid(
                    items = visible,
                    onOpenDetail = onOpenDetail,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            } else {
                visible.forEachIndexed { index, progress ->
                    MedalRow(
                        progress = progress,
                        onClick = { onOpenDetail(index) },
                        sharedModifier = with(sharedTransitionScope) {
                            Modifier.sharedElement(
                                rememberSharedContentState(key = medalSharedKey(progress.achievement.id)),
                                animatedVisibilityScope = animatedVisibilityScope,
                            )
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
    }
}

/** 两列网格徽章墙；整页仍由外壳的列滚动，所以按行分块而不是用懒加载网格。 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MedalGrid(
    items: List<AchievementProgress>,
    onOpenDetail: (Int) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    items.chunked(2).forEachIndexed { rowIndex, row ->
        if (rowIndex > 0) Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEachIndexed { columnIndex, progress ->
                MedalCell(
                    modifier = Modifier.weight(1f),
                    progress = progress,
                    onClick = { onOpenDetail(rowIndex * 2 + columnIndex) },
                    sharedModifier = with(sharedTransitionScope) {
                        Modifier.sharedElement(
                            rememberSharedContentState(key = medalSharedKey(progress.achievement.id)),
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                    },
                )
            }
            // 末尾只剩一枚时用占位保持列宽，避免它被拉成整行。
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun MedalCell(
    modifier: Modifier,
    progress: AchievementProgress,
    onClick: () -> Unit,
    sharedModifier: Modifier,
) {
    val achievement = progress.achievement
    val tierColor = tierColor(achievement.tier)
    val accent = CouiTheme.colorScheme.accent
    val dimmed = !progress.unlocked
    Column(
        modifier
            .fillMaxWidth()
            .clip(AquaTileShape)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .then(sharedModifier)
                .clip(RoundedCornerShape(percent = 32))
                .background(if (progress.unlocked) tierColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = achievement.icon,
                contentDescription = achievement.title,
                modifier = Modifier.size(26.dp),
                tint = if (progress.unlocked) tierColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = achievement.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (progress.unlocked) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = achievement.tier.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (progress.unlocked) tierColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(6.dp))
        // 网格单元里的进度条累计收短 10%，居中后两侧留白更透气。
        Box(Modifier.fillMaxWidth(0.90f)) {
            AquaLinearProgress(
                ratio = progress.ratio,
                color = if (progress.unlocked) accent else accent.copy(alpha = 0.4f),
            )
        }
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
    MenuBackdropWhileOpen(menuOpen)
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
                    imageVector = CouiIcons.Filled.ListFilter,
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
private fun MedalRow(progress: AchievementProgress, onClick: () -> Unit, sharedModifier: Modifier) {
    val achievement = progress.achievement
    val tierColor = tierColor(achievement.tier)
    val accent = CouiTheme.colorScheme.accent
    val dimmed = !progress.unlocked
    Row(
        Modifier
            .fillMaxWidth()
            .clip(AquaTileShape)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .then(sharedModifier)
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
