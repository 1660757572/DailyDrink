package com.aquaday.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.aquaday.app.data.ReminderSettings
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 提醒锚定在"上一次喝水时间 + 间隔"，因此记录新一次喝水会自然顺延下一次提醒。
 * 精确闹钟被系统拒绝时降级为 10 分钟窗口的非精确闹钟。
 */
class ReminderScheduler(private val context: Context) {

    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val zone = ZoneId.systemDefault()

    fun canScheduleExact(): Boolean = runCatching { alarms.canScheduleExactAlarms() }.getOrDefault(false)

    fun nextFire(settings: ReminderSettings, anchor: LocalDateTime?): LocalDateTime =
        nextFireTime(settings, anchor ?: LocalDateTime.now(), LocalDateTime.now())

    fun nextFireMillis(settings: ReminderSettings, anchor: LocalDateTime?): Long =
        nextFire(settings, anchor).atZone(zone).toInstant().toEpochMilli()

    fun apply(settings: ReminderSettings, anchor: LocalDateTime?) {
        clear()
        if (!settings.enabled) {
            Log.i(TAG, "apply: 提醒已关闭，仅取消闹钟")
            return
        }
        val fireAt = nextFireMillis(settings, anchor)
        val pending = firePending()
        if (canScheduleExact()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pending)
        } else {
            Log.w(TAG, "apply: 无精确闹钟权限，降级为窗口提醒")
            alarms.setWindow(AlarmManager.RTC_WAKEUP, fireAt, INEXACT_WINDOW_MS, pending)
        }
        Log.i(TAG, "apply: 下次提醒 = ${format(fireAt)}")
    }

    fun snooze(minutes: Int) {
        val fireAt = System.currentTimeMillis() + minutes * 60_000L
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, snoozePending())
        Log.i(TAG, "snooze: 顺延 $minutes 分钟")
    }

    fun clear() {
        alarms.cancel(firePending())
        alarms.cancel(snoozePending())
    }

    private fun firePending(): PendingIntent = broadcast(REQUEST_FIRE, ReminderActionReceiver.ACTION_FIRE)

    private fun snoozePending(): PendingIntent = broadcast(REQUEST_SNOOZE, ReminderActionReceiver.ACTION_FIRE)

    private fun broadcast(requestCode: Int, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, ReminderActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun format(millis: Long): String =
        LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), zone).toString()

    private companion object {
        const val TAG = "ReminderScheduler"
        const val REQUEST_FIRE = 4001
        const val REQUEST_SNOOZE = 4002
        const val INEXACT_WINDOW_MS = 10 * 60_000L

        /** 在活跃时间窗内推算下一次提醒。 */
        fun nextFireTime(settings: ReminderSettings, anchor: LocalDateTime, now: LocalDateTime): LocalDateTime {
            val dayStart = now.toLocalDate().atStartOfDay()
            val open = dayStart.plusHours(settings.activeStartHour.coerceIn(0, 23).toLong())
            val close = dayStart.plusHours(settings.activeEndHour.coerceIn(1, 24).toLong())
            val proposed = anchor.plusMinutes(settings.intervalMinutes.toLong())
            val earliest = maxOf(now.plusMinutes(1), open)
            val candidate = maxOf(proposed, earliest)
            return if (candidate >= close) open.plusDays(1) else candidate
        }
    }
}
