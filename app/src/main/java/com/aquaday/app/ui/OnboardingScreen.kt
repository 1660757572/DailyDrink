package com.aquaday.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.aquaday.app.R
import com.aquaday.app.ui.components.AquaCard
import com.aquaday.app.ui.components.AquaChip
import com.aquaday.app.ui.components.AquaDivider
import com.aquaday.app.ui.components.AquaLongButton
import com.aquaday.app.ui.components.AquaSliderRow
import com.aquaday.app.ui.components.AquaStepperRow
import com.aquaday.app.ui.components.AquaSwitchRow
import com.aquaday.app.ui.components.AvatarCropDialog
import com.itos.coui.backdrop.couiBackdrop
import com.itos.coui.backdrop.rememberCouiBackdrop
import com.itos.coui.scroll.couiVerticalScroll
import com.itos.coui.slider.Slider
import com.itos.coui.theme.CouiTheme

private const val STEP_COUNT = 5

/** 首次启动的引导：个人信息 → 目标 → 提醒 → 权限，风格对齐 iPhone 激活流程。 */
@Composable
fun OnboardingScreen(state: AquaUiState, viewModel: AquaViewModel) {
    val snapshot = state.metrics?.snapshot ?: return
    val settings = snapshot.settings
    val dark = rememberIsDark(snapshot.themeMode)
    var step by rememberSaveable { mutableIntStateOf(0) }
    var nicknameDraft by remember { mutableStateOf(snapshot.nickname) }
    var goalDraft by remember { mutableFloatStateOf(snapshot.goalMl.toFloat()) }
    var reminderEnabled by remember { mutableStateOf(settings.enabled) }
    var intervalDraft by remember { mutableFloatStateOf(settings.intervalMinutes.toFloat()) }
    var startHour by remember { mutableIntStateOf(settings.activeStartHour) }
    var endHour by remember { mutableIntStateOf(settings.activeEndHour) }
    val backdrop = rememberCouiBackdrop()
    var pendingAvatar by remember { mutableStateOf<Uri?>(null) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
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

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPermissions() }

    fun commitGoal() = viewModel.setGoal(goalDraft.roundToStep(100))
    fun commitReminder() = viewModel.updateSettings {
        it.copy(
            enabled = reminderEnabled,
            intervalMinutes = intervalDraft.roundToStep(5),
            activeStartHour = startHour,
            activeEndHour = endHour,
        )
    }

    fun finish() {
        commitGoal()
        commitReminder()
        viewModel.completeOnboarding()
    }

    fun next() {
        when (step) {
            0, 1 -> step += 1
            2 -> {
                commitGoal()
                step = 3
            }
            3 -> {
                commitReminder()
                step = 4
            }
            else -> finish()
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxSize().couiBackdrop(backdrop)) {
            AquaPageBackground(dark = dark, modifier = Modifier.fillMaxSize())
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                    .padding(horizontal = 24.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().height(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (step > 0) {
                        IconButton(onClick = { step -= 1 }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.onboarding_back),
                            )
                        }
                    } else {
                        Spacer(Modifier.size(48.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { finish() }) {
                        Text(
                            text = stringResource(R.string.onboarding_skip),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                AnimatedContent(
                    targetState = step,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally(tween(260)) { it / 6 } + fadeIn(tween(260)))
                                .togetherWith(slideOutHorizontally(tween(200)) { -it / 6 } + fadeOut(tween(160)))
                        } else {
                            (slideInHorizontally(tween(260)) { -it / 6 } + fadeIn(tween(260)))
                                .togetherWith(slideOutHorizontally(tween(200)) { it / 6 } + fadeOut(tween(160)))
                        }
                    },
                    label = "onboarding-step",
                ) { current ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .couiVerticalScroll(rememberScrollState())
                            .padding(bottom = 128.dp),
                    ) {
                        Spacer(Modifier.height(if (current == 0) 40.dp else 4.dp))
                        StepHero(current, if (current == 0) 136 else 88)
                        Spacer(Modifier.height(if (current == 0) 28.dp else 16.dp))
                        Text(
                            text = stringResource(
                                when (current) {
                                    0 -> R.string.onboarding_welcome_title
                                    1 -> R.string.onboarding_profile_title
                                    2 -> R.string.onboarding_goal_title
                                    3 -> R.string.onboarding_reminder_title
                                    else -> R.string.onboarding_permission_title
                                },
                            ),
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(
                                when (current) {
                                    0 -> R.string.onboarding_welcome_body
                                    1 -> R.string.onboarding_profile_body
                                    2 -> R.string.onboarding_goal_body
                                    3 -> R.string.onboarding_reminder_body
                                    else -> R.string.onboarding_permission_body
                                },
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                        )
                        Spacer(Modifier.height(28.dp))
                        when (current) {
                            1 -> ProfileStep(
                                nickname = nicknameDraft,
                                avatarPath = snapshot.avatarImage,
                                onNicknameChange = {
                                    nicknameDraft = it.take(NICKNAME_MAX)
                                    viewModel.setNickname(nicknameDraft)
                                },
                                onPickAvatar = {
                                    avatarPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                            )
                            2 -> GoalStep(goalDraft = goalDraft, onGoalChange = { goalDraft = it })
                            3 -> ReminderStep(
                                enabled = reminderEnabled,
                                onEnabledChange = { reminderEnabled = it },
                                intervalDraft = intervalDraft,
                                onIntervalChange = { intervalDraft = it },
                                startHour = startHour,
                                endHour = endHour,
                                onStartChange = { startHour = it },
                                onEndChange = { endHour = it },
                            )
                            4 -> {
                                PermissionSettingsScreen(state, viewModel)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.onboarding_summary_permission),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StepDots(step)
            Spacer(Modifier.height(14.dp))
            AquaLongButton(
                text = stringResource(
                    if (step == STEP_COUNT - 1) R.string.onboarding_start else R.string.onboarding_continue,
                ),
                icon = null,
                onClick = { next() },
                backdrop = backdrop,
                dark = dark,
            )
        }
    }
}

/** 每步标题图标：欢迎步用应用图标，其余用与该步内容对应的 Material 图标。 */
@Composable
private fun ColumnScope.StepHero(step: Int, sizeDp: Int) {
    val modifier = Modifier
        .size(sizeDp.dp)
        .align(Alignment.CenterHorizontally)
    if (step == 0) {
        Image(
            painter = painterResource(R.drawable.ic_aqua_launcher_foreground),
            contentDescription = null,
            modifier = modifier,
        )
    } else {
        val hero: Pair<ImageVector, Color> = when (step) {
            1 -> Icons.Filled.Person to TintProfile
            2 -> Icons.Filled.Flag to TintWindow
            3 -> Icons.Filled.Alarm to TintReminder
            else -> Icons.Filled.NotificationsActive to TintPermission
        }
        Icon(
            imageVector = hero.first,
            contentDescription = null,
            tint = hero.second,
            modifier = modifier,
        )
    }
}

@Composable
private fun StepDots(step: Int) {    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(STEP_COUNT) { index ->
            val active = index == step
            Box(
                Modifier
                    .width(if (active) 18.dp else 6.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (active) CouiTheme.colorScheme.accent
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f),
                    ),
            )
        }
    }
}

/** 引导里的个人信息步：只放头像与昵称，图片仍走系统选择器，与「我的 → 个人信息」同一套写入。 */
@Composable
private fun ProfileStep(
    nickname: String,
    avatarPath: String?,
    onNicknameChange: (String) -> Unit,
    onPickAvatar: () -> Unit,
) {
    AquaCard {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AquaAvatar(path = avatarPath, nickname = nickname, size = 88.dp)
            Spacer(Modifier.height(14.dp))
            AquaChip(text = stringResource(R.string.action_choose_image), selected = true, onClick = onPickAvatar)
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = nickname,
                onValueChange = onNicknameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.profile_nickname_label)) },
                placeholder = { Text(stringResource(R.string.profile_nickname_placeholder)) },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.profile_nickname_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GoalStep(goalDraft: Float, onGoalChange: (Float) -> Unit) {
    val accent = CouiTheme.colorScheme.accent
    AquaCard {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "${goalDraft.roundToStep(50)}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                Text(
                    text = " ml",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Slider(
                modifier = Modifier.fillMaxWidth(),
                value = goalDraft,
                onValueChange = onGoalChange,
                valueRange = 800f..5000f,
                steps = 41,
            )
            Spacer(Modifier.height(10.dp))
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(1500, 2000, 2500, 3000).forEach { preset ->
                    AquaChip(
                        text = stringResource(R.string.onboarding_preset, preset),
                        selected = goalDraft.roundToStep(50) == preset,
                        onClick = { onGoalChange(preset.toFloat()) },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.onboarding_goal_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReminderStep(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    intervalDraft: Float,
    onIntervalChange: (Float) -> Unit,
    startHour: Int,
    endHour: Int,
    onStartChange: (Int) -> Unit,
    onEndChange: (Int) -> Unit,
) {
    AquaCard {
        Column(Modifier.padding(vertical = 2.dp)) {
            AquaSwitchRow(
                title = stringResource(R.string.setting_reminder_on),
                summary = stringResource(R.string.setting_reminder_summary),
                checked = enabled,
                icon = Icons.Filled.Alarm,
                iconTint = TintReminder,
                onCheckedChange = onEnabledChange,
            )
            AquaDivider()
            AquaSliderRow(
                title = stringResource(R.string.setting_interval),
                value = intervalDraft,
                onValueChange = onIntervalChange,
                valueRange = 15f..240f,
                steps = 44,
                valueText = stringResource(R.string.setting_interval_summary, intervalDraft.roundToStep(5)),
            )
            AquaDivider()
            AquaStepperRow(
                title = stringResource(R.string.setting_window_start),
                value = hourLabel(startHour),
                onDecrease = { onStartChange((startHour - 1).coerceAtLeast(0)) },
                onIncrease = { onStartChange((startHour + 1).coerceAtMost(endHour - 1)) },
                summary = stringResource(R.string.setting_window_summary, startHour, endHour),
                icon = Icons.Filled.Bedtime,
                iconTint = TintWindow,
            )
            AquaDivider()
            AquaStepperRow(
                title = stringResource(R.string.setting_window_end),
                value = hourLabel(endHour),
                onDecrease = { onEndChange((endHour - 1).coerceAtLeast(startHour + 1)) },
                onIncrease = { onEndChange((endHour + 1).coerceAtMost(24)) },
                icon = Icons.Filled.Schedule,
                iconTint = TintSchedule,
            )
        }
    }
}
