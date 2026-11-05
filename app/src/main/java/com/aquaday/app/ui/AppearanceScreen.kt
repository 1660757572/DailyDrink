package com.aquaday.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aquaday.app.R
import com.aquaday.app.data.ColorStyle
import com.aquaday.app.data.MAX_BLUR_DP
import com.aquaday.app.data.MIN_CONTAINER_OPACITY
import com.aquaday.app.data.ThemeMode
import com.aquaday.app.ui.components.AquaCard
import com.aquaday.app.ui.components.AquaChip
import com.aquaday.app.ui.components.AquaDivider
import com.aquaday.app.ui.components.AquaRow
import com.aquaday.app.ui.components.AquaSectionTitle
import com.aquaday.app.ui.components.AquaSegmented
import com.aquaday.app.ui.components.AquaSliderRow
import com.aquaday.app.ui.components.AquaTileShape
import com.itos.coui.dialog.CouiAlertDialog
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.slider.CouiSlider
import com.itos.coui.theme.CouiTheme
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AppearanceScreen(state: AquaUiState, viewModel: AquaViewModel) {
    val snapshot = state.metrics?.snapshot ?: return
    val imagePath = snapshot.backgroundImage
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.setBackgroundImage(uri)
    }
    var showColorWarning by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var scrimDraft by remember(snapshot.backgroundScrim) { mutableFloatStateOf(snapshot.backgroundScrim * 100f) }
    var blurDraft by remember(snapshot.backgroundBlur) { mutableFloatStateOf(snapshot.backgroundBlur) }
    var opacityDraft by remember(snapshot.containerOpacity) { mutableFloatStateOf(snapshot.containerOpacity * 100f) }

    AquaSectionTitle(stringResource(R.string.appearance_section_mode))
    AquaCard {
        Column {
            AquaRow(
                title = stringResource(R.string.setting_theme),
                summary = stringResource(R.string.mine_appearance_hint),
                icon = Icons.Filled.DarkMode,
                iconTint = TintTheme,
            )
            Spacer(Modifier.height(6.dp))
            val modes = listOf(
                stringResource(R.string.setting_theme_system),
                stringResource(R.string.setting_theme_light),
                stringResource(R.string.setting_theme_dark),
            )
            AquaSegmented(
                options = modes,
                selectedIndex = when (snapshot.themeMode) {
                    ThemeMode.System -> 0
                    ThemeMode.Light -> 1
                    ThemeMode.Dark -> 2
                },
                onSelect = { index ->
                    viewModel.setThemeMode(listOf(ThemeMode.System, ThemeMode.Light, ThemeMode.Dark)[index])
                },
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    AquaSectionTitle(stringResource(R.string.appearance_section_color))
    AquaCard {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ColorTemplates.forEach { template ->
                    ColorTemplateItem(
                        template = template,
                        selected = snapshot.colorStyle == template.style,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setColorStyle(template.style) },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.appearance_color_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            AquaDivider()
            AquaRow(
                title = stringResource(R.string.color_style_custom),
                summary = stringResource(R.string.appearance_color_custom_summary),
                icon = Icons.Filled.Colorize,
                iconTint = TintTheme,
                onClick = { showColorWarning = true },
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    color = if (snapshot.colorStyle == ColorStyle.Custom) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = CircleShape,
                                )
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(snapshot.customAccent?.let { Color(it) } ?: CouiTheme.colorScheme.accent),
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                },
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    AquaSectionTitle(stringResource(R.string.appearance_section_background))
    AquaCard {
        Column {
            BackgroundPreview(imagePath, snapshot.backgroundScrim, snapshot.backgroundBlur)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AquaChip(
                    text = stringResource(R.string.action_choose_image),
                    selected = true,
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    leading = Icons.Filled.Wallpaper,
                    modifier = Modifier.weight(1f),
                )
                AquaChip(
                    text = stringResource(R.string.action_remove_image),
                    selected = false,
                    enabled = imagePath != null,
                    onClick = { viewModel.setBackgroundImage(null) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.setting_background_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (imagePath != null) {
                Spacer(Modifier.height(6.dp))
                AquaDivider()
                AquaSliderRow(
                    title = stringResource(R.string.setting_background_scrim),
                    value = scrimDraft,
                    onValueChange = { scrimDraft = it },
                    onValueChangeFinished = { viewModel.setBackgroundScrim(scrimDraft.roundToInt()) },
                    valueRange = 0f..95f,
                    steps = 18,
                    valueText = "${scrimDraft.roundToInt()}%",
                    summary = stringResource(R.string.setting_background_scrim_summary),
                )
                AquaDivider()
                AquaSliderRow(
                    title = stringResource(R.string.setting_background_blur),
                    value = blurDraft,
                    onValueChange = { blurDraft = it },
                    onValueChangeFinished = { viewModel.setBackgroundBlur(blurDraft.roundToInt()) },
                    valueRange = 0f..MAX_BLUR_DP,
                    steps = 29,
                    valueText = "${blurDraft.roundToInt()} dp",
                    summary = stringResource(R.string.setting_background_blur_summary),
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    AquaSectionTitle(stringResource(R.string.appearance_section_container))
    AquaCard {
        AquaSliderRow(
            title = stringResource(R.string.setting_container_opacity),
            value = opacityDraft,
            onValueChange = { opacityDraft = it },
            onValueChangeFinished = { viewModel.setContainerOpacity(opacityDraft.roundToInt()) },
            valueRange = (MIN_CONTAINER_OPACITY * 100f)..100f,
            steps = 11,
            valueText = "${opacityDraft.roundToInt()}%",
            summary = stringResource(R.string.setting_container_opacity_summary),
        )
    }

    if (showColorWarning) {
        CustomColorWarningDialog(
            onDismiss = { showColorWarning = false },
            onContinue = {
                showColorWarning = false
                showColorPicker = true
            },
        )
    }

    if (showColorPicker) {
        CustomColorPickerDialog(
            initial = snapshot.customAccent?.let { Color(it) } ?: CouiTheme.colorScheme.accent,
            onDismiss = { showColorPicker = false },
            onConfirm = { chosen ->
                viewModel.setCustomAccent(chosen.toArgb())
                viewModel.setColorStyle(ColorStyle.Custom)
                showColorPicker = false
            },
        )
    }
}

private data class ColorTemplate(val style: ColorStyle, val color: Color?)

/** 固定模板与动态取色并排；自定义颜色单独放一行，避免色板过密。 */
private val ColorTemplates = listOf(
    ColorTemplate(ColorStyle.Default, Color(0xFF0B84FC)),
    ColorTemplate(ColorStyle.Dynamic, null),
    ColorTemplate(ColorStyle.Grass, Color(0xFF2F8F4E)),
    ColorTemplate(ColorStyle.Lemon, Color(0xFFE0A800)),
    ColorTemplate(ColorStyle.Mono, Color(0xFF1C1C1E)),
)

private val Rainbow = Brush.sweepGradient(
    listOf(
        Color(0xFF62D4FF),
        Color(0xFF2E86FF),
        Color(0xFF7B61FF),
        Color(0xFFFF5FA2),
        Color(0xFFFFA45E),
        Color(0xFFFFD75E),
        Color(0xFF4CD8A0),
        Color(0xFF62D4FF),
    ),
)

@Composable
private fun ColorTemplateItem(
    template: ColorTemplate,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val swatch = template.color
    val checkTint = if (swatch != null && swatch.luminance() > 0.55f) Color.Black else Color.White
    Column(
        modifier = modifier
            .clip(AquaTileShape)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                    shape = CircleShape,
                )
                .padding(4.dp)
                .clip(CircleShape)
                .then(if (swatch != null) Modifier.background(swatch) else Modifier.background(Rainbow)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = checkTint)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = template.style.label(),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
internal fun ColorStyle.label(): String = stringResource(
    when (this) {
        ColorStyle.Default -> R.string.color_style_default
        ColorStyle.Dynamic -> R.string.color_style_dynamic
        ColorStyle.Grass -> R.string.color_style_grass
        ColorStyle.Lemon -> R.string.color_style_lemon
        ColorStyle.Mono -> R.string.color_style_mono
        ColorStyle.Custom -> R.string.color_style_custom
    },
)

/** CouiAlertDialog 的内容区会吞掉拖动手势，滑块必须放进独立的取色弹窗。 */
@Composable
private fun CustomColorWarningDialog(onDismiss: () -> Unit, onContinue: () -> Unit) {
    CouiAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.color_style_custom),
        message = stringResource(R.string.color_custom_warning),
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.action_continue),
            onClick = onContinue,
            style = CouiDialogActionStyle.Primary,
            colors = CouiDialogDefaults.primaryActionColors(MaterialTheme.colorScheme.primary),
        ),
        dismissAction = CouiDialogAction(text = stringResource(R.string.action_cancel), onClick = onDismiss),
    )
}

@Composable
private fun CustomColorPickerDialog(
    initial: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
) {
    val hsv = remember(initial) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toArgb(), it) }
    }
    var hue by remember(hsv) { mutableFloatStateOf(hsv[0]) }
    var saturation by remember(hsv) { mutableFloatStateOf(hsv[1].coerceAtLeast(0.45f)) }
    val picked = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 0.92f)))

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp)) {
                Text(
                    text = stringResource(R.string.color_style_custom),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(picked),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.color_preview),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = String.format(Locale.US, "#%06X", picked.toArgb() and 0xFFFFFF),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.color_hue),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CouiSlider(
                    modifier = Modifier.fillMaxWidth(),
                    value = hue,
                    onValueChange = { hue = it },
                    valueRange = 0f..360f,
                )
                Text(
                    text = stringResource(R.string.color_saturation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CouiSlider(
                    modifier = Modifier.fillMaxWidth(),
                    value = saturation,
                    onValueChange = { saturation = it },
                    valueRange = 0f..1f,
                )
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = onDismiss,
                        shape = CircleShape,
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Button(
                        onClick = { onConfirm(picked) },
                        shape = CircleShape,
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) {
                        Text(stringResource(R.string.action_use))
                    }
                }
            }
        }
    }
}

@Composable
private fun BackgroundPreview(path: String?, scrim: Float, blur: Float) {
    val image = path?.let { rememberBackgroundBitmap(it) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(AquaTileShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .border(0.7.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), AquaTileShape),
        contentAlignment = Alignment.Center,
    ) {
        if (image == null) {
            Text(
                text = stringResource(R.string.background_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (blur > 0f) Modifier.blur(blur.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded) else Modifier),
                contentScale = ContentScale.Crop,
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = scrim.coerceIn(0f, 1f))),
            )
        }
    }
}
