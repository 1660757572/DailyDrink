package com.aquaday.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import com.aquaday.app.MainActivity
import com.aquaday.app.QuickActions
import com.aquaday.app.R
import com.aquaday.app.data.AquaMetrics
import com.aquaday.app.data.AquaSnapshot
import com.aquaday.app.data.AchievementProgress
import com.aquaday.app.data.Achievements
import com.aquaday.app.data.ColorStyle
import com.aquaday.app.ui.resolveDark
import kotlin.math.roundToInt

/** 桌面小组件的统一渲染入口；样式沿用页面的圆角卡片 + 外观设置里的强调色。 */
object AquaWidgets {

    private const val TAG = "AquaWidgets"
    private const val REQUEST_OPEN = 100
    private const val REQUEST_QUICK = 200
    private const val REQUEST_QUICK_AMOUNT = 300

    private const val LIGHT_PRIMARY = 0xFF111114.toInt()
    private const val LIGHT_SECONDARY = 0xFF5B5D63.toInt()
    private const val DARK_PRIMARY = 0xFFF2F2F7.toInt()
    private const val DARK_SECONDARY = 0xFFA1A1A6.toInt()

    private const val TRACK_ALPHA = 0x2E
    private const val RING_SIZE_DP = 108f
    private const val RING_STROKE_DP = 10f

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
        val accent = accentColor(context, snapshot, dark)
        val onAccent = if (ColorUtils.calculateLuminance(accent) > 0.5) Color.BLACK else Color.WHITE
        val primary = if (dark) DARK_PRIMARY else LIGHT_PRIMARY
        val secondary = if (dark) DARK_SECONDARY else LIGHT_SECONDARY
        val percent = (metrics.goalRatio * 100).roundToInt().coerceIn(0, 100)
        val defaultCupMl = snapshot.settings.defaultCupMl
        val wide = layoutRes == R.layout.widget_aqua_wide

        val views = RemoteViews(context.packageName, layoutRes)
        views.setInt(R.id.widget_root, "setBackgroundResource", if (dark) R.drawable.widget_bg_dark else R.drawable.widget_bg_light)
        views.setImageViewResource(R.id.widget_icon, R.drawable.ic_widget_drop)
        views.setInt(R.id.widget_icon, "setColorFilter", accent)
        views.setTextViewText(R.id.widget_label, context.getString(R.string.widget_today))
        views.setTextColor(R.id.widget_label, secondary)

        views.setInt(R.id.widget_action_bg, "setColorFilter", accent)
        views.setTextColor(R.id.widget_action_text, onAccent)
        views.setTextViewText(
            R.id.widget_action_text,
            if (wide) context.getString(R.string.widget_action_short)
            else context.getString(R.string.widget_action_format, defaultCupMl),
        )

        if (wide) {
            bindWide(context, views, metrics, dark, percent, accent, primary, secondary)
        } else {
            bindSmall(context, views, metrics, percent, accent, primary, secondary)
        }

