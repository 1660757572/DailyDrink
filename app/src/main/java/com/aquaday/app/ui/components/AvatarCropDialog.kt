package com.aquaday.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.aquaday.app.R
import com.aquaday.app.ui.aquaPrimaryActionColors
import com.itos.coui.dialog.CouiAlertDialog
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.slider.CouiSlider
import com.itos.coui.theme.CouiTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_DECODE_PX = 1440
private const val AVATAR_OUTPUT_PX = 720
private const val MAX_ZOOM = 4f

/**
 * 选择头像后先裁剪成正方形再落盘：预览用圆形蒙版，但保存的是完整方形，
 * 这样以后更换更圆的头像框也不会丢掉边角。
 */
@Composable
fun AvatarCropDialog(
    uri: Uri,
    onDismiss: () -> Unit,
    onCropped: (Bitmap) -> Unit,
) {
    val context = LocalContext.current
    val accent = CouiTheme.colorScheme.accent
    val scope = rememberCoroutineScope()
    var source by remember(uri) { mutableStateOf<Bitmap?>(null) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(uri) {
        source = withContext(Dispatchers.IO) { decodeCroppable(context, uri) }
        zoom = 1f
        offset = Offset.Zero
    }

    CouiAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.avatar_crop_title),
        message = stringResource(R.string.avatar_crop_hint),
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.action_confirm),
            enabled = source != null && viewport != IntSize.Zero,
            onClick = {
                val bitmap = source ?: return@CouiDialogAction
                scope.launch {
                    val cropped = withContext(Dispatchers.Default) {
                        cropSquare(bitmap, viewport, zoom, offset)
                    }
                    onCropped(cropped)
                }
            },
            style = CouiDialogActionStyle.Primary,
            colors = aquaPrimaryActionColors(accent),
        ),
        dismissAction = CouiDialogAction(
            text = stringResource(R.string.action_cancel),
            onClick = onDismiss,
        ),
        content = {
            Column(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.86f))
                        .onSizeChanged { viewport = it }
                        .pointerInput(viewport, source) {
                            val bitmap = source ?: return@pointerInput
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                val nextZoom = (zoom * gestureZoom).coerceIn(1f, MAX_ZOOM)
                                zoom = nextZoom
                                offset = clampOffset(offset + pan, nextZoom, viewport, bitmap)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    val bitmap = source
                    if (bitmap == null) {
                        CircularProgressIndicator(
                            color = accent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(28.dp),
                        )
                    } else {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = zoom
                                    scaleY = zoom
                                    translationX = offset.x
                                    translationY = offset.y
                                },
                        )
                        Canvas(Modifier.fillMaxSize()) {
                            val radius = size.minDimension / 2f
                            val mask = Path().apply {
                                fillType = PathFillType.EvenOdd
                                addRect(Rect(Offset.Zero, size))
                                addOval(Rect(center = center, radius = radius))
                            }
                            drawPath(mask, Color.Black.copy(alpha = 0.45f))
                            drawCircle(
                                color = Color.White.copy(alpha = 0.9f),
                                radius = radius,
                                center = center,
                                style = Stroke(width = 2.dp.toPx()),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.avatar_crop_zoom),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CouiSlider(
                    modifier = Modifier.fillMaxWidth(),
                    value = zoom,
                    onValueChange = {
                        zoom = it
                        source?.let { bitmap -> offset = clampOffset(offset, it, viewport, bitmap) }
                    },
                    valueRange = 1f..MAX_ZOOM,
                )
            }
        },
    )
}

/** ImageDecoder 会自动应用 EXIF 旋转，并统一落到软件位图，避免硬件位图无法做局部裁剪。 */
private fun decodeCroppable(context: Context, uri: Uri): Bitmap? = runCatching {
    val decoder = ImageDecoder.createSource(context.contentResolver, uri)
    ImageDecoder.decodeBitmap(decoder) { image, info, _ ->
        image.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val width = info.size.width
        val height = info.size.height
        val longest = maxOf(width, height)
        if (longest > MAX_DECODE_PX) {
            val scale = MAX_DECODE_PX.toFloat() / longest
            image.setTargetSize(
                (width * scale).toInt().coerceAtLeast(1),
                (height * scale).toInt().coerceAtLeast(1),
            )
        }
    }
}.getOrNull()

private fun baseScale(viewport: IntSize, bitmap: Bitmap): Float =
    maxOf(viewport.width.toFloat() / bitmap.width, viewport.height.toFloat() / bitmap.height)

private fun clampOffset(offset: Offset, zoom: Float, viewport: IntSize, bitmap: Bitmap): Offset {
    if (viewport == IntSize.Zero) return Offset.Zero
    val scale = baseScale(viewport, bitmap) * zoom
    val maxX = ((bitmap.width * scale - viewport.width) / 2f).coerceAtLeast(0f)
    val maxY = ((bitmap.height * scale - viewport.height) / 2f).coerceAtLeast(0f)
    return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
}

private fun cropSquare(bitmap: Bitmap, viewport: IntSize, zoom: Float, offset: Offset): Bitmap {
    val scale = baseScale(viewport, bitmap) * zoom
    val side = (minOf(viewport.width, viewport.height) / scale)
        .coerceIn(1f, minOf(bitmap.width, bitmap.height).toFloat())
    val centerX = bitmap.width / 2f - offset.x / scale
    val centerY = bitmap.height / 2f - offset.y / scale
    val size = side.toInt().coerceAtLeast(1)
    val left = (centerX - side / 2f).toInt().coerceIn(0, bitmap.width - size)
    val top = (centerY - side / 2f).toInt().coerceIn(0, bitmap.height - size)
    val cropped = Bitmap.createBitmap(bitmap, left, top, size, size)
    val scaled = Bitmap.createScaledBitmap(cropped, AVATAR_OUTPUT_PX, AVATAR_OUTPUT_PX, true)
    if (scaled !== cropped) cropped.recycle()
    return scaled
}
