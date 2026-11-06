package com.aquaday.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import androidx.compose.ui.unit.IntRect
import com.itgsa.animation.sharedview.ViewAnimationConfig
import com.itgsa.animation.sharedview.ViewAnimationExtras
import com.itgsa.animation.sharedview.ViewAnimationListener
import com.itgsa.animation.sharedview.ViewAnimationManager
import com.itgsa.animation.sharedview.ViewAnimationType
import java.util.Locale

/**
 * 金标联盟一镜到底无缝动画（SharedView Kit）接入层。
 * Kit 只在编译期提供 API 壳，动画实现来自设备侧 ROM，因此能力必须按运行时协议版本判断，
 * 不能按机型或系统版本硬编码；探测失败、构造失败都当作不支持，由调用方回退到应用内过场。
 */
object Seamless {

    /** >0 表示设备声明支持，返回值即协议版本。 */
    fun isSupported(): Boolean = runCatching { ViewAnimationManager.isViewAnimationSupported() > 0 }.getOrDefault(false)

    /** 设备声明的落点坐标系：只有明确写了 WINDOW 才用窗口坐标，其余按屏幕坐标。 */
    private val boundsInWindow: Boolean by lazy {
        val meta = runCatching {
            ViewAnimationManager.getViewAnimationExtras()?.get(ViewAnimationExtras.EXTRA_BOUNDS)
        }.getOrNull()
        if (meta.isNullOrBlank()) {
            false
        } else {
            val token = meta.split('|').getOrNull(1)?.trim()?.uppercase(Locale.ROOT)
            if (token == "SCREEN") false else token == "WINDOW" || meta.uppercase(Locale.ROOT).contains("WINDOW")
        }
    }

    /** 把 Compose 节点在根视图内的区域换算成 Kit 要求的落点矩形。 */
    fun boundsOf(root: View, inRoot: IntRect): Rect {
        if (inRoot.isEmpty) return Rect()
        val offset = IntArray(2)
        if (boundsInWindow) root.getLocationInWindow(offset) else root.getLocationOnScreen(offset)
        return Rect(
            offset[0] + inRoot.left,
            offset[1] + inRoot.top,
            offset[0] + inRoot.right,
            offset[1] + inRoot.bottom,
        )
    }

    /** 截取源节点当前画面作为动画素材；区域无效或绘制失败返回 null。 */
    fun capture(root: View, inRoot: IntRect): Bitmap? {
        if (inRoot.isEmpty) return null
        val width = inRoot.width
        val height = inRoot.height
        if (width <= 0 || height <= 0) return null
        return runCatching {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                Canvas(bitmap).apply {
                    translate(-inRoot.left.toFloat(), -inRoot.top.toFloat())
                    root.draw(this)
                }
            }
        }.getOrNull()
    }

    /**
     * 用源位图启动一镜到底并打开目标页。
     * 返回 false 表示设备不支持或 Kit 未能构造动画参数，调用方需要自己完成跳转。
     */
    fun open(
        activity: Activity,
        intent: Intent,
        thumbnail: Bitmap,
        bounds: Rect,
        radiusPx: Float,
        colorArgb: Int,
    ): Boolean {
        if (!isSupported() || bounds.isEmpty) return false
        val config = ViewAnimationConfig()
            .setRadius(radiusPx.coerceAtLeast(0f))
            .setColor(colorArgb)
            .setBackAnimType(ViewAnimationType.VIEW_ANIM_DEFAULT)
            .toBundle()
        val options = runCatching {
            ViewAnimationManager.makeViewAnimationOptions(thumbnail, bounds, radiusPx.coerceAtLeast(0f), config, SilentListener)
        }.getOrNull() ?: return false
        return runCatching { activity.startActivity(intent, options.toBundle()); true }.getOrDefault(false)
    }

    /**
     * 返回前刷新落点：源卡片已经不可见或位置取不到时改用淡出，避免整场动画被 Kit 的
     * position_changed 守卫取消。必须在 finish 之前调用。
     */
    fun overrideReturn(activity: Activity, bounds: Rect?, radiusPx: Float, colorArgb: Int) {
        if (!isSupported()) return
        val config = ViewAnimationConfig().setColor(colorArgb).setRadius(radiusPx.coerceAtLeast(0f))
        if (bounds != null && !bounds.isEmpty) {
            config.setBounds(bounds)
        } else {
            config.setBackAnimType(ViewAnimationType.VIEW_ANIM_FADE_OUT)
        }
        runCatching { ViewAnimationManager.overrideViewAnimationTransition(activity, config.toBundle()) }
    }

    private object SilentListener : ViewAnimationListener {
        override fun onAnimationStart(animMode: Int) = Unit
        override fun onAnimationCancel(animMode: Int) = Unit
        override fun onAnimationEnd(animMode: Int) = Unit
    }
}

/** Compose 的 LocalContext 拿到的是 ContextWrapper，需要逐层解到真正的 Activity。 */
internal fun Context.activityOrNull(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