        views.setOnClickPendingIntent(R.id.widget_root, openIntent(context))
        views.setOnClickPendingIntent(R.id.widget_action, quickLogIntent(context))
        return views
    }

    /** 2×2：百分比 + 一行进度说明 + 横向进度条 + 喝一杯。 */
    private fun bindSmall(
        context: Context,
        views: RemoteViews,
        metrics: AquaMetrics,
        percent: Int,
        accent: Int,
        primary: Int,
        secondary: Int,
    ) {
        views.setTextViewText(R.id.widget_amount, "$percent%")
        views.setTextColor(R.id.widget_amount, primary)
        views.setTextViewText(R.id.widget_drink_value, metrics.today.totalMl.toString())
        views.setTextColor(R.id.widget_drink_value, primary)
        views.setTextViewText(
            R.id.widget_drink_suffix,
            context.getString(
                if (metrics.today.reached) R.string.widget_drink_suffix_reached else R.string.widget_drink_suffix,
                metrics.snapshot.goalMl,
            ),
        )
        views.setTextColor(R.id.widget_drink_suffix, secondary)
        views.setInt(R.id.widget_progress_track, "setColorFilter", ColorUtils.setAlphaComponent(accent, TRACK_ALPHA))
        views.setInt(R.id.widget_progress_fill, "setColorFilter", accent)
        views.setInt(R.id.widget_progress_fill, "setImageLevel", percent * 100)
    }

    /** 4×2：首页大卡片的结构——圆环 + 已喝/目标/剩余/连续 + 最近徽章 + 快捷容量。 */
    private fun bindWide(
        context: Context,
        views: RemoteViews,
        metrics: AquaMetrics,
        dark: Boolean,
        percent: Int,
        accent: Int,
        primary: Int,
        secondary: Int,
    ) {
        val snapshot = metrics.snapshot
        views.setTextViewText(R.id.widget_percent, "$percent%")
        views.setTextColor(R.id.widget_percent, primary)
        // 小组件里不能实例化应用自定义 Drawable，圆环只能画成位图再交给 ImageView。
        views.setImageViewBitmap(R.id.widget_ring, ringBitmap(context, percent, accent))

        views.setTextViewText(R.id.widget_target, context.getString(R.string.widget_target_format, snapshot.goalMl))
        views.setTextColor(R.id.widget_target, secondary)
        views.setTextViewText(R.id.widget_drink_value, metrics.today.totalMl.toString())
        views.setTextColor(R.id.widget_drink_value, primary)
        val remain = (snapshot.goalMl - metrics.today.totalMl).coerceAtLeast(0)
        views.setTextViewText(
            R.id.widget_remaining,
            if (metrics.today.reached) {
                context.getString(R.string.widget_remaining_reached, metrics.streakDays)
            } else {
                context.getString(R.string.widget_remaining_format, remain, metrics.streakDays)
            },
        )
        views.setTextColor(R.id.widget_remaining, secondary)

        val recent = recentAchievement(metrics)
        views.setInt(R.id.widget_badge_icon, "setColorFilter", if (recent == null) secondary else accent)
        views.setTextViewText(
            R.id.widget_badge_title,
            recent?.achievement?.title ?: context.getString(R.string.widget_badge_none),
        )
        views.setTextColor(R.id.widget_badge_title, if (recent == null) secondary else primary)

        QuickAmounts.forEachIndexed { index, amount ->
            val chip = quickChips[index]
            views.setTextViewText(chip, context.getString(R.string.shortcut_amount_short, amount))
            views.setTextColor(chip, secondary)
            views.setInt(chip, "setBackgroundResource", if (dark) R.drawable.widget_chip_bg_dark else R.drawable.widget_chip_bg_light)
            views.setOnClickPendingIntent(chip, quickAmountIntent(context, index, amount))
        }
    }

    /** 与首页圆环同口径：轨道用强调色 18% 透明，弧线用实色、圆头、从 12 点方向顺时针。 */
    private fun ringBitmap(context: Context, percent: Int, accent: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val size = (RING_SIZE_DP * density).roundToInt().coerceAtLeast(1)
        val stroke = RING_STROKE_DP * density
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = (size - stroke) / 2f
        val center = size / 2f
        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            color = accent
            alpha = TRACK_ALPHA
        }
        val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            color = accent
        }
        canvas.drawCircle(center, center, radius, track)
        val progress = percent.coerceIn(0, 100)
        if (progress > 0) {
            val box = RectF(center - radius, center - radius, center + radius, center + radius)
            canvas.drawArc(box, -90f, 360f * progress / 100f, false, arc)
        }
        return bitmap
    }

    /** 与 AquaTheme 同一套规则：预设色板 / 取色 / 自定义色，再按深浅模式保证可读性。 */
    private fun accentColor(context: Context, snapshot: AquaSnapshot, dark: Boolean): Int {
        val accent = when {
            snapshot.colorStyle == ColorStyle.Dynamic -> context.getColor(
                if (dark) android.R.color.system_accent1_200 else android.R.color.system_accent1_500,
            )
            snapshot.colorStyle == ColorStyle.Custom && snapshot.customAccent != null -> snapshot.customAccent
            else -> presetAccent(snapshot.colorStyle, dark)
        }
        return readableAccent(accent, dark)
    }

    private fun presetAccent(style: ColorStyle, dark: Boolean): Int = when (style) {
        ColorStyle.Grass -> if (dark) 0xFF79D68C.toInt() else 0xFF2F8F4E.toInt()
        ColorStyle.Lemon -> if (dark) 0xFFFFD24A.toInt() else 0xFFB07A00.toInt()
        ColorStyle.Mono -> if (dark) 0xFFF2F2F7.toInt() else 0xFF1C1C1E.toInt()
        else -> if (dark) 0xFF4FA8FF.toInt() else 0xFF0B84FC.toInt()
    }

    private fun readableAccent(argb: Int, dark: Boolean): Int {
        val luminance = ColorUtils.calculateLuminance(argb)
        return when {
            dark && luminance < 0.22f -> ColorUtils.blendARGB(argb, Color.WHITE, 0.45f)
            !dark && luminance > 0.62f -> ColorUtils.blendARGB(argb, Color.BLACK, 0.32f)
            else -> argb
        }
    }

    private fun recentAchievement(metrics: AquaMetrics): AchievementProgress? =
        Achievements.evaluate(metrics)
            .filter { it.unlocked && it.unlockedAt != null }
            .maxByOrNull { it.unlockedAt ?: 0L }

    private fun openIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun quickLogIntent(context: Context): PendingIntent = quickIntent(context, REQUEST_QUICK, QuickActions.DEFAULT_CUP)

    private fun quickAmountIntent(context: Context, index: Int, amountMl: Int): PendingIntent =
        quickIntent(context, REQUEST_QUICK_AMOUNT + index, amountMl)

    private fun quickIntent(context: Context, requestCode: Int, amountMl: Int): PendingIntent {
        val intent = QuickActions.logIntent(context, amountMl)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private val QuickAmounts = QuickActions.amounts

    private val quickChips = intArrayOf(
        R.id.widget_quick_150,
        R.id.widget_quick_200,
        R.id.widget_quick_300,
        R.id.widget_quick_500,
    )
}
