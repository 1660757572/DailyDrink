package com.aquaday.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aquaday.app.R
import com.aquaday.app.ui.aquaPrimaryActionColors
import com.itos.coui.backdrop.CouiBackdrop
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.Dialog
import com.itos.coui.picker.CouiPickerDefaults
import com.itos.coui.picker.WheelPicker
import com.itos.coui.picker.couiPickerSelectionLines
import com.itos.coui.theme.CouiTheme

/**
 * COUI 时间选择：小时与分钟两列滚轮并排，选择线跨过整组滚轮，
 * 冒号只做视觉分隔；超出今天当前时刻时禁用确认。
 */
@Composable
fun AquaTimePickerDialog(
    initialMinutes: Int,
    maxMinutes: Int,
    backdrop: CouiBackdrop,
    dark: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var hour by remember(initialMinutes) { mutableIntStateOf((initialMinutes / 60).coerceIn(0, 23)) }
    var minute by remember(initialMinutes) { mutableIntStateOf(initialMinutes % 60) }
    val total = hour * 60 + minute
    val accent = CouiTheme.colorScheme.accent

    Dialog(
        onDismissRequest = onDismiss,
        dark = dark,
        title = stringResource(R.string.dialog_time_pick),
        message = stringResource(R.string.dialog_time_summary),
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.action_confirm),
            enabled = total <= maxMinutes,
            onClick = { onConfirm(total.coerceIn(0, maxMinutes)) },
            style = CouiDialogActionStyle.Primary,
            colors = aquaPrimaryActionColors(accent),
        ),
        dismissAction = CouiDialogAction(text = stringResource(R.string.action_cancel), onClick = onDismiss),
        material = if (CouiDialogDefaults.isMaterialSupported()) CouiDialogDefaults.material(dark) else null,
        backdrop = backdrop,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = CouiDialogDefaults.contentPadding)
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                Modifier.couiPickerSelectionLines(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    horizontalInset = 0.dp,
                ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WheelPicker(
                    modifier = Modifier.width(CouiPickerDefaults.columnWidth).height(CouiPickerDefaults.height),
                    value = hour,
                    onValueChange = { hour = it },
                    valueRange = 0..23,
                    label = stringResource(R.string.dialog_time_hour),
                    dark = dark,
                    formatter = { "%02d".format(it) },
                )
                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                WheelPicker(
                    modifier = Modifier.width(CouiPickerDefaults.columnWidth).height(CouiPickerDefaults.height),
                    value = minute,
                    onValueChange = { minute = it },
                    valueRange = 0..59,
                    label = stringResource(R.string.dialog_time_minute),
                    dark = dark,
                    formatter = { "%02d".format(it) },
                )
            }
        }
    }
}
