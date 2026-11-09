package com.aquaday.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.aquaday.app.reminder.AquaNotifier
import com.aquaday.app.reminder.ReminderScheduler
import java.io.File
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class AquaPermissions(
    val notifications: Boolean,
    val exactAlarms: Boolean,
    val location: Boolean = false,
)

/** 数据出口：任何写入之后统一同步成就、实时活动与提醒闹钟。 */
class HydrationRepository(private val context: Context) {

    private val store = HydrationStore(context)
    private val scheduler = ReminderScheduler(context)
    private val notifier = AquaNotifier(context)

    val metrics: Flow<AquaMetrics> = store.snapshot.map { HydrationMetrics.compute(it) }

    suspend fun currentMetrics(): AquaMetrics = HydrationMetrics.compute(store.snapshot.first())

    fun permissions(): AquaPermissions = AquaPermissions(
        notifications = notifier.notificationsEnabled(),
        exactAlarms = scheduler.canScheduleExact(),
        location = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED,
    )

    /** 供界面展示下一次提醒时间；设置关闭时返回 null。 */
    fun nextReminderAt(metrics: AquaMetrics): LocalDateTime? {
        val settings = metrics.snapshot.settings
        if (!settings.enabled) return null
        return scheduler.nextFire(settings, metrics.snapshot.reminderAnchor)
    }

    /** 返回本次新点亮的成就；调用方只用于提示，状态已经落盘。 */
    suspend fun synchronize(metrics: AquaMetrics): List<AchievementProgress> {
        val snapshot = metrics.snapshot
        val fresh = Achievements.evaluate(metrics)
            .filter { it.unlocked && !snapshot.unlocked.containsKey(it.achievement.id) }
        if (fresh.isNotEmpty()) {
            store.markUnlocked(fresh.map { it.achievement.id })
            fresh.last().let { notifier.postAchievement(it.achievement.title, it.achievement.summary) }
            Log.i(TAG, "synchronize: 新点亮 ${fresh.size} 个成就")
        }
        notifier.applyReminderPreferences(snapshot.settings)
        syncLiveActivity(metrics)
        scheduler.apply(snapshot.settings, snapshot.reminderAnchor)
        return fresh
    }

    suspend fun logDrink(
        amountMl: Int,
        type: DrinkType = DrinkType.WATER,
        at: LocalDateTime = LocalDateTime.now(),
    ) {
        Log.i(TAG, "logDrink: $amountMl ml / ${type.label} @ $at")
        store.logDrink(amountMl, type, at)
    }

    suspend fun addDrinkType(label: String): DrinkType? = store.addDrinkType(label)

    suspend fun updateDrinkType(id: String, label: String?, iconKey: String, tintArgb: Int?) =
        store.updateDrinkType(id, label, iconKey, tintArgb)

    suspend fun deleteEvent(id: Long) = store.deleteEvent(id)

    suspend fun setGoal(goalMl: Int) = store.setGoal(goalMl)

    suspend fun setThemeMode(mode: ThemeMode) = store.setThemeMode(mode)

    suspend fun setColorStyle(style: ColorStyle) = store.setColorStyle(style)

    suspend fun setCustomAccent(argb: Int?) = store.setCustomAccent(argb)

    suspend fun completeOnboarding() = store.setOnboarded(true)

    /** 图片复制到应用私有目录，避免相册条目被删除后背景丢失；旧文件随后清理。 */
    suspend fun setBackgroundImage(uri: Uri?) = swapImage(
        uri = uri,
        prefix = "background",
        previous = store.snapshot.first().backgroundImage,
        write = store::setBackgroundImage,
    )

    suspend fun setBackgroundScrim(percent: Int) = store.setBackgroundScrim(percent)

    suspend fun setBackgroundBlur(dp: Int) = store.setBackgroundBlur(dp)

    suspend fun setContainerOpacity(percent: Int) = store.setContainerOpacity(percent)

