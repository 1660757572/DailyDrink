package com.aquaday.app.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager

/** 通知渠道一次装配；实时活动使用低频渠道，避免每次刷新都响铃。 */
object AquaChannels {
    // v2：常驻实时活动改用 DEFAULT 渠道，Android 16 的提升常驻通知要求不低静默渠道；
    // 旧渠道不可修改重要性，只能换 id 重建。
    const val LIVE = "aqua_live_v2"
    const val REMINDER = "aqua_reminder"
    const val ACHIEVEMENT = "aqua_achievement"

    const val LIVE_ID = 1001
    const val REMINDER_ID = 1002
    const val ACHIEVEMENT_ID = 1003
    const val GROUP_AQUA = "aqua_group"

    fun ensure(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val live = NotificationChannel(
            LIVE, "喝水实时活动", NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "常驻显示今日补水进度与下次提醒倒计时"
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        val achievement = NotificationChannel(
            ACHIEVEMENT, "成就解锁", NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "解锁新徽章时提示"
        }
        manager.createNotificationChannels(listOf(live, reminder(true, true), achievement))
    }

    /**
     * 渠道的声音与震动在创建后不再接受程序改动，只能在设置变更后删旧建新，
     * 否则"提示音/震动"开关会看起来完全无效。
     */
    fun applyReminderPreferences(context: Context, soundEnabled: Boolean, vibrateEnabled: Boolean) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val current = manager.notificationChannels?.firstOrNull { it.id == REMINDER } ?: return
        val soundMatches = (current.sound != null) == soundEnabled
        val vibrationMatches = current.shouldVibrate() == vibrateEnabled
        if (soundMatches && vibrationMatches) return
        manager.deleteNotificationChannel(REMINDER)
        manager.createNotificationChannel(reminder(soundEnabled, vibrateEnabled))
    }

    private fun reminder(soundEnabled: Boolean, vibrateEnabled: Boolean) = NotificationChannel(
        REMINDER, "喝水提醒", NotificationManager.IMPORTANCE_HIGH,
    ).apply {
        description = "到点提醒喝水，可直接在通知里确认已喝"
        enableVibration(vibrateEnabled)
        if (soundEnabled) {
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        } else {
            setSound(null, null)
        }
    }
}
