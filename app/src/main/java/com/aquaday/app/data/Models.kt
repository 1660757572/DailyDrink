package com.aquaday.app.data

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 饮品类型；补水系数用于折算实际有效补水量。
 * 自定义类型由用户命名，没有可靠的折算依据，按 100% 记。
 */
data class DrinkType(
    val id: String,
    val label: String,
    val factor: Double,
    val builtin: Boolean = false,
) {
    companion object {
        // 顺序即旧版本的 ordinal，解码历史记录时依赖它。
        val WATER = DrinkType("water", "白水", 1.0, true)
        val TEA = DrinkType("tea", "淡茶", 0.9, true)
        val MILK = DrinkType("milk", "牛奶", 0.8, true)
        val SOUP = DrinkType("soup", "汤品", 0.8, true)
        val COFFEE = DrinkType("coffee", "咖啡", 0.6, true)
        val SODA = DrinkType("soda", "饮料", 0.5, true)
        val builtins = listOf(WATER, TEA, MILK, SOUP, COFFEE, SODA)

        fun custom(label: String, id: String): DrinkType = DrinkType(id, label, 1.0, false)
    }
}

data class DrinkEvent(
    val id: Long,
    val amountMl: Int,
    val type: DrinkType,
    val at: LocalDateTime,
) {
    val effectiveMl: Int get() = (amountMl * type.factor).toInt()
}

enum class ThemeMode { System, Light, Dark }

const val DEFAULT_SCRIM = 0.40f
const val DEFAULT_BLUR_DP = 3f
const val MAX_BLUR_DP = 30f
const val MIN_CONTAINER_OPACITY = 0.4f

/** 强调色方案：固定模板之外还有跟随壁纸与用户自选。 */
enum class ColorStyle { Default, Dynamic, Grass, Lemon, Mono, Custom }

data class ReminderSettings(
    val enabled: Boolean = true,
    val intervalMinutes: Int = 60,
    val activeStartHour: Int = 8,
    val activeEndHour: Int = 22,
    val liveActivityEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
    val defaultCupMl: Int = 200,
)

data class DayRecord(
    val date: LocalDate,
    val totalMl: Int,
    val eventCount: Int,
    val reached: Boolean,
    val ratio: Float,
)

/** 成就进度快照，所有指标一次算完，供日历、成就与实时活动复用。 */
data class AquaMetrics(
    val snapshot: AquaSnapshot,
    val today: DayRecord,
    val streakDays: Int,
    val bestStreakDays: Int,
    val cumulativeEffectiveMl: Int,
    val eventCount: Int,
    val earliestHour: Int?,
    val latestHour: Int?,
    val singleMaxMl: Int,
    val maxDailyEffectiveMl: Int,
    val maxDailyEventCount: Int,
    val distinctTypes: Int,
    val bestMonthReachedDays: Int,
    val perfectMonths: List<java.time.YearMonth>,
    val recordedDays: Int,
    val bestMonthMl: Int,
    val maxDailyBands: Int,
    val customTypeCount: Int,
    val topTypeCount: Int,
) {
    val remainingMl: Int get() = (snapshot.goalMl - today.totalMl).coerceAtLeast(0)
    val goalRatio: Float get() = if (snapshot.goalMl <= 0) 0f else (today.totalMl.toFloat() / snapshot.goalMl).coerceIn(0f, 1f)
}


data class AquaSnapshot(
    val events: List<DrinkEvent>,
    val goalMl: Int,
    val settings: ReminderSettings,
    val unlocked: Map<String, Long>,
    val themeMode: ThemeMode,
    val lastReminderAt: LocalDateTime?,
    val drinkTypes: List<DrinkType> = DrinkType.builtins,
    val colorStyle: ColorStyle = ColorStyle.Default,
    val customAccent: Int? = null,
    val backgroundImage: String? = null,
    /** 背景图上方遮罩的不透明度（0..1）与高斯模糊半径（dp）。 */
    val backgroundScrim: Float = DEFAULT_SCRIM,
    val backgroundBlur: Float = DEFAULT_BLUR_DP,
    /** 卡片等容器背景的不透明度（0..1），低于 1 时透出页面背景。 */
    val containerOpacity: Float = 1f,
    val onboarded: Boolean = false,
    val nickname: String = "",
    val avatarImage: String? = null,
) {
    val dynamicColor: Boolean get() = colorStyle == ColorStyle.Dynamic

    fun typeOf(id: String): DrinkType =
        drinkTypes.firstOrNull { it.id == id } ?: DrinkType.WATER

    /** 提醒锚点取"最近一次喝水"与"最近一次提醒"中的较晚者，避免未确认时逐分钟追响。 */
    val reminderAnchor: LocalDateTime?
        get() = events.maxOfOrNull { it.at }.let { lastDrink ->
            val lastFire = lastReminderAt
            when {
                lastDrink == null -> lastFire
                lastFire == null -> lastDrink
                else -> if (lastDrink.isAfter(lastFire)) lastDrink else lastFire
            }
        }
}

