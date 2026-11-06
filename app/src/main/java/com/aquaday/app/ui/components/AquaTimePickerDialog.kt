package com.aquaday.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aquaday.app.R
import com.aquaday.app.ui.aquaPrimaryActionColors
import com.itos.coui.dialog.CouiAlertDialog
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.theme.CouiTheme

/**
 * COUI-Kit 没有时间选择器，这里把 Material3 的表盘装进 COUI 弹窗，
 * 保持外框、按钮与配色一致；结果统一裁剪到今天当前时刻之前。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AquaTimePickerDialog(
    initialMinutes: Int,
    maxMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = (initialMinutes / 60).coerceIn(0, 23),
        initialMinute = initialMinutes % 60,
        is24Hour = true,
    )
    val accent = CouiTheme.colorScheme.accent
    val colors = MaterialTheme.colorScheme

    CouiAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.dialog_time_pick),
        message = stringResource(R.string.dialog_time_summary),
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.action_confirm),
            onClick = { onConfirm((state.hour * 60 + state.minute).coerceIn(0, maxMinutes)) },
            style = CouiDialogActionStyle.Primary,
            colors = aquaPrimaryActionColors(accent),
        ),
        dismissAction = CouiDialogAction(text = stringResource(R.string.action_cancel), onClick = onDismiss),
        content = {
            // 表盘自带边距，内容区不再加水平内边距，窄屏也能完整显示。
            Box(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = colors.surfaceVariant,
                        clockDialSelectedContentColor = colors.onPrimary,
                        clockDialContentColor = colors.onSurface,
                        selectorColor = accent,
                        containerColor = colors.surface,
                        periodSelectorBorderColor = colors.outlineVariant,
                        periodSelectorSelectedContainerColor = accent.copy(alpha = 0.18f),
                        periodSelectorContainerColor = colors.surfaceVariant.copy(alpha = 0.5f),
                        periodSelectorSelectedContentColor = accent,
                        periodSelectorContentColor = colors.onSurfaceVariant,
                    ),
                )
            }
        },
    )
}
