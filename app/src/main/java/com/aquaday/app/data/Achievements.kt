package com.aquaday.app.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

enum class AchievementTier(val label: String) {
    Bronze("青铜"),
    Silver("白银"),
    Gold("黄金"),
    Legend("传说"),
}

data class Achievement(
    val id: String,
    val title: String,
    val summary: String,
    val category: String,
    val icon: ImageVector,
    val tier: AchievementTier,
    val unit: String,
    val target: (AquaMetrics) -> Int,
    val progress: (AquaMetrics) -> Int,
)

data class AchievementProgress(
    val achievement: Achievement,
    val value: Int,
    val target: Int,
    val unlocked: Boolean,
    val unlockedAt: Long?,
) {
    val ratio: Float get() = if (target <= 0) 1f else (value.toFloat() / target).coerceIn(0f, 1f)
    val detail: String get() = "$value / $target${achievement.unit}"
}

/** 成就规则全部由 AquaMetrics 推导，新增成就只需在此追加条目。 */
object Achievements {

    const val CATEGORY_DAILY = "今日"
    const val CATEGORY_STREAK = "坚持"
    const val CATEGORY_VOLUME = "总量"
    const val CATEGORY_TASTE = "习惯"
    const val CATEGORY_SPECIAL = "特别"

    val all: List<Achievement> = listOf(
        Achievement(
            id = "first_sip", title = "第一杯水", summary = "记录你的第一次喝水",
            category = CATEGORY_DAILY, icon = Icons.Filled.WaterDrop, tier = AchievementTier.Bronze, unit = " 次",
            target = { 1 }, progress = { it.eventCount.coerceAtMost(1) },
        ),
        Achievement(
            id = "today_goal", title = "今日达标", summary = "当天补水量达到目标",
            category = CATEGORY_DAILY, icon = Icons.Filled.LocalDrink, tier = AchievementTier.Silver, unit = " ml",
            target = { it.snapshot.goalMl }, progress = { it.today.totalMl },
        ),
        Achievement(
            id = "big_gulp", title = "豪饮一口", summary = "单次喝下 500 毫升",
            category = CATEGORY_DAILY, icon = Icons.Filled.Water, tier = AchievementTier.Bronze, unit = " ml",
            target = { 500 }, progress = { it.singleMaxMl },
        ),
        Achievement(
            id = "three_liters", title = "三日量一日毕", summary = "单日补水达到 3000 毫升",
            category = CATEGORY_DAILY, icon = Icons.Filled.Spa, tier = AchievementTier.Gold, unit = " ml",
            target = { 3_000 }, progress = { it.maxDailyEffectiveMl },
        ),
        Achievement(
            id = "ten_cups", title = "十杯满贯", summary = "单日记录 10 次喝水",
            category = CATEGORY_DAILY, icon = Icons.Filled.Coffee, tier = AchievementTier.Silver, unit = " 次",
            target = { 10 }, progress = { it.maxDailyEventCount },
        ),
        Achievement(
            id = "streak_3", title = "三日水润", summary = "连续 3 天达标",
            category = CATEGORY_STREAK, icon = Icons.Filled.Repeat, tier = AchievementTier.Bronze, unit = " 天",
            target = { 3 }, progress = { it.bestStreakDays },
        ),
        Achievement(
            id = "streak_7", title = "七日不辍", summary = "连续 7 天达标",
            category = CATEGORY_STREAK, icon = Icons.Filled.Repeat, tier = AchievementTier.Silver, unit = " 天",
            target = { 7 }, progress = { it.bestStreakDays },
        ),
        Achievement(
            id = "streak_21", title = "三周成习", summary = "连续 21 天达标",
            category = CATEGORY_STREAK, icon = Icons.Filled.SelfImprovement, tier = AchievementTier.Gold, unit = " 天",
            target = { 21 }, progress = { it.bestStreakDays },
        ),
        Achievement(
            id = "streak_100", title = "百日长流", summary = "连续 100 天达标",
            category = CATEGORY_STREAK, icon = Icons.Filled.MilitaryTech, tier = AchievementTier.Legend, unit = " 天",
            target = { 100 }, progress = { it.bestStreakDays },
        ),
        Achievement(
            id = "current_7", title = "此刻正在坚持", summary = "当前连续达标天数达到 7 天",
            category = CATEGORY_STREAK, icon = Icons.Filled.RocketLaunch, tier = AchievementTier.Silver, unit = " 天",
            target = { 7 }, progress = { it.streakDays },
        ),
        Achievement(
            id = "volume_10l", title = "一壶之水", summary = "累计有效补水 10 升",
            category = CATEGORY_VOLUME, icon = Icons.Filled.WaterDrop, tier = AchievementTier.Bronze, unit = " ml",
            target = { 10_000 }, progress = { it.cumulativeEffectiveMl },
        ),
        Achievement(
            id = "volume_50l", title = "半吨清泉", summary = "累计有效补水 50 升",
            category = CATEGORY_VOLUME, icon = Icons.Filled.BeachAccess, tier = AchievementTier.Silver, unit = " ml",
            target = { 50_000 }, progress = { it.cumulativeEffectiveMl },
        ),
        Achievement(
            id = "volume_200l", title = "二百升江河", summary = "累计有效补水 200 升",
            category = CATEGORY_VOLUME, icon = Icons.Filled.CalendarMonth, tier = AchievementTier.Gold, unit = " ml",
            target = { 200_000 }, progress = { it.cumulativeEffectiveMl },
        ),
        Achievement(
            id = "perfect_month_1", title = "完美一月", summary = "某个月份每一天都达标",
            category = CATEGORY_VOLUME, icon = Icons.Filled.EmojiEvents, tier = AchievementTier.Legend, unit = " 次",
            target = { 1 }, progress = { it.perfectMonths.size },
        ),
        Achievement(
            id = "month_20", title = "月盈二十", summary = "单月达标天数达到 20 天",
            category = CATEGORY_STREAK, icon = Icons.Filled.CalendarMonth, tier = AchievementTier.Gold, unit = " 天",
            target = { 20 }, progress = { it.bestMonthReachedDays },
        ),
        Achievement(
            id = "variety_4", title = "五味俱全", summary = "记录过 4 种不同饮品",
            category = CATEGORY_TASTE, icon = Icons.Filled.Coffee, tier = AchievementTier.Silver, unit = " 种",
            target = { 4 }, progress = { it.distinctTypes },
        ),
        Achievement(
            id = "early_bird", title = "晨起第一杯", summary = "在早晨 5-7 点记录过喝水",
            category = CATEGORY_SPECIAL, icon = Icons.Filled.WbSunny, tier = AchievementTier.Bronze, unit = " 小时",
            target = { 1 }, progress = { if (it.earliestHour != null && it.earliestHour in 5..7) 1 else 0 },
        ),
        Achievement(
            id = "night_owl", title = "夜深补水", summary = "在 22 点后记录过喝水",
            category = CATEGORY_SPECIAL, icon = Icons.Filled.NightsStay, tier = AchievementTier.Bronze, unit = " 小时",
            target = { 1 }, progress = { if (it.latestHour != null && it.latestHour >= 22) 1 else 0 },
        ),
        Achievement(
            id = "bedtime_ritual", title = "睡前一杯", summary = "在 21-23 点记录过喝水",
            category = CATEGORY_SPECIAL, icon = Icons.Filled.Bedtime, tier = AchievementTier.Silver, unit = " 小时",
            target = { 1 }, progress = { if (it.latestHour != null && it.latestHour in 21..23) 1 else 0 },
        ),
        Achievement(
            id = "half_year", title = "半年之约", summary = "记录跨越 180 天",
            category = CATEGORY_SPECIAL, icon = Icons.Filled.MilitaryTech, tier = AchievementTier.Legend, unit = " 天",
            target = { 180 }, progress = { it.spanDays() },
        ),
    )

