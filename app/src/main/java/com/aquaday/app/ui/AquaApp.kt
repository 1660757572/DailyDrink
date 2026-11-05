package com.aquaday.app.ui

import android.app.Activity
import android.os.Build
import androidx.activity.compose.PredictiveBackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aquaday.app.R
import com.aquaday.app.data.Achievements
import com.aquaday.app.data.ColorStyle
import com.aquaday.app.data.DEFAULT_SCRIM
import com.aquaday.app.data.DrinkType
import com.aquaday.app.data.ThemeMode
import com.aquaday.app.ui.components.AquaAddSheet
import com.itos.coui.backdrop.couiBackdrop
import com.itos.coui.backdrop.rememberCouiBackdrop
import com.itos.coui.effect.CouiFloatingRenderer
import com.itos.coui.navigation.CouiBottomBar
import com.itos.coui.navigation.CouiNavigationItem
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.CouiTopAppBar
import com.itos.coui.topappbar.CouiTopAppBarButton

/** API 34 起 RuntimeShader 可用，悬浮底栏与顶栏按钮才具备浮动材质。 */
@RequiresApi(34)
private class ShellRenderers {
    val bottom = CouiFloatingRenderer()
}

private enum class AquaPage(val titleRes: Int) {
    Home(R.string.tab_home),
    Calendar(R.string.tab_calendar),
    Medals(R.string.tab_medal),
    Mine(R.string.tab_mine),
}

/** 「我的」里的二级设置页；路由用序号保存，返回键与顶栏返回按钮共用同一状态。 */
private enum class AquaSubpage(val titleRes: Int) {
    Reminder(R.string.mine_subpage_reminder),
    Permission(R.string.mine_subpage_permission),
    Appearance(R.string.mine_subpage_appearance),
    Profile(R.string.mine_subpage_profile),
    About(R.string.setting_about),
}

/** 徽章详情路由：单例对象，滑动切换徽章时不改变路由，避免重复过场动画。 */
private object MedalRoute

