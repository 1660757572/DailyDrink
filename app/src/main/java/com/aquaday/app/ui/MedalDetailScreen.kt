package com.aquaday.app.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aquaday.app.R
import com.aquaday.app.data.AchievementProgress
import com.aquaday.app.ui.components.AquaLinearProgress
import com.aquaday.app.ui.components.AquaLongButton
import com.aquaday.app.ui.components.bleedHorizontal
import com.itos.coui.backdrop.CouiBackdrop
import com.itos.coui.theme.CouiTheme

private const val DetailPanelHeight = 500

/**
 * 徽章详情：整页是一张铺到屏幕边缘的渐变面板，左右滑动浏览同一分类下的其他徽章。
 * startIndex 只在首次进入时读取，滑动不再改动路由，避免 AnimatedContent 重新过场。
 * 这里用不参与保存的 PagerState：路由的 SaveableStateHolder 会还原上次滑到的页码，
 * 重新点开另一枚徽章时会落在旧页。
 */
@Composable
fun MedalDetailScreen(
    items: List<AchievementProgress>,
    startIndex: Int,
    backdrop: CouiBackdrop,
    dark: Boolean,
) {
    if (items.isEmpty()) return
    val start = startIndex.coerceIn(0, items.lastIndex)
    val pager = remember(start, items.size) { PagerState(start) { items.size } }

    HorizontalPager(
        state = pager,
        modifier = Modifier.fillMaxWidth().height(DetailPanelHeight.dp).bleedHorizontal(),
        contentPadding = PaddingValues(horizontal = 12.dp),
        pageSpacing = 12.dp,
    ) { page ->
        val progress = items[page]
        MedalDetailPage(
            progress = progress,
            backdrop = backdrop,
            dark = dark,
        )
    }

    if (items.size > 1) {
        Spacer(Modifier.height(16.dp))
        DetailDots(count = items.size, current = pager.currentPage)
    }
}

@Composable
private fun MedalDetailPage(
    progress: AchievementProgress,
    backdrop: CouiBackdrop,
    dark: Boolean,
) {
    val achievement = progress.achievement
    val tier = tierColor(achievement.tier)
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val context = LocalContext.current
    val accent = if (progress.unlocked) tier else tier.copy(alpha = 0.55f)

    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.verticalGradient(
                    listOf(lerp(surface, tier, if (progress.unlocked) 0.42f else 0.16f), surface),
                ),
            )
            .padding(horizontal = 22.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MedalMedallion(
            icon = achievement.icon,
            tier = tier,
            unlocked = progress.unlocked,
        )
        Spacer(Modifier.height(26.dp))
        Text(
            text = achievement.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = achievement.tier.label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = accent,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = achievement.summary,
            style = MaterialTheme.typography.bodyLarge,
            color = onVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(22.dp))
        AquaLinearProgress(
            ratio = progress.ratio,
            color = if (progress.unlocked) accent else accent.copy(alpha = 0.4f),
            track = onVariant.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (progress.unlocked) {
                progress.unlockedAt?.let { stringResource(R.string.medal_unlocked_time, it.asDateLabel()) }
                    ?: stringResource(R.string.medal_unlocked_now)
            } else {
                stringResource(R.string.medal_locked, progress.detail)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (progress.unlocked) accent else onVariant,
        )
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(18.dp))
        if (progress.unlocked) {
            AquaLongButton(
                text = stringResource(R.string.medal_share),
                icon = Icons.Filled.Share,
                onClick = {
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    context.getString(R.string.medal_share_text, achievement.title, achievement.summary),
                                )
                            },
                            context.getString(R.string.medal_share_title),
                        ),
                    )
                },
                backdrop = backdrop,
                dark = dark,
                modifier = Modifier.fillMaxWidth(),
                height = 52.dp,
            )
        } else {
            Text(
                text = stringResource(R.string.medal_share_locked_hint),
                style = MaterialTheme.typography.bodySmall,
                color = onVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * 详情页的奖章本体：与列表缩略图同形状，共享元素才能平滑放大。
 * 未点亮时用中性灰，浅色底上白图标会整个看不见。
 */
@Composable
private fun MedalMedallion(
    icon: ImageVector,
    tier: Color,
    unlocked: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(percent = 32)
    val dim = MaterialTheme.colorScheme.onSurfaceVariant
    val fill = if (unlocked) {
        Brush.linearGradient(listOf(tier.copy(alpha = 0.95f), tier.copy(alpha = 0.45f)))
    } else {
        Brush.linearGradient(listOf(dim.copy(alpha = 0.34f), dim.copy(alpha = 0.16f)))
    }
    Box(
        modifier
            .size(168.dp)
            .clip(shape)
            .background(fill)
            .border(1.2.dp, if (unlocked) Color.White.copy(alpha = 0.5f) else dim.copy(alpha = 0.22f), shape)
            .padding(20.dp)
            .clip(shape)
            .background(Color.Black.copy(alpha = if (unlocked) 0.18f else 0.05f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(76.dp),
            tint = if (unlocked) Color.White else dim.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun DetailDots(count: Int, current: Int) {
    if (count > DOT_LIMIT) {
        Text(
            text = "${current + 1} / $count",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        return
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val active = index == current
            Box(
                Modifier
                    .width(if (active) 18.dp else 6.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (active) CouiTheme.colorScheme.accent
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f),
                    ),
            )
        }
    }
}

private const val DOT_LIMIT = 10
