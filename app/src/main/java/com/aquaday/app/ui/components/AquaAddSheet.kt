package com.aquaday.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.aquaday.app.R
import com.aquaday.app.ui.aquaPrimaryActionColors
import com.aquaday.app.data.DrinkType
import com.aquaday.app.ui.asClock
import com.aquaday.app.ui.DrinkIcons
import com.itos.coui.backdrop.CouiBackdrop
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.Dialog
import com.itos.coui.dialog.InputDialog
import com.itos.coui.theme.CouiTheme
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.roundToInt

private val QuickAmounts = listOf(150, 200, 300, 500)

/**
 * 喝一杯：快捷份量 + 自定义滑杆 + 饮品类型 + 记录时间。
 * 时间只允许选今天已过去的时刻，用于补记漏掉的饮水。
 */
@Composable
fun AquaAddSheet(
    defaultCupMl: Int,
    goalMl: Int,
    types: List<DrinkType>,
    backdrop: CouiBackdrop,
    dark: Boolean,
    onDismiss: () -> Unit,
    onCreateType: (String) -> Unit,
    onEditType: (DrinkType) -> Unit,
    onConfirm: (amountMl: Int, type: DrinkType, at: LocalDateTime) -> Unit,
) {
    val now = remember { LocalDateTime.now() }
    val maxMinutes = now.hour * 60 + now.minute
    var amount by remember(defaultCupMl) { mutableFloatStateOf(defaultCupMl.coerceIn(50, 1500).toFloat()) }
    // 只记 id，编辑过图标或名称后当前选中项仍能跟着快照里的最新对象刷新。
    var selectedId by remember { mutableStateOf(DrinkType.WATER.id) }
    val type = types.firstOrNull { it.id == selectedId } ?: DrinkType.WATER
    var minutes by remember { mutableIntStateOf(maxMinutes) }
    var typeDialog by remember { mutableStateOf(false) }
    var timeDialog by remember { mutableStateOf(false) }
    var pendingLabel by remember { mutableStateOf<String?>(null) }
    var typeName by remember { mutableStateOf(TextFieldValue("")) }
    val rounded = (amount / 50f).roundToInt() * 50
    val effective = (rounded * type.factor).roundToInt()
    val time = LocalTime.of(minutes / 60, minutes % 60)
    val accent = CouiTheme.colorScheme.accent

    LaunchedEffect(types, pendingLabel) {
        val wanted = pendingLabel ?: return@LaunchedEffect
        types.firstOrNull { it.label == wanted }?.let {
            selectedId = it.id
            pendingLabel = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        dark = dark,
        title = stringResource(R.string.action_add),
        message = stringResource(R.string.dialog_add_hint, goalMl),
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.dialog_add_submit, rounded),
            onClick = { onConfirm(rounded, type, LocalDateTime.of(now.toLocalDate(), time)) },
            style = CouiDialogActionStyle.Primary,
            colors = aquaPrimaryActionColors(accent),
        ),
        dismissAction = CouiDialogAction(text = stringResource(R.string.action_cancel), onClick = onDismiss),
        material = if (CouiDialogDefaults.isMaterialSupported()) CouiDialogDefaults.material(dark) else null,
        backdrop = backdrop,
    ) {
            // COUI 只给标题和正文加 contentPadding，自定义内容需要自己对齐同一 24dp。
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CouiDialogDefaults.contentPadding)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.dialog_effective, rounded, type.label, effective),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickAmounts.forEach { quick ->
                        AquaChip(
                            text = "+ $quick",
                            selected = rounded == quick,
                            onClick = { amount = quick.toFloat() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 9.dp),
                            maxLines = 1,
                        )
                    }
                }
                AquaSliderRow(
                    title = stringResource(R.string.dialog_amount),
                    value = amount,
                    onValueChange = { amount = it },
                    valueRange = 50f..1500f,
                    steps = 28,
                    valueText = "$rounded ml",
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        types.forEach { option ->
                            AquaChip(
                                text = option.label,
                                selected = option.id == selectedId,
                                onClick = { selectedId = option.id },
                                leading = DrinkIcons.vector(option.iconKey),
                                // 只在自定义色时着色；跟随强调色时交给芯片自身的选中配色。
                                leadingTint = option.tintArgb?.let { Color(it) },
                                onLongClick = { onEditType(option) },
                            )
                        }
                        AquaChip(
                            text = stringResource(R.string.dialog_custom_type),
                            selected = false,
                            onClick = {
                                typeName = TextFieldValue("")
                                typeDialog = true
                            },
                            leading = Icons.Filled.Add,
                        )
                    }
                    Text(
                        text = stringResource(R.string.dialog_factor_hint, type.label, (type.factor * 100).roundToInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AquaRow(
                    title = stringResource(R.string.dialog_time),
                    summary = stringResource(R.string.dialog_time_summary),
                    icon = Icons.Filled.Schedule,
                    onClick = { timeDialog = true },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = time.asClock(),
                                style = MaterialTheme.typography.titleMedium,
                                color = CouiTheme.colorScheme.accent,
                            )
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

    if (typeDialog) {
        val trimmed = typeName.text.trim()
        InputDialog(
            value = typeName,
            onValueChange = { next -> if (next.text.length <= 12) typeName = next },
            onDismissRequest = { typeDialog = false },
            dark = dark,
            confirmAction = CouiDialogAction(
                text = stringResource(R.string.action_confirm),
                enabled = trimmed.isNotEmpty(),
                onClick = {
                    typeDialog = false
                    pendingLabel = trimmed
                    onCreateType(trimmed)
                },
                style = CouiDialogActionStyle.Primary,
                colors = aquaPrimaryActionColors(accent),
            ),
            dismissAction = CouiDialogAction(text = stringResource(R.string.action_cancel), onClick = { typeDialog = false }),
            clearContentDescription = stringResource(R.string.dialog_clear_input),
            title = stringResource(R.string.dialog_new_type),
            message = stringResource(R.string.dialog_new_type_hint),
            placeholder = stringResource(R.string.dialog_new_type_placeholder),
            counter = "${typeName.text.length}/12",
            accent = accent,
            material = if (CouiDialogDefaults.isMaterialSupported()) CouiDialogDefaults.material(dark) else null,
            backdrop = backdrop,
        )
    }

    if (timeDialog) {
        AquaTimePickerDialog(
            initialMinutes = minutes,
            maxMinutes = maxMinutes,
            backdrop = backdrop,
            dark = dark,
            onDismiss = { timeDialog = false },
            onConfirm = { picked ->
                minutes = picked
                timeDialog = false
            },
        )
    }
}
