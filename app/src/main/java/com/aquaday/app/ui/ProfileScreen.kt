package com.aquaday.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.aquaday.app.R
import com.aquaday.app.ui.components.AquaCard
import com.aquaday.app.ui.components.AquaChip
import com.aquaday.app.ui.components.AquaRow
import com.aquaday.app.ui.components.AquaSectionTitle
import com.aquaday.app.ui.components.AvatarCropDialog
import com.itos.coui.backdrop.CouiBackdrop
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.InputDialog
import com.itos.coui.theme.CouiTheme

internal const val NICKNAME_MAX = 24

/** 个人信息：头像走系统图片选择器，昵称用 COUI 输入弹窗，确认后写入 DataStore。 */
@Composable
fun ProfileScreen(
    state: AquaUiState,
    viewModel: AquaViewModel,
    backdrop: CouiBackdrop,
    dark: Boolean,
) {
    val snapshot = state.metrics?.snapshot ?: return
    var editorOpen by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(TextFieldValue(snapshot.nickname)) }
    var pendingAvatar by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        pendingAvatar = uri
    }

    pendingAvatar?.let { uri ->
        AvatarCropDialog(
            uri = uri,
            backdrop = backdrop,
            dark = dark,
            onDismiss = { pendingAvatar = null },
            onCropped = { bitmap ->
                pendingAvatar = null
                viewModel.setAvatarBitmap(bitmap)
            },
        )
    }

    AquaSectionTitle(stringResource(R.string.profile_avatar_section))
    AquaCard {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AquaAvatar(path = snapshot.avatarImage, nickname = snapshot.nickname, size = 96.dp)
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AquaChip(
                    text = stringResource(R.string.action_choose_image),
                    selected = true,
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f),
                )
                AquaChip(
                    text = stringResource(R.string.action_remove_avatar),
                    selected = false,
                    enabled = snapshot.avatarImage != null,
                    onClick = { viewModel.setAvatarImage(null) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.profile_avatar_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    AquaSectionTitle(stringResource(R.string.profile_nickname_section))
    AquaCard {
        AquaRow(
            title = profileDisplayName(snapshot.nickname),
            summary = stringResource(R.string.profile_nickname_hint),
            icon = Icons.Filled.Person,
            onClick = {
                draft = TextFieldValue(snapshot.nickname)
                editorOpen = true
            },
            trailing = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            },
        )
    }

    if (editorOpen) {
        val trimmed = draft.text.trim()
        InputDialog(
            value = draft,
            onValueChange = { next ->
                if (next.text.length <= NICKNAME_MAX) draft = next
            },
            onDismissRequest = { editorOpen = false },
            dark = dark,
            accent = CouiTheme.colorScheme.accent,
            clearContentDescription = stringResource(R.string.dialog_clear_input),
            title = stringResource(R.string.profile_nickname_edit),
            message = stringResource(R.string.profile_nickname_hint),
            placeholder = stringResource(R.string.profile_nickname_placeholder),
            counter = "${draft.text.length} / $NICKNAME_MAX",
            confirmAction = CouiDialogAction(
                text = stringResource(R.string.action_save),
                enabled = trimmed.isNotEmpty(),
                onClick = {
                    viewModel.setNickname(trimmed)
                    editorOpen = false
                },
                style = CouiDialogActionStyle.Primary,
                colors = aquaPrimaryActionColors(CouiTheme.colorScheme.accent),
            ),
            dismissAction = CouiDialogAction(
                text = stringResource(R.string.action_cancel),
                onClick = { editorOpen = false },
            ),
            material = if (CouiDialogDefaults.isMaterialSupported()) CouiDialogDefaults.material(dark) else null,
            backdrop = backdrop,
        )
    }
}
