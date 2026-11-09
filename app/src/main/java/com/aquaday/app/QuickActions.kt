package com.aquaday.app

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/** 一次来自桌面快捷方式或小组件的快速记录请求；id 只为让连续两次同容量也能各触发一次。 */
data class QuickLogRequest(val id: Long, val amountMl: Int)

/** 长按图标弹出的快速操作；容量与首页快捷芯片保持一致。 */
object QuickActions {

    const val ACTION_QUICK_LOG = "com.aquaday.app.action.QUICK_LOG"
    const val EXTRA_QUICK_ML = "com.aquaday.app.extra.QUICK_ML"

    /** 0 表示“按设置的默认一杯”。 */
    const val DEFAULT_CUP = 0

    val amounts = listOf(150, 200, 300, 500)

    fun logIntent(context: Context, amountMl: Int): Intent =
        Intent(context, MainActivity::class.java)
            .setAction(ACTION_QUICK_LOG)
            .putExtra(EXTRA_QUICK_ML, amountMl)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    /** 默认杯量会随设置变化，每次进前台都按最新值重发一遍快捷方式。 */
    fun publishShortcuts(context: Context, defaultCupMl: Int) {
        val shortcuts = buildList {
            add(
                shortcut(
                    context = context,
                    id = "quick_default",
                    shortLabel = context.getString(R.string.shortcut_default_cup_short),
                    longLabel = context.getString(R.string.shortcut_default_cup_long, defaultCupMl),
                    amountMl = DEFAULT_CUP,
                    rank = 0,
                ),
            )
            amounts.forEachIndexed { index, amount ->
                add(
                    shortcut(
                        context = context,
                        id = "quick_$amount",
                        shortLabel = context.getString(R.string.shortcut_amount_short, amount),
                        longLabel = context.getString(R.string.shortcut_amount_long, amount),
                        amountMl = amount,
                        rank = index + 1,
                    ),
                )
            }
        }
        val limit = runCatching { ShortcutManagerCompat.getMaxShortcutCountPerActivity(context) }
            .getOrDefault(shortcuts.size)
            .coerceAtLeast(1)
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts.take(limit)) }
    }

    private fun shortcut(
        context: Context,
        id: String,
        shortLabel: String,
        longLabel: String,
        amountMl: Int,
        rank: Int,
    ): ShortcutInfoCompat = ShortcutInfoCompat.Builder(context, id)
        .setShortLabel(shortLabel)
        .setLongLabel(longLabel)
        .setIcon(IconCompat.createWithResource(context, R.drawable.ic_quick_drop))
        .setIntent(logIntent(context, amountMl))
        .setRank(rank)
        .build()
}
