package com.aquaday.app.data

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** 由事件列表推导出的纯函数指标，不含任何 Android 依赖。 */
object HydrationMetrics {

    fun dayRecords(events: List<DrinkEvent>, goalMl: Int): Map<LocalDate, DayRecord> {
        if (events.isEmpty()) return emptyMap()
        return events.groupBy { it.at.toLocalDate() }
            .map { (date, group) ->
                val total = group.sumOf { it.effectiveMl }
                DayRecord(
                    date = date,
                    totalMl = total,
                    eventCount = group.size,
                    reached = total >= goalMl,
                    ratio = if (goalMl <= 0) 0f else total.toFloat() / goalMl,
                )
            }
            .associateBy { it.date }
    }

    fun compute(snapshot: AquaSnapshot, today: LocalDate = LocalDate.now()): AquaMetrics {
        val events = snapshot.events
        val byDay = dayRecords(events, snapshot.goalMl)
        val current = byDay[today] ?: DayRecord(today, 0, 0, reached = false, ratio = 0f)

        val reachedDays = byDay.values.filter { it.reached }.map { it.date }.sorted()
        val streak = currentStreak(reachedDays, today)
        val bestStreak = longestRun(reachedDays)

        val monthReached = reachedDays.groupBy { YearMonth.from(it) }
        val perfectMonths = monthReached.entries
            .filter { (month, days) ->
                val elapsed = if (month == YearMonth.from(today)) today.dayOfMonth else month.lengthOfMonth()
                elapsed >= PERFECT_MONTH_MIN_DAYS && days.size >= elapsed
            }
            .map { it.key }
            .sorted()

        return AquaMetrics(
            snapshot = snapshot,
            today = current,
            streakDays = streak,
            bestStreakDays = bestStreak,
            cumulativeEffectiveMl = events.sumOf { it.effectiveMl },
            eventCount = events.size,
            earliestHour = events.minOfOrNull { it.at.hour },
            latestHour = events.maxOfOrNull { it.at.hour },
            singleMaxMl = events.maxOfOrNull { it.amountMl } ?: 0,
            maxDailyEffectiveMl = byDay.values.maxOfOrNull { it.totalMl } ?: 0,
            maxDailyEventCount = byDay.values.maxOfOrNull { it.eventCount } ?: 0,
            distinctTypes = events.map { it.type }.distinct().size,
            bestMonthReachedDays = monthReached.values.maxOfOrNull { it.size } ?: 0,
            perfectMonths = perfectMonths,
        )
    }

    /** 今天尚未达标时不断链，从昨天往前连续计数。 */
    fun currentStreak(reachedDays: List<LocalDate>, today: LocalDate): Int {
        if (reachedDays.isEmpty()) return 0
        val set = reachedDays.toHashSet()
        var cursor = if (today in set) today else today.minusDays(1)
        if (cursor !in set) return 0
        var count = 0
        while (cursor in set) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    fun longestRun(reachedDays: List<LocalDate>): Int {
        if (reachedDays.isEmpty()) return 0
        var best = 1
        var run = 1
        for (index in 1 until reachedDays.size) {
            val gap = ChronoUnit.DAYS.between(reachedDays[index - 1], reachedDays[index])
            run = if (gap == 1L) run + 1 else 1
            best = maxOf(best, run)
        }
        return best
    }

    /** 月历网格：首行按星期对齐，末尾补齐整周。 */
    fun monthGrid(month: YearMonth, byDay: Map<LocalDate, DayRecord>, today: LocalDate): List<CalendarCell> {
        val firstDay = month.atDay(1)
        val leading = (firstDay.dayOfWeek.value + 6) % 7
        val cells = MutableList(leading) { CalendarCell(blank = true) }
        for (day in 1..month.lengthOfMonth()) {
            val date = month.atDay(day)
            cells.add(
                CalendarCell(
                    blank = false,
                    record = byDay[date] ?: DayRecord(date, 0, 0, reached = false, ratio = 0f),
                    isFuture = date.isAfter(today),
                    isToday = date == today,
                ),
            )
        }
        while (cells.size % 7 != 0) cells.add(CalendarCell(blank = true))
        return cells
    }


    private const val PERFECT_MONTH_MIN_DAYS = 7
}

data class CalendarCell(
    val blank: Boolean = false,
    val record: DayRecord? = null,
    val isFuture: Boolean = false,
    val isToday: Boolean = false,
)
