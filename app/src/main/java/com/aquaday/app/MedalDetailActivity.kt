package com.aquaday.app

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aquaday.app.ui.Seamless
import com.aquaday.app.ui.SeamlessMedalDetail

/**
 * 徽章详情的一镜到底宿主页。
 * Kit 的无缝动画只挂在 Activity 转场上，所以详情页单独成页：进入时由「徽章墙」把源卡片的
 * 位图与落点交给 Kit，返回前用同一份落点刷新关闭动画。
 */
class MedalDetailActivity : ComponentActivity() {

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val ids = intent.getStringArrayExtra(EXTRA_IDS)?.toList() ?: emptyList()
        val start = intent.getIntExtra(EXTRA_START, 0)
        val bounds = intent.getParcelableExtra(EXTRA_BOUNDS) as? Rect
        val radius = intent.getFloatExtra(EXTRA_RADIUS, 0f)
        val color = intent.getIntExtra(EXTRA_COLOR, 0)
        val back = {
            // 落点必须在 finish 之前刷新，晚于关闭动画启动会被 Kit 的位置守卫整场取消。
            Seamless.overrideReturn(this, bounds, radius, color)
            finish()
        }
        // 侧滑与返回键同样走这条路径，否则系统直接 finish 就没有关闭动画。
        onBackPressedDispatcher.addCallback(this) { back() }
        setContent { SeamlessMedalDetail(ids = ids, startIndex = start, fillColor = color, onBack = back) }
    }

    companion object {
        const val EXTRA_IDS = "com.aquaday.app.extra.MEDAL_IDS"
        const val EXTRA_START = "com.aquaday.app.extra.MEDAL_START"
        const val EXTRA_BOUNDS = "com.aquaday.app.extra.MEDAL_BOUNDS"
        const val EXTRA_RADIUS = "com.aquaday.app.extra.MEDAL_RADIUS"
        const val EXTRA_COLOR = "com.aquaday.app.extra.MEDAL_COLOR"

        /** 组装详情转场意图：ids 是当前筛选下的徽章顺序，bounds 已换算到 Kit 声明的坐标系。 */
        fun intentTo(
            context: Context,
            ids: List<String>,
            start: Int,
            bounds: Rect,
            radiusPx: Float,
            colorArgb: Int,
        ): Intent = Intent(context, MedalDetailActivity::class.java)
            .putExtra(EXTRA_IDS, ids.toTypedArray())
            .putExtra(EXTRA_START, start)
            .putExtra(EXTRA_BOUNDS, bounds)
            .putExtra(EXTRA_RADIUS, radiusPx)
            .putExtra(EXTRA_COLOR, colorArgb)
    }
}
