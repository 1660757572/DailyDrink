package com.aquaday.app.reminder

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import com.aquaday.app.MainActivity
import com.aquaday.app.R
import com.aquaday.app.data.AquaMetrics
import com.aquaday.app.data.DrinkType
import com.aquaday.app.data.ReminderSettings
import java.time.Duration
import java.time.LocalDateTime

/** 负责三类通知的装配：常驻实时活动、到点提醒、成就解锁。 */
class AquaNotifier(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    fun notificationsEnabled(): Boolean = manager.areNotificationsEnabled()

    fun showLiveActivity(metrics: AquaMetrics, nextFireMillis: Long) {
        AquaChannels.ensure(context)
        val notification = buildLiveActivity(metrics, nextFireMillis)
        notify(AquaChannels.LIVE_ID, notification, "实时活动")
    }

    fun hideLiveActivity() = manager.cancel(AquaChannels.LIVE_ID)

    /** 提醒渠道的声音/震动跟随设置重建，保证开关立即生效。 */
    fun applyReminderPreferences(settings: ReminderSettings) {
        AquaChannels.ensure(context)
        AquaChannels.applyReminderPreferences(context, settings.soundEnabled, settings.vibrateEnabled)
    }

    fun postReminder(metrics: AquaMetrics, lastDrinkAt: LocalDateTime?) {
        AquaChannels.ensure(context)
        val goal = metrics.snapshot.goalMl
        val total = metrics.today.totalMl
        val cupMl = metrics.snapshot.settings.defaultCupMl
        val idleMinutes = lastDrinkAt?.let { Duration.between(it, LocalDateTime.now()).toMinutes() }
        val body = if (idleMinutes != null && idleMinutes > 0) {
            context.getString(R.string.notif_reminder_body, total, goal, idleMinutes)
        } else {
            context.getString(R.string.notif_reminder_body_fresh, total, goal)
        }
        val notification = Notification.Builder(context, AquaChannels.REMINDER)
            .setSmallIcon(R.drawable.ic_aqua_stat)
            .setContentTitle(context.getString(R.string.notif_reminder_title))
            .setContentText(body)
            .setSubText(context.getString(R.string.live_activity_channel))
            .setAutoCancel(true)
            .setNumber(metrics.remainingMl)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setGroup(AquaChannels.GROUP_AQUA)
            .setColor(ACCENT)
            .setContentIntent(openApp())
            .setTimeoutAfter(SNOOZE_MINUTES * 60_000L)
            .addAction(confirmAction(DrinkType.WATER, cupMl))
            .addAction(snoozeAction())
            .setStyle(Notification.BigTextStyle().setBigContentTitle(context.getString(R.string.notif_reminder_title)).bigText(body))
            .build()
        notify(AquaChannels.REMINDER_ID, notification, "喝水提醒")
    }

    fun dismissReminder() = manager.cancel(AquaChannels.REMINDER_ID)

    fun postAchievement(title: String, summary: String) {
        AquaChannels.ensure(context)
        val notification = Notification.Builder(context, AquaChannels.ACHIEVEMENT)
            .setSmallIcon(R.drawable.ic_aqua_stat)
            .setContentTitle(context.getString(R.string.notif_achievement_title, title))
            .setContentText(summary)
            .setSubText(context.getString(R.string.tab_medal))
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_SOCIAL)
            .setColor(ACCENT)
            .setContentIntent(openApp())
            .build()
        notify(AquaChannels.ACHIEVEMENT_ID, notification, "成就解锁")
    }

    private fun buildLiveActivity(metrics: AquaMetrics, nextFireMillis: Long): Notification {
        val settings = metrics.snapshot.settings
        val goal = metrics.snapshot.goalMl
        val total = metrics.today.totalMl
        val percent = progressPercent(total, goal)
        val title = if (metrics.today.reached) {
            context.getString(R.string.live_reached, total)
        } else {
            context.getString(R.string.live_remaining, metrics.remainingMl)
        }
        val text = context.getString(R.string.live_body, total, goal, metrics.streakDays, metrics.eventCount)
        val counting = settings.enabled && !metrics.today.reached
        val builder = Notification.Builder(context, AquaChannels.LIVE)
            .setSmallIcon(progressIcon(percent))
            .setContentTitle(title)
            .setContentText(text)
            .setNumber(total)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setColor(ACCENT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(openApp())
            .setWhen(if (counting) nextFireMillis else System.currentTimeMillis())
            .setShowWhen(true)
            .setUsesChronometer(counting)
            .setChronometerCountDown(counting)
            .addAction(confirmAction(DrinkType.WATER, settings.defaultCupMl, progressIcon(percent)))
            .addAction(snoozeAction(progressIcon(percent)))
        // ColorOS 16 的流体云读取原生 Live Updates；部分 36 构建未编入
        // ProgressStyle/提升常驻通知，失败时退回普通进度通知而不是让进程崩溃。
        val liveUpdates = Build.VERSION.SDK_INT >= 36 && runCatching {
            builder.setStyle(progressStyle(total, goal, percent))
        }.isSuccess
        if (liveUpdates) {
            if (supportsPromotedOngoing) builder.setRequestPromotedOngoing(true)
            val chipText = if (metrics.today.reached) {
                context.getString(R.string.live_chip_reached)
            } else {
                context.getString(R.string.live_chip_remaining, metrics.remainingMl)
            }
            runCatching<Unit> { builder.setShortCriticalText(chipText) }
        } else {
            builder.setProgress(goal, total.coerceAtMost(goal), false)
            builder.setStyle(Notification.BigTextStyle().setBigContentTitle(title).bigText(text))
            if (supportsPromotedOngoing) {
                // 提升到锁屏与状态栏的实时活动位；该方法只在部分 36 之后构建里存在。
                builder.setRequestPromotedOngoing(true)
            }
        }
        return builder.build()
    }

    /**
     * 四段进度条对应每日目标的四个刻度，进度值与段长同单位（0–100）。
     * 胶囊与锁屏卡片都读取这里的分段、追踪图标和颜色。
     */
    @RequiresApi(36)
    private fun progressStyle(totalMl: Int, goalMl: Int, percent: Int): Notification.ProgressStyle {
        val style = Notification.ProgressStyle()
            .setProgress(percent)
            .setProgressTrackerIcon(Icon.createWithResource(context, progressIcon(percent)))
        SEGMENT_COLORS.forEachIndexed { index, color ->
            style.addProgressSegment(Notification.ProgressStyle.Segment(25).setId(index).setColor(color))
        }
        return style
    }

    /** 水杯里的水面跟着进度走：没到一半是空杯，过半是半杯，达标是满杯。 */
    private fun progressIcon(percent: Int): Int = when {
        percent >= FULL_PERCENT -> R.drawable.ic_aqua_stat_full
        percent >= HALF_PERCENT -> R.drawable.ic_aqua_stat_medium
        else -> R.drawable.ic_aqua_stat_low
    }

    private fun progressPercent(totalMl: Int, goalMl: Int): Int =
        if (goalMl <= 0) 0 else (totalMl * 100 / goalMl).coerceIn(0, 100)

    private fun confirmAction(type: DrinkType, amountMl: Int, iconRes: Int = R.drawable.ic_aqua_stat): Notification.Action {
        val intent = Intent(context, ReminderActionReceiver::class.java)
            .setAction(ReminderActionReceiver.ACTION_CONFIRM)
            .putExtra(ReminderActionReceiver.EXTRA_AMOUNT, amountMl)
            .putExtra(ReminderActionReceiver.EXTRA_TYPE_ID, type.id)
        val label = context.getString(R.string.notif_confirm, amountMl)
        return Notification.Action.Builder(
            Icon.createWithResource(context, iconRes),
            label,
            broadcast(ReminderActionReceiver.ACTION_CONFIRM, amountMl, intent),
        ).build()
    }

    private fun snoozeAction(iconRes: Int = R.drawable.ic_aqua_stat): Notification.Action {
        val intent = Intent(context, ReminderActionReceiver::class.java)
            .setAction(ReminderActionReceiver.ACTION_SNOOZE)
            .putExtra(ReminderActionReceiver.EXTRA_MINUTES, SNOOZE_MINUTES)
        return Notification.Action.Builder(
            Icon.createWithResource(context, iconRes),
            context.getString(R.string.notif_snooze, SNOOZE_MINUTES),
            broadcast(ReminderActionReceiver.ACTION_SNOOZE, SNOOZE_MINUTES, intent),
        ).build()
    }

    private fun broadcast(action: String, requestCodeTail: Int, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_BASE + action.hashCode() + requestCodeTail,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        context,
        REQUEST_OPEN,
        Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun notify(id: Int, notification: Notification, label: String) {
        runCatching { manager.notify(id, notification) }
            .onFailure { Log.w(TAG, "$label 投递失败：${it.message}") }
    }

    private companion object {
        /**
         * 反射探测而不是比较 SDK_INT：setRequestPromotedOngoing 只出现在部分 36 之后的
         * 平台构建里，直接调用会在缺少该方法的系统上抛 NoSuchMethodError。
         */
        val supportsPromotedOngoing: Boolean by lazy {
            runCatching {
                Notification.Builder::class.java
                    .getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
            }.isSuccess
        }

        const val TAG = "AquaNotifier"
        const val REQUEST_BASE = 5000
        const val REQUEST_OPEN = 5900
        const val SNOOZE_MINUTES = 10
        const val HALF_PERCENT = 50
        const val FULL_PERCENT = 100
        val ACCENT = Color.rgb(0, 128, 255)
        val SEGMENT_COLORS = intArrayOf(
            Color.rgb(98, 212, 255),
            Color.rgb(46, 134, 255),
            Color.rgb(123, 97, 255),
            Color.rgb(255, 95, 162),
        )
    }
}
