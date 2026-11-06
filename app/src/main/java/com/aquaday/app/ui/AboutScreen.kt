package com.aquaday.app.ui

import android.content.Intent
import android.os.Build
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aquaday.app.BuildConfig
import com.aquaday.app.R
import com.aquaday.app.ui.components.AquaCard
import com.aquaday.app.ui.components.AquaDivider
import com.aquaday.app.ui.components.AquaRow
import com.aquaday.app.ui.components.AquaSectionTitle
import com.itos.coui.backdrop.CouiBackdrop
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.CouiDialogPlacement
import com.itos.coui.dialog.Dialog
import com.itos.coui.effect.CouiFloatingRenderer
import com.itos.coui.floatingactionbutton.Fab
import com.itos.coui.theme.CouiTheme

private const val REPO_URL = "https://github.com/1660757572/DailyDrink"
private const val ISSUE_URL = "$REPO_URL/issues"

private enum class AboutDoc { License, ThirdParty }

@Composable
fun AboutScreen(backdrop: CouiBackdrop, dialogBackdrop: CouiBackdrop, dark: Boolean) {
    val uriHandler = LocalUriHandler.current
    var doc by remember { mutableStateOf<AboutDoc?>(null) }
    val versionName = BuildConfig.VERSION_NAME
    val versionCode = BuildConfig.VERSION_CODE

    Column(Modifier.fillMaxWidth()) {
        Hero(versionName, backdrop, dark)
        Spacer(Modifier.height(24.dp))

        AquaSectionTitle(stringResource(R.string.about_section_help))
        AquaCard {
            HelpRow(
                icon = Icons.Filled.LocalDrink,
                tint = CouiTheme.colorScheme.accent,
                title = stringResource(R.string.about_help_log_title),
                body = stringResource(R.string.about_help_log_body),
            )
            AquaDivider()
            HelpRow(
                icon = Icons.Filled.Alarm,
                tint = TintReminder,
                title = stringResource(R.string.about_help_reminder_title),
                body = stringResource(R.string.about_help_reminder_body),
            )
            AquaDivider()
            HelpRow(
                icon = Icons.Filled.EmojiEvents,
                tint = TintSchedule,
                title = stringResource(R.string.about_help_progress_title),
                body = stringResource(R.string.about_help_progress_body),
            )
            AquaDivider()
            HelpRow(
                icon = Icons.Filled.Palette,
                tint = TintProfile,
                title = stringResource(R.string.about_help_style_title),
                body = stringResource(R.string.about_help_style_body),
            )
        }

        Spacer(Modifier.height(16.dp))
        AquaSectionTitle(stringResource(R.string.about_section_stack))
        AquaCard {
            StackRow(stringResource(R.string.about_stack_ui_label), stringResource(R.string.about_stack_ui))
            AquaDivider()
            StackRow(stringResource(R.string.about_stack_lang_label), stringResource(R.string.about_stack_lang))
            AquaDivider()
            StackRow(stringResource(R.string.about_stack_data_label), stringResource(R.string.about_stack_data))
            AquaDivider()
            StackRow(stringResource(R.string.about_stack_alarm_label), stringResource(R.string.about_stack_alarm))
        }

        Spacer(Modifier.height(16.dp))
        AquaSectionTitle(stringResource(R.string.about_section_info))
        AquaCard {
            AquaRow(
                title = stringResource(R.string.setting_about_version),
                summary = stringResource(
                    R.string.about_version_line,
                    versionName,
                    versionCode,
                ),
                icon = Icons.Filled.SystemUpdate,
                iconTint = TintAbout,
            )
            AquaDivider()
            AquaRow(
                title = stringResource(R.string.about_repo),
                summary = stringResource(R.string.about_repo_summary),
                icon = Icons.Filled.Code,
                iconTint = TintAbout,
                onClick = { uriHandler.openUri(REPO_URL) },
                trailing = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
            )
            AquaDivider()
            AquaRow(
                title = stringResource(R.string.about_license),
                summary = stringResource(R.string.about_license_summary),
                icon = Icons.Filled.Description,
                iconTint = TintAbout,
                onClick = { doc = AboutDoc.License },
                trailing = { AboutChevron() },
            )
            AquaDivider()
            AquaRow(
                title = stringResource(R.string.about_third_party),
                summary = stringResource(R.string.about_third_party_summary),
                icon = Icons.Filled.Description,
                iconTint = TintAbout,
                onClick = { doc = AboutDoc.ThirdParty },
                trailing = { AboutChevron() },
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.about_footer),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    doc?.let { target ->
        DocDialog(
            title = stringResource(
                if (target == AboutDoc.License) R.string.about_license else R.string.about_third_party,
            ),
            body = stringResource(
                if (target == AboutDoc.License) R.string.about_license_body else R.string.about_third_party_body,
            ),
            backdrop = dialogBackdrop,
            dark = dark,
            onDismiss = { doc = null },
        )
    }
}

@Composable
private fun Hero(versionName: String, backdrop: CouiBackdrop, dark: Boolean) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    Column(
        Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(92.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color.White)
                .border(0.7.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_aqua_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().scale(1.45f),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                .padding(horizontal = 12.dp, vertical = 5.dp),
        ) {
            Text(
                text = stringResource(R.string.about_version_tag, versionName),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.about_slogan),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 28.dp),
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CircleAction(Icons.Filled.Code, stringResource(R.string.about_open_repo), backdrop, dark) {
                uriHandler.openUri(REPO_URL)
            }
            CircleAction(Icons.Filled.BugReport, stringResource(R.string.about_feedback), backdrop, dark) {
                uriHandler.openUri(ISSUE_URL)
            }
            CircleAction(Icons.Filled.Share, stringResource(R.string.about_share), backdrop, dark) {
                context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.about_share_text, REPO_URL))
                        },
                        context.getString(R.string.about_share),
                    ),
                )
            }
        }
    }
}

@Composable
private fun CircleAction(
    icon: ImageVector,
    description: String,
    backdrop: CouiBackdrop,
    dark: Boolean,
    onClick: () -> Unit,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        CouiCircleAction(icon, description, backdrop, dark, onClick)
    } else {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, description, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@RequiresApi(34)
@Composable
private fun CouiCircleAction(
    icon: ImageVector,
    description: String,
    backdrop: CouiBackdrop,
    dark: Boolean,
    onClick: () -> Unit,
) {
    Fab(
        backdrop = backdrop,
        renderer = remember { CouiFloatingRenderer.standalone() },
        onClick = onClick,
        dark = dark,
    ) {        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(22.dp),
            tint = CouiTheme.colorSchemeFor(dark).onAccent,
        )
    }
}

@Composable
private fun HelpRow(icon: ImageVector, tint: Color, title: String, body: String) {
    AquaRow(title = title, summary = body, icon = icon, iconTint = tint)
}

@Composable
private fun StackRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(72.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AboutChevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    )
}

@Composable
private fun DocDialog(title: String, body: String, backdrop: CouiBackdrop, dark: Boolean, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        dark = dark,
        title = title,
        message = body,
        confirmAction = CouiDialogAction(
            text = stringResource(R.string.action_confirm),
            onClick = onDismiss,
            style = CouiDialogActionStyle.Primary,
            colors = aquaPrimaryActionColors(CouiTheme.colorScheme.accent),
        ),
        placement = CouiDialogPlacement.Center,
        material = if (CouiDialogDefaults.isMaterialSupported()) CouiDialogDefaults.material(dark) else null,
        backdrop = backdrop,
    )
}
