package com.aquaday.app.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.itos.coui.backdrop.CouiBackdrop
import com.itos.coui.effect.CouiFloatingRenderer
import com.itos.coui.effect.CouiFloatingSurface
import com.itos.coui.effect.LocalCouiContourGlowEnabled
import com.itos.coui.effect.SpotlightPreset
import com.itos.coui.effect.couiFabFeedback
import com.itos.coui.effect.couiFloatingSurface
import com.itos.coui.effect.rememberCouiSpotlight
import com.itos.coui.floatingactionbutton.CouiFabDefaults
import com.itos.coui.motion.couiPressMotion
import com.itos.coui.motion.couiTouchMotion
import com.itos.coui.motion.rememberCouiPressMotion
import com.itos.coui.motion.rememberCouiTouchMotion
import com.itos.coui.shape.CouiCapsuleShape
import com.itos.coui.theme.CouiTheme

/**
 * COUI 没有独立的页面长按钮，这里按 CouiFab 的浮动材质与按压形变复刻：
 * 同样的 spotlight、touch/press motion 与 couiFloatingSurface，只是把形状换成整行胶囊。
 */
@Composable
fun AquaLongButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    backdrop: CouiBackdrop?,
    modifier: Modifier = Modifier,
    dark: Boolean = CouiTheme.isDark,
    height: Dp = 56.dp,
) {
    if (backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        CouiLongButton(text, icon, onClick, backdrop, modifier, dark, height)
    } else {
        PlainLongButton(text, icon, onClick, modifier, height)
    }
}

@RequiresApi(34)
@Composable
private fun CouiLongButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    backdrop: CouiBackdrop,
    modifier: Modifier,
    dark: Boolean,
    height: Dp,
) {
    val shape = CouiCapsuleShape
    val colors = CouiTheme.colorSchemeFor(dark)
    val material = remember(colors.accent, dark) { CouiFabDefaults.material(colors.accent, dark) }
    val contour = LocalCouiContourGlowEnabled.current
    val density = LocalDensity.current.density
    val shadow = remember(material, contour, density) {
        CouiFabDefaults.shadow(material.firstColor, density, contour)
    }
    val renderer = remember { CouiFloatingRenderer.standalone() }
    val contentLayer = rememberGraphicsLayer()
    val motion = rememberCouiTouchMotion(true)
    val press = rememberCouiPressMotion()
    val interactionSource = remember { MutableInteractionSource() }
    val (lightInput, drawLight) = rememberCouiSpotlight(
        interactive = true,
        preset = SpotlightPreset.OpaqueSmall1,
        dark = dark,
    )
    var bounds by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .onSizeChanged { bounds = it }
            // 按钮盖在白色卡片上：只叠前景材质，不能回填采样到的背景，否则按钮周围会露出一块矩形。
            .couiFloatingSurface(backdrop, renderer, material, shadow, shape, contentLayer, contour,
                foregroundComposition = true) {
                CouiFloatingSurface(
                    Rect(
                        Offset(motion.translationX.value, motion.translationY.value),
                        Size(bounds.width.toFloat(), bounds.height.toFloat()),
                    ),
                    motion.scaleX.value + press.scale.value - 1f,
                    motion.scaleY.value + press.scale.value - 1f,
                    cornerWeight = 1f,
                )
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = motion.translationX.value
                    translationY = motion.translationY.value
                    scaleX = motion.scaleX.value + press.scale.value - 1f
                    scaleY = motion.scaleY.value + press.scale.value - 1f
                }
                .couiTouchMotion(motion, true)
                .couiPressMotion(press)
                .then(lightInput)
                .drawWithContent { contentLayer.record { this@drawWithContent.drawContent() } }
                .clip(shape)
                .drawBehind { drawLight?.invoke(this) }
                .couiFabFeedback(dark, interactionSource)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = colors.onAccent)
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onAccent,
                )
            }
        }
    }
}

/** API 31–33 没有浮动材质，退回同色的普通胶囊按钮。 */
@Composable
private fun PlainLongButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier,
    height: Dp,
) {
    val accent = CouiTheme.colorScheme.accent
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(accent)
            .clickable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = CouiTheme.colorScheme.onAccent)
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = CouiTheme.colorScheme.onAccent,
            )
        }
    }
}
