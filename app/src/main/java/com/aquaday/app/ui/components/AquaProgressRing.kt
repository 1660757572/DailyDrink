package com.aquaday.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.dp
import com.itos.coui.theme.CouiTheme

/**
 * 圆环进度：首页大环与日历小格共用，比例超过 1 时按满环绘制。
 * 起点在 12 点方向，顺时针生长，端点用圆头贴合 ColorOS 的水滴观感。
 */
@Composable
fun AquaProgressRing(
    ratio: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 12f,
    trackColor: Color = Color.Unspecified,
    content: @Composable () -> Unit = {},
) {
    val accent = CouiTheme.colorScheme.accent
    val track = if (trackColor.isSpecified) trackColor else MaterialTheme.colorScheme.surfaceVariant
    val progress = ratio.coerceIn(0f, 1f)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.dp.toPx().coerceAtMost((size.minDimension / 2f).coerceAtLeast(1f))
            val inset = stroke / 2f
            val arcSize = Size((size.width - stroke).coerceAtLeast(0f), (size.height - stroke).coerceAtLeast(0f))
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (progress > 0f) {
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                if (progress >= 0.999f) {
                    // 满环时首尾端点重合，补一个圆点盖住接缝。
                    drawCircle(color = accent, radius = stroke / 2f, center = Offset(size.width / 2f, inset))
                }
            }
        }
        content()
    }
}

/** 柱状达成度：日历格用细柱表示单日比例，0 时只留底色轨道。 */
@Composable
fun AquaDayBar(
    ratio: Float,
    modifier: Modifier = Modifier,
    color: Color = CouiTheme.colorScheme.accent,
    track: Color = color.copy(alpha = 0.16f),
) {
    val fraction = ratio.coerceIn(0f, 1f)
    Canvas(modifier = modifier) {
        val width = 6.dp.toPx()
        val left = (size.width - width) / 2f
        val corner = CornerRadius(width / 2f, width / 2f)
        drawRoundRect(color = track, topLeft = Offset(left, 0f), size = Size(width, size.height), cornerRadius = corner)
        if (fraction > 0f) {
            val filled = size.height * fraction
            drawRoundRect(
                color = color,
                topLeft = Offset(left, size.height - filled),
                size = Size(width, filled),
                cornerRadius = corner,
            )
        }
    }
}
