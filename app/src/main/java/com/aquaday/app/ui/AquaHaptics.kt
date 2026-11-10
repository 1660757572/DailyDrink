package com.aquaday.app.ui

import android.content.Context
import android.media.AudioAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.aquaday.app.data.HapticLevel
import com.aquaday.app.data.ReminderSettings

/**
 * 关键点击的触感反馈。力度映射到系统预设振动效果，用户在系统设置里关掉触感时
 * 由 Vibrator 自行静默，这里只尊重应用内的开关。
 */
class AquaHaptics internal constructor(
    private val vibrator: Vibrator?,
    private val enabled: Boolean,
    private val level: HapticLevel,
) {

    fun tap() {
        if (!enabled) return
        val vibrator = vibrator ?: return
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(effect(), HapticAudioAttributes)
    }

    private fun effect(): VibrationEffect = try {
        VibrationEffect.createPredefined(
            when (level) {
                HapticLevel.Light -> VibrationEffect.EFFECT_TICK
                HapticLevel.Standard -> VibrationEffect.EFFECT_CLICK
                HapticLevel.Strong -> VibrationEffect.EFFECT_HEAVY_CLICK
            },
        )
    } catch (_: IllegalArgumentException) {
        // 没有预设效果的设备和模拟器退回定时长＋振幅，力度仍然分三档。
        VibrationEffect.createOneShot(level.durationMs, level.amplitude)
    }
}

private val HapticLevel.durationMs: Long
    get() = when (this) {
        HapticLevel.Light -> 8
        HapticLevel.Standard -> 14
        HapticLevel.Strong -> 24
    }

private val HapticLevel.amplitude: Int
    get() = when (this) {
        HapticLevel.Light -> 70
        HapticLevel.Standard -> 150
        HapticLevel.Strong -> 255
    }

private val HapticAudioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    .build()

@Composable
fun rememberAquaHaptics(settings: ReminderSettings?): AquaHaptics {
    val context = LocalContext.current.applicationContext
    val manager = remember(context) {
        context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
    }
    return remember(manager, settings?.hapticsEnabled, settings?.hapticLevel) {
        AquaHaptics(
            vibrator = manager?.defaultVibrator,
            enabled = settings?.hapticsEnabled ?: true,
            level = settings?.hapticLevel ?: HapticLevel.Standard,
        )
    }
}

/** 关闭时振动器为 null，预览与未注入的场景都不会触发硬件振动。 */
val LocalHaptics = staticCompositionLocalOf { AquaHaptics(null, enabled = false, level = HapticLevel.Standard) }
