package com.aquaday.app.ui

import androidx.compose.ui.graphics.Color

// 每组图标一个低饱和色，避免整页只有主题蓝。
internal val TintReminder = Color(0xFF5B8DEF)
internal val TintWindow = Color(0xFF8E7BF0)
internal val TintSchedule = Color(0xFF4FAFC4)
internal val TintLive = Color(0xFF43B7C4)
internal val TintSound = Color(0xFFE0A248)
internal val TintVibrate = Color(0xFFDD7E9E)
internal val TintPermission = Color(0xFF4CAF8E)
internal val TintProfile = Color(0xFFE08A3C)
internal val TintAlarmPermission = Color(0xFFC98A5E)
internal val TintTheme = Color(0xFF7A6FE0)
internal val TintBackground = Color(0xFF5E9B7A)
internal val TintReset = Color(0xFFD05C5C)
internal val TintAbout = Color(0xFF8A8F98)

/**
 * 破坏性操作的填充色。M3 的 error 在深色下是浅粉，白字几乎看不清；
 * 这里固定成高饱和红，两个主题共用。
 */
internal val AquaDanger = Color(0xFFE5322B)