@Composable
fun AquaApp(viewModel: AquaViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snapshot = state.metrics?.snapshot
    val dark = (snapshot?.themeMode ?: ThemeMode.System).resolveDark(isSystemInDarkTheme())
    // 应用内主题可以覆盖系统深色，enableEdgeToEdge 只读系统配置，需要跟随应用主题改写状态栏图标明暗。
    val view = LocalView.current
    val activity = LocalContext.current as? Activity
    SideEffect {
        val window = activity?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).run {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    AquaTheme(
        dark = dark,
        colorStyle = snapshot?.colorStyle ?: ColorStyle.Default,
        customAccent = snapshot?.customAccent,
    ) {
        when {
            snapshot == null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            !snapshot.onboarded -> OnboardingScreen(state, viewModel)
            else -> AquaShell(state, viewModel, dark)
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun AquaShell(state: AquaUiState, viewModel: AquaViewModel, dark: Boolean) {
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPermissions() }
    var pageIndex by rememberSaveable { mutableIntStateOf(0) }
    var subpageIndex by rememberSaveable { mutableIntStateOf(-1) }
    var medalFilter by rememberSaveable { mutableIntStateOf(0) }
    var medalDetail by rememberSaveable { mutableStateOf(false) }
    var medalStart by rememberSaveable { mutableIntStateOf(0) }
    var addOpen by rememberSaveable { mutableStateOf(false) }
    val page = AquaPage.entries.getOrElse(pageIndex) { AquaPage.Home }
    val subpage = AquaSubpage.entries.getOrNull(subpageIndex)
    // 徽章详情与设置二级页共用「离开顶层」这套返回与隐藏底栏的逻辑。
    val inSubpage = subpage != null || medalDetail
    val route: Any = if (medalDetail) MedalRoute else (subpage ?: page)
    val routeTitle = stringResource(
        when (route) {
            is AquaSubpage -> route.titleRes
            is AquaPage -> route.titleRes
            else -> R.string.medal_detail_title
        },
    )
    val medalItems = remember(state.achievements, medalFilter) {
        if (medalFilter <= 0) state.achievements
        else state.achievements.filter { it.achievement.category == Achievements.categories[medalFilter - 1] }
    }
    val shellSnapshot = state.metrics?.snapshot
    val backgroundImage = shellSnapshot?.backgroundImage
    val backgroundScrim = shellSnapshot?.backgroundScrim ?: DEFAULT_SCRIM
    val backgroundBlur = shellSnapshot?.backgroundBlur ?: 0f
    var backProgress by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler(enabled = inSubpage) { progress ->
        try {
            progress.collect { backProgress = it.progress }
            if (medalDetail) medalDetail = false else subpageIndex = -1
        } finally {
            backProgress = 0f
        }
    }
    val snackbar = remember { SnackbarHostState() }
    val backdrop = rememberCouiBackdrop()
    // 页面内的悬浮件（长按钮、分类胶囊）不能采样自己所在的源，需要额外的兄弟源。
    val pageBackdrop = rememberCouiBackdrop()
    val renderers = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) runCatching { ShellRenderers() } else null
    }
    val floatingBar = renderers?.getOrNull()
    val homeTitle = stringResource(AquaPage.Home.titleRes)
    val calendarTitle = stringResource(AquaPage.Calendar.titleRes)
    val medalsTitle = stringResource(AquaPage.Medals.titleRes)
    val mineTitle = stringResource(AquaPage.Mine.titleRes)
    val items = remember(homeTitle, calendarTitle, medalsTitle, mineTitle) {
        listOf(
            CouiNavigationItem(homeTitle, Icons.Outlined.Home, Icons.Filled.Home),
            CouiNavigationItem(calendarTitle, Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
            CouiNavigationItem(medalsTitle, Icons.Outlined.EmojiEvents, Icons.Filled.EmojiEvents),
            CouiNavigationItem(mineTitle, Icons.Outlined.Person, Icons.Filled.Person),
        )
    }
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp
    val bottomPadding = if (inSubpage) {
        32.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    } else {
        112.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    }
    // 每个路由保存各自的滚动位置：切换标签再回来仍停在原处，二级页退出也不会把「我的」顶回顶部。
    val routeStates = rememberSaveableStateHolder()
    var collapse by remember { mutableFloatStateOf(0f) }
    val collapseDistance = with(LocalDensity.current) { 40.dp.toPx() }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        viewModel.consumeMessage()
    }
    renderers?.exceptionOrNull()?.let { failure ->
        LaunchedEffect(failure) {
            snackbar.showSnackbar(context.getString(R.string.renderer_error, failure.javaClass.simpleName))
        }
    }

    SharedTransitionLayout {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // 模糊源：页面内容铺满整屏，顶栏与底栏作为兄弟节点采样它。
            Box(Modifier.fillMaxSize().couiBackdrop(backdrop)) {
                // 只录制背景：页面里的按钮与胶囊采样这一层，采样方与被采样方互为兄弟。
                Box(Modifier.fillMaxSize().couiBackdrop(pageBackdrop)) {
                    AquaPageBackground(
                        dark = dark,
                        backgroundImage = backgroundImage,
                        scrim = backgroundScrim,
                        blur = backgroundBlur,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                AnimatedContent(
                    targetState = route,
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                    transitionSpec = {
                        (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.985f))
                            .togetherWith(fadeOut(tween(140)))
                    },
                    label = "aqua-page",
                ) { target ->
                    val animatedVisibilityScope = this
                    routeStates.SaveableStateProvider(routeKey(target)) {
                        val scroll = rememberScrollState()
                        LaunchedEffect(scroll, target, route) {
                            snapshotFlow { (scroll.value / collapseDistance).coerceIn(0f, 1f) }
                                .collect { value -> if (target == route) collapse = value }
                        }
                        Column(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(scroll)
                                .graphicsLayer {
                                    val gesture = if (inSubpage) backProgress else 0f
                                    val scale = 1f - 0.03f * gesture
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = gesture * 20.dp.toPx()
                                }
                                .padding(top = topPadding, bottom = bottomPadding)
                                .padding(horizontal = 16.dp),
                        ) {
                            when (target) {
                                is AquaSubpage -> SharedSubpageHeader(
                                    page = target,
                                    state = state,
                                    collapse = collapse,
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                )
                                is AquaPage -> AquaLargeTitle(title = stringResource(target.titleRes), collapse = collapse)
                                else -> AquaLargeTitle(title = routeTitle, collapse = collapse)
                            }
                            when (target) {
                                AquaPage.Home -> HomeScreen(
                                    state,
                                    viewModel,
                                    onAdd = { addOpen = true },
                                    backdrop = pageBackdrop,
                                    dark = dark,
                                )
                                AquaPage.Calendar -> CalendarScreen(state, viewModel)
                                AquaPage.Medals -> MedalsScreen(
                                    state,
                                    backdrop = pageBackdrop,
                                    dark = dark,
                                    filter = medalFilter,
                                    onFilterChange = { medalFilter = it },
                                    onOpenDetail = { index ->
                                        medalStart = index
                                        medalDetail = true
                                    },
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                )
                                AquaPage.Mine -> MineScreen(
                                    state,
                                    viewModel,
                                    onOpenProfile = { subpageIndex = AquaSubpage.Profile.ordinal },
                                    onOpenReminder = { subpageIndex = AquaSubpage.Reminder.ordinal },
                                    onOpenPermission = { subpageIndex = AquaSubpage.Permission.ordinal },
                                    onOpenAppearance = { subpageIndex = AquaSubpage.Appearance.ordinal },
                                    onOpenAbout = { subpageIndex = AquaSubpage.About.ordinal },
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                )
                                AquaSubpage.Reminder -> ReminderSettingsScreen(state, viewModel)
                                AquaSubpage.Permission -> PermissionSettingsScreen(state, viewModel)
                                AquaSubpage.Appearance -> AppearanceScreen(state, viewModel)
                                AquaSubpage.Profile -> ProfileScreen(state, viewModel)
                                AquaSubpage.About -> AboutScreen()
                                else -> MedalDetailScreen(
                                    medalItems,
                                    medalStart,
                                    this@SharedTransitionLayout,
                                    animatedVisibilityScope,
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }

            CouiTopAppBar(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(), backdrop = backdrop) {
                AquaTopBarContent(
                    title = routeTitle,
                    compactAlpha = collapse,
                    showBack = inSubpage,
                    onBack = { if (medalDetail) medalDetail = false else subpageIndex = -1 },
                )
            }

            if (!inSubpage) {
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && floatingBar != null) {
                        CouiBottomBar(
                            modifier = Modifier.fillMaxWidth().widenFloatBar(),
                            backdrop = backdrop,
                            renderer = floatingBar.bottom,
                            items = items,
                            selectedIndex = AquaPage.entries.indexOf(page),
                            rightIcon = Icons.Filled.Add,
                            rightDescription = stringResource(R.string.action_add),
                            rightVisible = true,
                            onRightClick = { addOpen = true },
                            rightContainerColor = CouiTheme.colorScheme.accent,
                            // 白色焦散在浅色页面上会糊出一圈方块光晕；rgb 保留给边缘高光，alpha 置零关掉焦散。
                            lightColor = Color.White.copy(alpha = 0f),
                            themeTint = MaterialTheme.colorScheme.background,
                            dark = dark,
                            onSelection = { pageIndex = it.coerceIn(0, AquaPage.entries.lastIndex) },
                        )
                    } else {
                        PlainTabBar(items, AquaPage.entries.indexOf(page)) { pageIndex = it }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 116.dp),
            )

            if (addOpen) {
                AquaAddSheet(
                    defaultCupMl = state.metrics?.snapshot?.settings?.defaultCupMl ?: 200,
                    goalMl = state.metrics?.snapshot?.goalMl ?: 2000,
                    types = state.metrics?.snapshot?.drinkTypes ?: DrinkType.builtins,
                    onDismiss = { addOpen = false },
                    onCreateType = { label -> viewModel.addDrinkType(label) },
                    onConfirm = { amount, type, at ->
                        viewModel.addDrink(amount, type, at)
                        addOpen = false
                    },
                )
            }
        }
    }
}

private fun routeKey(route: Any): String = when (route) {
    is AquaPage -> "page:${route.name}"
    is AquaSubpage -> "sub:${route.name}"
    else -> "sub:medal_detail"
}

internal object AquaSharedKeys {
    const val Reminder = "aqua-shared-reminder"
    const val Permission = "aqua-shared-permission"
    const val Appearance = "aqua-shared-appearance"
    const val Profile = "aqua-shared-profile"
    const val About = "aqua-shared-about"
}

/** 二级页顶部的大标题与「我的」入口共用同一枚图标，进入时由共享元素接续。 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedSubpageHeader(
    page: AquaSubpage,
    state: AquaUiState,
    collapse: Float,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val snapshot = state.metrics?.snapshot
    val sharedModifier = with(sharedTransitionScope) {
        Modifier.sharedElement(
            rememberSharedContentState(key = page.sharedKey()),
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }
    Row(
        modifier = Modifier
            .padding(start = 4.dp, top = 4.dp, bottom = 14.dp)
            .graphicsLayer {
                alpha = (1f - collapse * 1.4f).coerceIn(0f, 1f)
                translationY = -collapse * 8.dp.toPx()
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (page == AquaSubpage.Profile && snapshot != null) {
            AquaAvatar(
                path = snapshot.avatarImage,
                nickname = snapshot.nickname,
                size = 30.dp,
                modifier = sharedModifier,
            )
        } else {
            Icon(
                imageVector = page.icon(),
                contentDescription = null,
                modifier = Modifier.size(30.dp).then(sharedModifier),
                tint = page.tint(),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

private fun AquaSubpage.sharedKey(): String = when (this) {
    AquaSubpage.Reminder -> AquaSharedKeys.Reminder
    AquaSubpage.Permission -> AquaSharedKeys.Permission
    AquaSubpage.Appearance -> AquaSharedKeys.Appearance
    AquaSubpage.Profile -> AquaSharedKeys.Profile
    AquaSubpage.About -> AquaSharedKeys.About
}

private fun AquaSubpage.icon(): ImageVector = when (this) {
    AquaSubpage.Reminder -> Icons.Filled.Alarm
    AquaSubpage.Permission -> Icons.Filled.NotificationsActive
    AquaSubpage.Appearance -> Icons.Filled.DarkMode
    AquaSubpage.Profile -> Icons.Filled.Person
    AquaSubpage.About -> Icons.Filled.Info
}

private fun AquaSubpage.tint(): Color = when (this) {
    AquaSubpage.Reminder -> TintReminder
    AquaSubpage.Permission -> TintPermission
    AquaSubpage.Appearance -> TintTheme
    AquaSubpage.Profile -> TintProfile
    AquaSubpage.About -> TintAbout
}

/** 折叠顶栏：页面顶部是随内容滚动的大标题，滚出后由顶栏内的小标题接替。 */
@Composable
private fun AquaLargeTitle(title: String, collapse: Float) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .padding(start = 4.dp, top = 4.dp, bottom = 14.dp)
            .graphicsLayer {
                alpha = (1f - collapse * 1.4f).coerceIn(0f, 1f)
                translationY = -collapse * 8.dp.toPx()
            },
    )
}

@Composable
private fun AquaTopBarContent(
    title: String,
    compactAlpha: Float,
    showBack: Boolean = false,
    onBack: () -> Unit = {},
) {
    Column(Modifier.padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = if (showBack) 8.dp else 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBack) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    CouiTopAppBarButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                } else {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.graphicsLayer { alpha = compactAlpha },
            )
        }
    }
}

