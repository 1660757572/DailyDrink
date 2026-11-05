package com.aquaday.app.ui

import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aquaday.app.data.ThemeMode
import com.itos.coui.shape.CouiShape
import com.itos.coui.theme.CouiColorScheme
import com.itos.coui.theme.CouiTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val AquaPrimary = Color(0xFF0B84FC)
private val AquaPrimaryDark = Color(0xFF4FA8FF)

private val LightScheme: ColorScheme = lightColorScheme(
    primary = AquaPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3EBFF),
    onPrimaryContainer = Color(0xFF001E33),
    secondary = Color(0xFF2E9E7A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8F1E2),
    background = Color(0xFFF0F1F3),
    onBackground = Color(0xFF111114),
    surface = Color.White,
    onSurface = Color(0xFF111114),
    surfaceVariant = Color(0xFFE8E9EC),
    onSurfaceVariant = Color(0xFF5B5D63),
    outline = Color(0xFF8E9096),
    outlineVariant = Color(0xFFD9DADE),
    error = Color(0xFFB3261E),
    onError = Color.White,
    inversePrimary = Color(0xFF95C8FF),
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = AquaPrimaryDark,
    onPrimary = Color(0xFF002F52),
    primaryContainer = Color(0xFF084A78),
    onPrimaryContainer = Color(0xFFD3EBFF),
    secondary = Color(0xFF6FD3AC),
    onSecondary = Color(0xFF003829),
    secondaryContainer = Color(0xFF1B5340),
    background = Color.Black,
    onBackground = Color(0xFFF2F2F7),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFA1A1A6),
    outline = Color(0xFF6C6C70),
    outlineVariant = Color(0xFF3A3A3C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF691E17),
    inversePrimary = Color(0xFF0B84FC),
)

/**
 * Material You 只接管强调色与容器色，页面底色仍保持 ColorOS 的纯色规则；
 * 深色页面用纯黑做底，动态色只在卡片与按钮上体现。
 */
private fun ColorScheme.withDynamicAccent(dynamic: ColorScheme): ColorScheme = copy(
    primary = dynamic.primary,
    onPrimary = dynamic.onPrimary,
    primaryContainer = dynamic.primaryContainer,
    onPrimaryContainer = dynamic.onPrimaryContainer,
    inversePrimary = dynamic.inversePrimary,
    secondary = dynamic.secondary,
    onSecondary = dynamic.onSecondary,
    secondaryContainer = dynamic.secondaryContainer,
)

/** COUI 组件在消费处读取 CouiColorScheme，accent 必须与 M3 primary 保持一致。 */
private fun couiScheme(dark: Boolean, colors: ColorScheme): CouiColorScheme = CouiColorScheme(
    accent = colors.primary,
    onAccent = colors.onPrimary,
    surface = colors.surface,
    container = colors.surface,
    onSurface = colors.onSurface,
    onSurfaceVariant = colors.onSurfaceVariant,
    outlineVariant = colors.outlineVariant,
    accentOnSurface = colors.primary,
    menuSurface = colors.surface,
    checkboxAccent = if (dark) colors.inversePrimary else colors.primary,
    menuCheckboxAccent = if (dark) colors.inversePrimary else colors.primary,
)

@Composable
fun AquaTheme(dark: Boolean, dynamicColor: Boolean = false, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val base = if (dark) DarkScheme else LightScheme
        val dynamic = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        base.withDynamicAccent(dynamic)
    } else {
        if (dark) DarkScheme else LightScheme
    }
    MaterialTheme(colorScheme = colors) {
        CouiTheme(darkTheme = dark, colorScheme = couiScheme(dark, colors), materialTint = colors.surface) {
            content()
        }
    }
}

fun ThemeMode.resolveDark(systemDark: Boolean): Boolean = when (this) {
    ThemeMode.System -> systemDark
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

@Composable
fun rememberIsDark(mode: ThemeMode): Boolean = mode.resolveDark(isSystemInDarkTheme())

/** ColorOS 17 的卡片轮廓：G2 连续圆角，页面卡片统一走这里。 */
object AquaShapes {
    val card = CouiShape(22.dp)
    val tile = CouiShape(16.dp)
    val chip = CouiShape(12.dp)
}

/** 纯色页面底：浅色 #F0F1F3、深色纯黑；设置背景图片后叠加半透明遮罩保底可读性。 */
@Composable
fun AquaPageBackground(dark: Boolean, backgroundImage: String? = null, modifier: Modifier = Modifier) {
    Box(modifier.background(if (dark) Color.Black else Color(0xFFF0F1F3))) {
        if (backgroundImage == null) return@Box
        val bitmap = rememberBackgroundBitmap(backgroundImage) ?: return@Box
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    if (dark) Color.Black.copy(alpha = 0.62f) else Color(0xFFF0F1F3).copy(alpha = 0.55f),
                ),
        )
    }
}

@Composable
internal fun rememberBackgroundBitmap(path: String): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                val sample = maxOf(1, minOf(bounds.outWidth / 1080, bounds.outHeight / 1920))
                BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
                    ?.asImageBitmap()
            }.getOrNull()
        }
    }
    return bitmap
}
