package com.aquaday.app.ui

import androidx.compose.ui.graphics.Color

// 每组图标一个固定色相的高饱和色，避免整页只有主题蓝。
internal val TintReminder = Color(0xFF2F7BF5)
internal val TintWindow = Color(0xFF7A5CFA)
internal val TintSchedule = Color(0xFF1FA6C4)
internal val TintLive = Color(0xFF10B3C6)
internal val TintSound = Color(0xFFF0981A)
internal val TintVibrate = Color(0xFFF0558C)
internal val TintPermission = Color(0xFF17AE7C)
internal val TintProfile = Color(0xFFF08018)
internal val TintAlarmPermission = Color(0xFFD6783D)
internal val TintTheme = Color(0xFF6552F2)
internal val TintBackground = Color(0xFF2E9E68)
internal val TintReset = Color(0xFFE53A3A)
internal val TintAbout = Color(0xFF6F7B8A)

/**
 * 破坏性操作的填充色。M3 的 error 在深色下是浅粉，白字几乎看不清；
 * 这里固定成高饱和红，两个主题共用。
 */
internal val AquaDanger = Color(0xFFE5322B)
