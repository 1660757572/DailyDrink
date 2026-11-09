package com.aquaday.app.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aquaday.app.R
import com.aquaday.app.data.ColorStyle
import com.aquaday.app.data.DEFAULT_BLUR_DP
import com.aquaday.app.data.DEFAULT_SCRIM
import com.aquaday.app.data.MAX_BLUR_DP
import com.aquaday.app.data.MIN_CONTAINER_OPACITY
import com.aquaday.app.data.ThemeMode
import com.aquaday.app.ui.components.AquaCard
import com.aquaday.app.ui.components.AquaChip
import com.aquaday.app.ui.components.AquaDivider
import com.aquaday.app.ui.components.AquaRow
import com.aquaday.app.ui.components.AquaSectionTitle
import com.aquaday.app.ui.components.AquaSliderRow
import com.aquaday.app.ui.components.AquaTileShape
import com.aquaday.app.ui.components.MenuBackdropWhileOpen
import com.itos.coui.backdrop.CouiBackdrop
import com.itos.coui.chip.ChipTab
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.Dialog
import com.itos.coui.menu.CouiMenuItem
import com.itos.coui.menu.CouiMenuDefaults
import com.itos.coui.menu.Menu
import com.itos.coui.menu.couiMenuAnchor
import com.itos.coui.menu.rememberCouiMenuAnchor
import com.itos.coui.slider.Slider
import com.itos.coui.theme.CouiTheme
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AppearanceScreen(
    state: AquaUiState,
    viewModel: AquaViewModel,
    backdrop: CouiBackdrop,
    dialogBackdrop: CouiBackdrop,
    dark: Boolean,
) {
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
    val themeAnchor = rememberCouiMenuAnchor()
    var themeMenuOpen by remember { mutableStateOf(false) }
    val themeOptions = listOf(
        ThemeMode.System to stringResource(R.string.setting_theme_system),
        ThemeMode.Light to stringResource(R.string.setting_theme_light),
        ThemeMode.Dark to stringResource(R.string.setting_theme_dark),
    )
    AquaCard {
        AquaRow(
            modifier = Modifier.couiMenuAnchor(themeAnchor),
            title = stringResource(R.string.setting_theme),
            summary = themeOptions.firstOrNull { it.first == snapshot.themeMode }?.second
                ?: stringResource(R.string.setting_theme_system),
            icon = Icons.Filled.DarkMode,
            iconTint = TintTheme,
            onClick = { themeAnchor.open { themeMenuOpen = true } },
            trailing = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            },
        )
        MenuBackdropWhileOpen(themeMenuOpen)
        Menu(
            expanded = themeMenuOpen,
            onDismissRequest = { themeMenuOpen = false },
            items = themeOptions.map { (mode, label) ->
                CouiMenuItem(mode, label, selected = mode == snapshot.themeMode)
            },
            onItemClick = { item ->
                viewModel.setThemeMode(item.value)
                themeMenuOpen = false
            },
            style = CouiMenuDefaults.style(dark),
            anchor = themeAnchor,
        )
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
                CouiChipAction(
                    label = stringResource(R.string.action_choose_image),
                    icon = Icons.Filled.Wallpaper,
                    selected = true,
                    backdrop = backdrop,
                    dark = dark,
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f),
                )
                CouiChipAction(
                    label = stringResource(R.string.action_remove_image),
                    icon = Icons.Filled.Delete,
                    selected = false,
                    enabled = imagePath != null,
                    backdrop = backdrop,
                    dark = dark,
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
                Spacer(Modifier.height(8.dp))
                CouiChipAction(
                    label = stringResource(R.string.action_reset_background),
                    icon = Icons.Filled.RestartAlt,
                    selected = false,
                    backdrop = backdrop,
                    dark = dark,
                    onClick = {
                        scrimDraft = DEFAULT_SCRIM * 100f
                        blurDraft = DEFAULT_BLUR_DP
                        viewModel.setBackgroundScrim(scrimDraft.roundToInt())
                        viewModel.setBackgroundBlur(blurDraft.roundToInt())
                    },
                    modifier = Modifier.fillMaxWidth(),
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
            backdrop = dialogBackdrop,
            dark = dark,
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
            backdrop = dialogBackdrop,
            dark = dark,
            onDismiss = { showColorPicker = false },
            onConfirm = { chosen ->
                viewModel.setCustomAccent(chosen.toArgb())
                viewModel.setColorStyle(ColorStyle.Custom)
                showColorPicker = false
            },
        )
    }
}

/** 图片操作用 COUI 分类胶囊；低版本没有浮动材质，退回同形状的普通胶囊。 */
@Composable
private fun CouiChipAction(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    backdrop: CouiBackdrop,
    dark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        CouiChipActionImpl(label, icon, selected, backdrop, dark, onClick, modifier, enabled)
    } else {
        AquaChip(
            text = label,
            selected = selected,
            onClick = onClick,
            modifier = modifier,
            leading = icon,
            enabled = enabled,
        )
    }
}

@RequiresApi(34)
@Composable
private fun CouiChipActionImpl(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    backdrop: CouiBackdrop,
    dark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
) {
    ChipTab(
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        label = label,
        backdrop = backdrop,
        dark = dark,
        enabled = enabled,
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
    )}

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

/** 取色弹窗的内容区会吞掉拖动手势，滑块放在独立的 COUI 弹窗里。 */
@Composable
private fun CustomColorWarningDialog(
    backdrop: CouiBackdrop,
    dark: Boolean,
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        dark = dark,
        title = stringResource(R.string.color_style_custom),
        message = stringResource(R.string.color_custom_warning),
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.action_continue),
            onClick = onContinue,
            style = CouiDialogActionStyle.Primary,
            colors = aquaPrimaryActionColors(MaterialTheme.colorScheme.primary),
        ),
        dismissAction = CouiDialogAction(text = stringResource(R.string.action_cancel), onClick = onDismiss),
        material = if (CouiDialogDefaults.isMaterialSupported()) CouiDialogDefaults.material(dark) else null,
        backdrop = backdrop,
    )
}

@Composable
private fun CustomColorPickerDialog(
    initial: Color,
    backdrop: CouiBackdrop,
    dark: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit,
) {
    val hsv = remember(initial) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toArgb(), it) }
    }
    var hue by remember(hsv) { mutableFloatStateOf(hsv[0]) }
    var saturation by remember(hsv) { mutableFloatStateOf(hsv[1].coerceAtLeast(0.45f)) }
    val picked = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 0.92f)))

    Dialog(
        onDismissRequest = onDismiss,
        dark = dark,
        title = stringResource(R.string.color_style_custom),
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.action_use),
            onClick = { onConfirm(picked) },
            style = CouiDialogActionStyle.Primary,
            colors = aquaPrimaryActionColors(CouiTheme.colorScheme.accent),
        ),
        dismissAction = CouiDialogAction(text = stringResource(R.string.action_cancel), onClick = onDismiss),
        material = if (CouiDialogDefaults.isMaterialSupported()) CouiDialogDefaults.material(dark) else null,
        backdrop = backdrop,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = CouiDialogDefaults.contentPadding)
                .padding(bottom = 8.dp),
        ) {
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
            Slider(
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
            Slider(
                modifier = Modifier.fillMaxWidth(),
                value = saturation,
                onValueChange = { saturation = it },
                valueRange = 0f..1f,
            )
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