    val categories: List<String> = listOf(
        CATEGORY_DAILY, CATEGORY_STREAK, CATEGORY_VOLUME, CATEGORY_TASTE, CATEGORY_SPECIAL,
    )

    fun evaluate(metrics: AquaMetrics): List<AchievementProgress> {
        val unlocked = metrics.snapshot.unlocked
        return all.map { achievement ->
            val value = achievement.progress(metrics).coerceAtLeast(0)
            val target = achievement.target(metrics).coerceAtLeast(1)
            val stored = unlocked[achievement.id]
            AchievementProgress(
                achievement = achievement,
                value = value,
                target = target,
                // 指标回退（例如连击中断）不应让已点亮徽章熄灭。
                unlocked = stored != null || value >= target,
                unlockedAt = stored,
            )
        }
    }

    /** 已达成的保持达成态，避免指标回退时成就消失。 */
    fun achievedIds(metrics: AquaMetrics): Set<String> =
        evaluate(metrics).filter { it.unlocked }.map { it.achievement.id }.toSet()

    fun completionRate(metrics: AquaMetrics): Float =
        evaluate(metrics).count { it.unlocked }.let { done ->
            (done * 100f / all.size).roundToInt() / 100f
        }

    private fun AquaMetrics.spanDays(): Int {
        val dates = snapshot.events.map { it.at.toLocalDate() }
        val first = dates.minOrNull() ?: return 0
        return ChronoUnit.DAYS.between(first, LocalDate.now()).toInt() + 1
    }
}