    suspend fun setNickname(nickname: String) = store.setNickname(nickname)

    /** 头像与背景同一套落盘逻辑，只是前缀不同。 */
    suspend fun setAvatarImage(uri: Uri?) = swapImage(
        uri = uri,
        prefix = "avatar",
        previous = store.snapshot.first().avatarImage,
        write = store::setAvatarImage,
    )

    /** 裁剪结果直接编码成 PNG 落盘，省去再经过一次 content:// 读取。 */
    suspend fun setAvatarBitmap(bitmap: Bitmap) = withContext(Dispatchers.IO) {
        val previous = store.snapshot.first().avatarImage
        val target = File(context.filesDir, "avatar_${System.currentTimeMillis()}.img")
        val saved = runCatching {
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            target.length() > 0
        }.getOrDefault(false)
        if (saved) {
            store.setAvatarImage(target.absolutePath)
            previous?.let { older -> if (older != target.absolutePath) File(older).delete() }
        } else {
            target.delete()
            Log.w(TAG, "setAvatarBitmap: 头像写入失败")
        }
    }

    private suspend fun swapImage(
        uri: Uri?,
        prefix: String,
        previous: String?,
        write: suspend (String?) -> Unit,
    ) = withContext(Dispatchers.IO) {
        if (uri == null) {
            write(null)
            previous?.let { File(it).delete() }
            return@withContext
        }
        val target = File(context.filesDir, "${prefix}_${System.currentTimeMillis()}.img")
        val copied = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            target.length() > 0
        }.getOrDefault(false)
        if (copied) {
            write(target.absolutePath)
            previous?.let { older -> if (older != target.absolutePath) File(older).delete() }
        } else {
            target.delete()
            Log.w(TAG, "swapImage: 无法读取所选图片（$prefix）")
        }
    }

    suspend fun updateSettings(transform: (ReminderSettings) -> ReminderSettings) =
        store.updateSettings(transform)

    suspend fun resetAll() {
        scheduler.clear()
        notifier.hideLiveActivity()
        notifier.dismissReminder()
        store.snapshot.first().backgroundImage?.let { File(it).delete() }
        store.resetAll()
    }

    /** 重启、每日兜底：按当前配置重建闹钟与实时活动。 */
    suspend fun restore() {
        val metrics = currentMetrics()
        syncLiveActivity(metrics)
        scheduler.apply(metrics.snapshot.settings, metrics.snapshot.reminderAnchor)
    }

    // ---- 通知动作入口（广播接收器内没有数据流收集器，需要显式同步）----

    suspend fun onReminderDue() {
        val metrics = currentMetrics()
        if (metrics.today.reached) {
            Log.i(TAG, "onReminderDue: 今日已达标，安静结束本轮提醒")
            notifier.hideLiveActivity()
            return
        }
        store.markReminderFired()
        notifier.postReminder(metrics, metrics.snapshot.events.maxOfOrNull { it.at })
        synchronize(currentMetrics())
    }

    suspend fun confirmDrink(amountMl: Int, typeId: String) {
        notifier.dismissReminder()
        val type = currentMetrics().snapshot.typeOf(typeId)
        store.logDrink(amountMl, type)
        synchronize(currentMetrics())
        Log.i(TAG, "confirmDrink: 通知内确认 $amountMl ml / ${type.label}")
    }

    suspend fun snoozeReminder(minutes: Int) {
        notifier.dismissReminder()
        scheduler.snooze(minutes)
    }

    private fun syncLiveActivity(metrics: AquaMetrics) {
        val settings = metrics.snapshot.settings
        if (!settings.liveActivityEnabled) {
            notifier.hideLiveActivity()
            return
        }
        val nextFire = scheduler.nextFireMillis(settings, metrics.snapshot.reminderAnchor)
        notifier.showLiveActivity(metrics, nextFire)
    }

    private companion object {
        const val TAG = "HydrationRepository"
    }
}
