package com.aquaday.app.ui

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

private val ClockFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val MonthFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy 年 M 月")
private val DayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("M 月 d 日")
private val FullDayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日")

fun LocalDateTime.asClock(): String = format(ClockFormat)

/** 成就点亮时间只到日期，精确到分钟反而噪音。 */
fun Long.asDateLabel(): String =
    java.time.Instant.ofEpochMilli(this).atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(FullDayFormat)

fun LocalDate.asMonthTitle(): String = YearMonth.from(this).format(MonthFormat)

fun YearMonth.asMonthTitle(): String = format(MonthFormat)

fun LocalDate.asDayLabel(today: LocalDate): String = when (ChronoUnit.DAYS.between(this, today)) {
    0L -> "今天"
    1L -> "昨天"
    -1L -> "明天"
    else -> format(if (year == today.year) DayFormat else FullDayFormat)
}

fun LocalTime.asClock(): String = format(ClockFormat)

/** 大水量用 L 表达，避免长数字。 */
fun milliliters(valueMl: Int): String =
    if (abs(valueMl) >= 1000) {
        val liters = valueMl / 1000f
        if (valueMl % 100 == 0) "${valueMl / 1000} L" else String.format(Locale.getDefault(), "%.1f L", liters)
    } else {
        "$valueMl ml"
    }

fun idleText(minutes: Long): String = when {
    minutes < 1L -> "刚刚"
    minutes < 60L -> "$minutes 分钟"
    else -> "${minutes / 60} 小时 ${minutes % 60} 分钟"
}