/**
 * CouiBottomBar 把胶囊宽度写死成「可用宽度 - 136dp」（为左右附件各留 56dp），
 * 手机上四项摊下来每项只剩六十来 dp。这里整体加宽并向左放置：
 * 多出来的宽度全给胶囊，FAB 与胶囊右端位置不变，左附件槽位本来为空，被裁掉也看不出。
 * 其中 48dp 用来拉长胶囊，另外 19dp 把胶囊左缘推到与 FAB 右缘相同的屏幕边距。
 */
private fun Modifier.widenFloatBar(): Modifier = layout { measurable, constraints ->
    val extra = 67.dp.roundToPx()
    val width = constraints.maxWidth + extra
    val placeable = measurable.measure(
        Constraints(
            minWidth = width,
            maxWidth = width,
            minHeight = constraints.minHeight,
            maxHeight = constraints.maxHeight,
        ),
    )
    layout(constraints.maxWidth, placeable.height) {
        placeable.place(-extra, 0)
    }
}

/** API 31–33 没有浮动材质，退回同色的普通底栏。 */
@Composable
private fun PlainTabBar(items: List<CouiNavigationItem>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val accent = CouiTheme.colorScheme.accent
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = { onSelect(index) })
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                val tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
                Icon(item.icon.takeIf { !selected } ?: item.selectedIcon, item.title, tint = tint)
                Spacer(Modifier.width(6.dp))
                Text(item.title, style = MaterialTheme.typography.labelMedium, color = tint)
            }
        }
    }
}

@Composable
private fun ThemeMode.label(): String = stringResource(
    when (this) {
        ThemeMode.System -> R.string.setting_theme_system
        ThemeMode.Light -> R.string.setting_theme_light
        ThemeMode.Dark -> R.string.setting_theme_dark
    },
)
