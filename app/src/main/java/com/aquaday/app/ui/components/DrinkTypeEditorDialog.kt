package com.aquaday.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.aquaday.app.R
import com.aquaday.app.data.DrinkType
import com.aquaday.app.ui.AquaDanger
import com.aquaday.app.ui.DrinkIcons
import com.aquaday.app.ui.TintPermission
import com.aquaday.app.ui.TintProfile
import com.aquaday.app.ui.TintReminder
import com.aquaday.app.ui.TintSchedule
import com.aquaday.app.ui.TintSound
import com.aquaday.app.ui.TintVibrate
import com.aquaday.app.ui.TintWindow
import com.aquaday.app.ui.aquaPrimaryActionColors
import com.aquaday.app.ui.drinkIconTint
import com.itos.coui.backdrop.CouiBackdrop
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.Dialog
import com.itos.coui.theme.CouiTheme

private val TintSwatches = listOf(
    TintReminder,
    TintWindow,
    TintSchedule,
    TintSound,
    TintVibrate,
    TintPermission,
    TintProfile,
    AquaDanger,
)

/**
 * 长按饮品标签进入：同一张弹窗里改名称、图标与图标颜色，自定义类型可删除。
 * 内容不要自己套 verticalScroll——COUI 弹窗的正文已经是可滚动的 weight 列，
 * 再叠一层会在测量时拿到无界高度直接崩溃。
 */
@Composable
fun DrinkTypeEditorDialog(
    type: DrinkType,
    backdrop: CouiBackdrop,
    dark: Boolean,
    onDismiss: () -> Unit,
    onSave: (label: String, iconKey: String, tintArgb: Int?) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember(type.id) { mutableStateOf(TextFieldValue(type.label)) }
    var iconKey by remember(type.id) { mutableStateOf(type.iconKey) }
    var tint by remember(type.id) { mutableStateOf(type.tintArgb) }
    var deleteArmed by remember(type.id) { mutableStateOf(false) }
    val accent = CouiTheme.colorScheme.accent
    val tintedColor = drinkIconTint(tint, accent)
    val trimmed = name.text.trim()

    Dialog(
        onDismissRequest = onDismiss,
        dark = dark,
        title = stringResource(R.string.drink_edit_title),
        message = stringResource(R.string.drink_edit_hint),
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.action_save),
            enabled = trimmed.isNotEmpty(),
            onClick = { onSave(trimmed, iconKey, tint) },
            style = CouiDialogActionStyle.Primary,
            colors = aquaPrimaryActionColors(accent),
        ),
        dismissAction = CouiDialogAction(text = stringResource(R.string.action_cancel), onClick = onDismiss),
        material = if (CouiDialogDefaults.isMaterialSupported()) CouiDialogDefaults.material(dark) else null,
        backdrop = backdrop,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = CouiDialogDefaults.contentPadding)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.drink_edit_name),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { next -> if (next.text.length <= 12) name = next },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    placeholder = { Text(stringResource(R.string.dialog_new_type_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.drink_edit_icon),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DrinkIcons.catalog.forEach { (key, vector) ->
                        val selected = key == iconKey
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(AquaTileShape)
                                .background(
                                    if (selected) tintedColor.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                )
                                .border(
                                    width = if (selected) 1.6.dp else 0.dp,
                                    color = if (selected) tintedColor else Color.Transparent,
                                    shape = AquaTileShape,
                                )
                                .clickable { iconKey = key },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(vector, null, Modifier.size(22.dp), tint = tintedColor)
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.drink_edit_tint),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AquaChip(
                        text = stringResource(R.string.drink_tint_follow),
                        selected = tint == null,
                        onClick = { tint = null },
                    )
                    TintSwatches.forEach { swatch ->
                        val argb = swatch.toArgb()
                        val selected = tint == argb
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(
                                    width = if (selected) 2.4.dp else 0.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape,
                                )
                                .clickable { tint = if (selected) null else argb },
                        )
                    }
                }
            }
            if (type.builtin) {
                Text(
                    text = stringResource(R.string.drink_builtin_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(AquaTileShape)
                            .background(
                                if (deleteArmed) AquaDanger.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            )
                            .clickable { if (deleteArmed) onDelete() else deleteArmed = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.Delete, null, Modifier.size(18.dp), tint = AquaDanger)
                        Text(
                            text = stringResource(
                                if (deleteArmed) R.string.drink_delete_armed else R.string.drink_delete,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AquaDanger,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (deleteArmed) {
                        Text(
                            text = stringResource(R.string.drink_delete_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
