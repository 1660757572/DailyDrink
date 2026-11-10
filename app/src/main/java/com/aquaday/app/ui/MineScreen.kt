package com.aquaday.app.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.RestartAlt
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aquaday.app.R
import com.aquaday.app.data.ColorStyle
import com.aquaday.app.data.ThemeMode
import com.aquaday.app.ui.components.AquaCard
import com.aquaday.app.ui.components.AquaCardHorizontalPadding
import com.aquaday.app.ui.components.AquaCardShape
import com.aquaday.app.ui.components.AquaCardVerticalPadding
import com.aquaday.app.ui.components.AquaDivider
import com.aquaday.app.ui.components.AquaRow
import com.aquaday.app.ui.components.AquaSectionTitle
import com.aquaday.app.ui.components.AquaSliderRow
import com.itos.coui.backdrop.CouiBackdrop
import com.itos.coui.dialog.CouiDialogAction
import com.itos.coui.dialog.CouiDialogActionStyle
import com.itos.coui.dialog.CouiDialogDefaults
import com.itos.coui.dialog.Dialog
import com.itos.coui.icons.CouiIcons

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MineScreen(
    state: AquaUiState,
    viewModel: AquaViewModel,
    onOpenProfile: () -> Unit,
    onOpenReminder: () -> Unit,
    onOpenPermission: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenAbout: () -> Unit,
    dialogBackdrop: CouiBackdrop,
    dark: Boolean,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val metrics = state.metrics ?: return
    val snapshot = metrics.snapshot
    val settings = snapshot.settings
    var goalDraft by remember(snapshot.goalMl) { mutableFloatStateOf(snapshot.goalMl.toFloat()) }
    var cupDraft by remember(settings.defaultCupMl) { mutableFloatStateOf(settings.defaultCupMl.toFloat()) }
    var showReset by remember { mutableStateOf(false) }
    val profileAvatar = sharedIconModifier(sharedTransitionScope, animatedVisibilityScope, AquaSharedKeys.Profile)
    val reminderIcon = sharedIconModifier(sharedTransitionScope, animatedVisibilityScope, AquaSharedKeys.Reminder)
    val permissionIcon = sharedIconModifier(sharedTransitionScope, animatedVisibilityScope, AquaSharedKeys.Permission)
    val appearanceIcon = sharedIconModifier(sharedTransitionScope, animatedVisibilityScope, AquaSharedKeys.Appearance)
    val aboutIcon = sharedIconModifier(sharedTransitionScope, animatedVisibilityScope, AquaSharedKeys.About)

    AquaCard(contentPadding = false) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(AquaCardShape)
                .clickable(onClick = onOpenProfile)
                .padding(horizontal = AquaCardHorizontalPadding, vertical = AquaCardVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AquaAvatar(
                path = snapshot.avatarImage,
                nickname = snapshot.nickname,
                size = 52.dp,
                modifier = profileAvatar,
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = profileDisplayName(snapshot.nickname),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.profile_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Chevron()
        }
    }

    Spacer(Modifier.height(16.dp))
    AquaSectionTitle(stringResource(R.string.mine_section_target))
    AquaCard(contentPadding = false) {
        // COUI 滑杆保留 48dp 触摸高度，轨道下方多出约 16dp 看不见空白；下边距收掉后与其它卡片留白对齐。
        Column(
            Modifier.padding(
                start = AquaCardHorizontalPadding,
                end = AquaCardHorizontalPadding,
                top = AquaCardVerticalPadding,
                bottom = 4.dp,
            ),
        ) {
            AquaSliderRow(
                title = stringResource(R.string.setting_goal),
                value = goalDraft,
                onValueChange = { goalDraft = it },
                onValueChangeFinished = { viewModel.setGoal(goalDraft.roundToStep(100)) },
                valueRange = 800f..5000f,
                steps = 41,
                valueText = "${goalDraft.roundToStep(100)} ml",
                summary = stringResource(R.string.setting_goal_summary, snapshot.goalMl),
            )
            AquaDivider()
            AquaSliderRow(
                title = stringResource(R.string.setting_default_cup),
                value = cupDraft,
                onValueChange = { cupDraft = it },
                onValueChangeFinished = { viewModel.updateSettings { it.copy(defaultCupMl = cupDraft.roundToStep(50)) } },
                valueRange = 100f..600f,
                steps = 9,
                valueText = "${cupDraft.roundToStep(50)} ml",
                summary = stringResource(R.string.setting_default_cup_summary),
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    AquaSectionTitle(stringResource(R.string.mine_section_settings))
    AquaCard {
        AquaRow(
            title = stringResource(R.string.mine_subpage_reminder),
            summary = stringResource(
                R.string.mine_reminder_summary,
                settings.intervalMinutes,
                hourLabel(settings.activeStartHour),
                hourLabel(settings.activeEndHour),
            ),
            icon = Icons.Filled.Alarm,
            iconTint = TintReminder,
            iconModifier = reminderIcon,
            onClick = onOpenReminder,
            trailing = { Chevron() },
        )
        AquaDivider()
        AquaRow(
            title = stringResource(R.string.mine_subpage_permission),
            summary = if (state.permissions.notifications && state.permissions.exactAlarms && state.permissions.location) {
                stringResource(R.string.mine_permission_summary_ready)
            } else {
                stringResource(
                    R.string.mine_permission_summary_missing,
                    listOf(state.permissions.notifications, state.permissions.exactAlarms, state.permissions.location)
                        .count { !it },
                )
            },
            icon = CouiIcons.Filled.Bell,
            iconTint = TintPermission,
            iconModifier = permissionIcon,
            onClick = onOpenPermission,
            trailing = { Chevron() },
        )
        AquaDivider()
        AquaRow(
            title = stringResource(R.string.mine_subpage_appearance),
            summary = appearanceSummary(snapshot.themeMode, snapshot.colorStyle, snapshot.backgroundImage != null),
            icon = CouiIcons.Filled.DarkLight,
            iconTint = TintTheme,
            iconModifier = appearanceIcon,
            onClick = onOpenAppearance,
            trailing = { Chevron() },
        )
    }

    Spacer(Modifier.height(16.dp))
    AquaSectionTitle(stringResource(R.string.mine_section_data))
    AquaCard {
        AquaRow(
            title = stringResource(R.string.setting_reset),
            summary = stringResource(R.string.setting_reset_summary),
            icon = Icons.Filled.RestartAlt,
            iconTint = TintReset,
            onClick = { showReset = true },
            trailing = {
                Text(
                    text = stringResource(R.string.action_confirm),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            },
        )
        AquaDivider()
        AquaRow(
            title = stringResource(R.string.setting_about),
            summary = stringResource(R.string.setting_about_summary),
            icon = CouiIcons.Filled.BookOpen,
            iconTint = TintAbout,
            iconModifier = aboutIcon,
            onClick = onOpenAbout,
            trailing = { Chevron() },
        )
    }
    Spacer(Modifier.height(20.dp))
    Text(
        text = stringResource(R.string.mine_footer, snapshot.goalMl, metrics.eventCount),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    if (showReset) {
        Dialog(
            onDismissRequest = { showReset = false },
            dark = dark,
            title = stringResource(R.string.setting_reset),
            message = stringResource(R.string.setting_reset_confirm),
            confirmAction = CouiDialogAction(
                text = stringResource(R.string.action_confirm),
                onClick = {
                    showReset = false
                    viewModel.resetAll()
                },
                style = CouiDialogActionStyle.Primary,
                colors = CouiDialogDefaults.primaryActionColors(AquaDanger),
            ),
            dismissAction = CouiDialogAction(text = stringResource(R.string.action_cancel), onClick = { showReset = false }),
            material = if (CouiDialogDefaults.isMaterialSupported()) CouiDialogDefaults.material(dark) else null,
            backdrop = dialogBackdrop,
        )
    }
}

@Composable
private fun Chevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun sharedIconModifier(
    scope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    key: String,
): Modifier = with(scope) {
    Modifier.sharedElement(rememberSharedContentState(key), animatedVisibilityScope)
}

@Composable
private fun appearanceSummary(mode: ThemeMode, colorStyle: ColorStyle, hasBackground: Boolean): String {
    val parts = buildList {
        add(stringResource(R.string.mine_appearance_guide))
        add(
            stringResource(
                when (mode) {
                    ThemeMode.System -> R.string.setting_theme_system
                    ThemeMode.Light -> R.string.setting_theme_light
                    ThemeMode.Dark -> R.string.setting_theme_dark
                },
            ),
        )
        if (colorStyle != ColorStyle.Default) add(colorStyle.label())
        if (hasBackground) add(stringResource(R.string.mine_appearance_background))
    }
    return parts.joinToString(" · ")
}
