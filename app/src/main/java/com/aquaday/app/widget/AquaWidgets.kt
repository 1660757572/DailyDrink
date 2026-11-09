package com.aquaday.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.aquaday.app.MainActivity
import com.aquaday.app.QuickActions
import com.aquaday.app.R
import com.aquaday.app.data.AquaMetrics
import com.aquaday.app.data.AchievementProgress
import com.aquaday.app.data.Achievements
import com.aquaday.app.data.ThemeMode
import com.aquaday.app.ui.resolveDark
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** 桌面小组件的统一渲染入口；样式沿用页面的圆角卡片 + 水滴蓝强调色。 */
object AquaWidgets {

    private const val TAG = "AquaWidgets"
    private const val REQUEST_OPEN = 100
    private const val REQUEST_QUICK = 200

    private const val COLOR_ACCENT = 0xFF0B84FC.toInt()
    private const val LIGHT_PRIMARY = 0xFF111114.toInt()
    private const val LIGHT_SECONDARY = 0xFF5B5D63.toInt()
    private const val DARK_PRIMARY = 0xFFF2F2F7.toInt()
    private const val DARK_SECONDARY = 0xFFA1A1A6.toInt()

    private val BadgeDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("M 月 d 日")

    /** 数据变化后由界面层顺带刷新，省一次独立的 DataStore 读取。 */
    fun updateAll(context: Context, metrics: AquaMetrics) {
        val manager = AppWidgetManager.getInstance(context)
        renderEach(context, manager, metrics, AquaWidgetSmallProvider::class.java, R.layout.widget_aqua_small)
        renderEach(context, manager, metrics, AquaWidgetWideProvider::class.java, R.layout.widget_aqua_wide)
    }

    fun render(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray,
        metrics: AquaMetrics,
        layoutRes: Int,
    ) {
        val views = build(context, metrics, layoutRes)
        ids.forEach { id -> manager.updateAppWidget(id, views) }
        Log.i(TAG, "render: ${ids.size} 个小组件已刷新（layout=$layoutRes）")
    }

    private fun renderEach(
        context: Context,
        manager: AppWidgetManager,
        metrics: AquaMetrics,
        provider: Class<*>,
        layoutRes: Int,
    ) {
        val ids = manager.getAppWidgetIds(ComponentName(context, provider))
        if (ids.isNotEmpty()) render(context, manager, ids, metrics, layoutRes)
    }

    private fun build(context: Context, metrics: AquaMetrics, layoutRes: Int): RemoteViews {
        val snapshot = metrics.snapshot
        val dark = snapshot.themeMode.resolveDark(
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES,
        )
        val primary = if (dark) DARK_PRIMARY else LIGHT_PRIMARY
        val secondary = if (dark) DARK_SECONDARY else LIGHT_SECONDARY
        val percent = (metrics.goalRatio * 100).roundToInt().coerceIn(0, 100)
        val defaultCupMl = snapshot.settings.defaultCupMl

        val views = RemoteViews(context.packageName, layoutRes)
        views.setInt(R.id.widget_root, "setBackgroundResource", if (dark) R.drawable.widget_bg_dark else R.drawable.widget_bg_light)
        views.setViewVisibility(R.id.widget_icon, View.VISIBLE)
        views.setImageViewResource(R.id.widget_icon, R.drawable.ic_widget_drop)
        views.setInt(R.id.widget_icon, "setColorFilter", COLOR_ACCENT)
        views.setTextViewText(R.id.widget_label, context.getString(R.string.widget_today))
        views.setTextColor(R.id.widget_label, secondary)
        views.setTextViewText(R.id.widget_amount, "$percent%")
        views.setTextColor(R.id.widget_amount, primary)
        views.setTextViewText(
            R.id.widget_goal,
            if (metrics.today.reached) {
                context.getString(R.string.widget_reached)
            } else {
                context.getString(R.string.widget_goal_format, metrics.today.totalMl, snapshot.goalMl)
            },
        )
        views.setTextColor(R.id.widget_goal, secondary)
        views.setProgressBar(R.id.widget_progress, 100, percent, false)
        views.setTextViewText(R.id.widget_action, context.getString(R.string.widget_action_format, defaultCupMl))
        views.setTextColor(R.id.widget_action, 0xFFFFFFFF.toInt())
        views.setInt(
            R.id.widget_action,
            "setBackgroundResource",
            if (dark) R.drawable.widget_action_bg_dark else R.drawable.widget_action_bg_light,
        )

        val recent = recentAchievement(metrics)
        if (layoutRes == R.layout.widget_aqua_wide) {
            views.setInt(R.id.widget_badge_icon, "setColorFilter", if (recent == null) secondary else COLOR_ACCENT)
            views.setTextColor(R.id.widget_badge_label, secondary)
            views.setTextViewText(R.id.widget_badge_title, recent?.achievement?.title ?: context.getString(R.string.widget_badge_none))
            views.setTextColor(R.id.widget_badge_title, if (recent == null) secondary else primary)
            views.setTextViewText(
                R.id.widget_badge_meta,
                recent?.let { badgeMeta(context, it) } ?: context.getString(R.string.widget_badge_none_meta),
            )
            views.setTextColor(R.id.widget_badge_meta, secondary)
        }

        views.setOnClickPendingIntent(R.id.widget_root, openIntent(context))
        views.setOnClickPendingIntent(R.id.widget_action, quickLogIntent(context))
        return views
    }

    private fun recentAchievement(metrics: AquaMetrics): AchievementProgress? =
        Achievements.evaluate(metrics)
            .filter { it.unlocked && it.unlockedAt != null }
            .maxByOrNull { it.unlockedAt ?: 0L }

    private fun badgeMeta(context: Context, progress: AchievementProgress): String {
        val date = Instant.ofEpochMilli(progress.unlockedAt ?: 0L)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(BadgeDateFormat)
        return context.getString(R.string.widget_badge_meta, progress.achievement.tier.label, date)
    }

    private fun openIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun quickLogIntent(context: Context): PendingIntent {
        val intent = QuickActions.logIntent(context, QuickActions.DEFAULT_CUP)
        return PendingIntent.getActivity(
            context,
            REQUEST_QUICK,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
