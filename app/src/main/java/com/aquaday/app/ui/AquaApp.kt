package com.aquaday.app.ui

import android.app.Activity
import android.os.Build
import androidx.activity.compose.PredictiveBackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aquaday.app.QuickActions
import com.aquaday.app.QuickLogRequest
import com.aquaday.app.R
import com.aquaday.app.data.Achievements
import com.aquaday.app.data.ColorStyle
import com.aquaday.app.data.DEFAULT_BLUR_DP
import com.aquaday.app.data.DEFAULT_SCRIM
import com.aquaday.app.data.DrinkType
import com.aquaday.app.data.ThemeMode
import com.aquaday.app.ui.components.AquaAddSheet
import com.aquaday.app.ui.components.DrinkTypeEditorDialog
import com.aquaday.app.ui.components.LocalMenuBackdropGate
import com.aquaday.app.ui.components.MenuBackdropGate
import com.itos.coui.backdrop.CouiBackdrop
import com.itos.coui.backdrop.couiBackdrop
import com.itos.coui.backdrop.rememberCouiBackdrop
import com.itos.coui.effect.CouiFloatingRenderer
import com.itos.coui.icons.CouiIcons
import com.itos.coui.menu.ProvideCouiMenuBackdrop
import com.itos.coui.navigation.CouiNavigationItem
import com.itos.coui.navigation.NavigationBar
import com.itos.coui.scroll.couiVerticalScroll
import com.itos.coui.theme.CouiTheme
import com.itos.coui.topappbar.CouiTopAppBarDefaults
import com.itos.coui.topappbar.TopAppBar
import com.itos.coui.topappbar.TopAppBarButton

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
fun AquaApp(
    viewModel: AquaViewModel = viewModel(),
    quickLog: MutableStateFlow<QuickLogRequest?>? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snapshot = state.metrics?.snapshot
    val context = LocalContext.current
    // 默认杯量可变，快捷方式文案跟着最新设置走；小组件的按钮文案也由仓库刷新时同步。
    val defaultCupMl = snapshot?.settings?.defaultCupMl ?: 200
    LaunchedEffect(defaultCupMl) { QuickActions.publishShortcuts(context, defaultCupMl) }
    LaunchedEffect(quickLog) {
        val events = quickLog ?: return@LaunchedEffect
        events.filterNotNull().collect { request ->
            viewModel.logQuick(request.amountMl)
            events.value = null
        }
    }
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
    // COUI 菜单的背景材质只能由 ProvideCouiMenuBackdrop 注入（取样 CompositionLocal 是库内私有的）。
    // 采样源只在菜单展开期间挂上，避免常驻的第三层全屏离屏录制增加每帧开销。
    val menuGate = remember { MenuBackdropGate() }
    CompositionLocalProvider(LocalMenuBackdropGate provides menuGate) {
        ProvideCouiMenuBackdrop(isolateContent = false) { menuSource ->
            Box(
                Modifier.fillMaxSize().then(
                    if (menuGate.openCount > 0) menuSource else Modifier,
                ),
            ) {
                AquaTheme(
                    dark = dark,
                    colorStyle = snapshot?.colorStyle ?: ColorStyle.Default,
                    customAccent = snapshot?.customAccent,
                    containerOpacity = snapshot?.containerOpacity ?: 1f,
                ) {
                    when {
                        snapshot == null -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                        !snapshot.onboarded -> OnboardingScreen(state, viewModel)
                        else -> AquaShell(state, viewModel, dark)
                    }
                }
            }
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
    // 长按饮品标签打开的编辑弹窗，按 id 取最新快照，编辑后内容跟着刷新。
    var editorTypeId by rememberSaveable { mutableStateOf<String?>(null) }
    // 徽章墙两列网格开关；底栏左附件只在成就页出现。
    var medalGrid by rememberSaveable { mutableStateOf(false) }
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
        when (medalFilter) {
            0 -> state.achievements
            1 -> state.achievements.filter { it.unlocked }
            else -> state.achievements.filter { it.achievement.category == Achievements.categories[medalFilter - 2] }
        }
    }
    val shellSnapshot = state.metrics?.snapshot
    val backgroundImage = shellSnapshot?.backgroundImage
    val backgroundScrim = shellSnapshot?.backgroundScrim ?: DEFAULT_SCRIM
    val backgroundBlur = shellSnapshot?.backgroundBlur ?: DEFAULT_BLUR_DP
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
    val medalsIcon = CouiIcons.Filled.Certificate
    val mineIcon = CouiIcons.Filled.UserCircle
    val items = remember(homeTitle, calendarTitle, medalsTitle, mineTitle, medalsIcon, mineIcon) {
        listOf(
            CouiNavigationItem(homeTitle, Icons.Outlined.Home, Icons.Filled.Home),
            CouiNavigationItem(calendarTitle, Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
            CouiNavigationItem(medalsTitle, medalsIcon, medalsIcon),
            CouiNavigationItem(mineTitle, mineIcon, mineIcon),
        )
    }
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp
    // 宿主页只在自身窗口获焦时避让键盘；输入弹窗打开后由弹窗自己处理，避免提前空出一截。
    val imePadding = if (LocalWindowInfo.current.isWindowFocused) Modifier.imePadding() else Modifier
    val bottomPadding = if (inSubpage) {
        32.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    } else {
        112.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    }
    // 每个路由保存各自的滚动位置：切换标签再回来仍停在原处，二级页退出也不会把「我的」顶回顶部。
    val routeStates = rememberSaveableStateHolder()
    var collapse by remember { mutableFloatStateOf(0f) }
    val collapseDistance = with(LocalDensity.current) { 40.dp.toPx() }
    // 双击顶栏只发信号，回到顶部由当前路由自己的滚动状态执行。
    var scrollToTopTick by remember { mutableIntStateOf(0) }

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
                    modifier = Modifier.fillMaxSize().then(imePadding),
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
                        // 每个路由只消费自己看到的新信号，切回成就页不会误触发回顶。
                        var handledTick by rememberSaveable { mutableIntStateOf(0) }
                        LaunchedEffect(scrollToTopTick, target) {
                            if (scrollToTopTick == handledTick) return@LaunchedEffect
                            handledTick = scrollToTopTick
                            if (target == route && target == AquaPage.Medals) scroll.animateScrollTo(0)
                        }
                        LaunchedEffect(scroll, target, route) {
                            snapshotFlow { (scroll.value / collapseDistance).coerceIn(0f, 1f) }
                                .collect { value -> if (target == route) collapse = value }
                        }
                        Column(
                            Modifier
                                .fillMaxSize()
                                .couiVerticalScroll(scroll)
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
                                is AquaPage -> if (target == AquaPage.Home) {
                                    AquaGreeting(state, collapse)
                                } else {
                                    AquaLargeTitle(title = stringResource(target.titleRes), collapse = collapse)
                                }
                                else -> AquaLargeTitle(title = routeTitle, collapse = collapse)
                            }
                            when (target) {
                                AquaPage.Home -> HomeScreen(
                                    state,
                                    viewModel,
                                    backdrop = pageBackdrop,
                                    dialogBackdrop = backdrop,
                                    dark = dark,
                                    onEditType = { editorTypeId = it.id },
                                )
                                AquaPage.Calendar -> CalendarScreen(
                                    state,
                                    viewModel,
                                    backdrop = pageBackdrop,
                                    dark = dark,
                                    onEditType = { editorTypeId = it.id },
                                )
                                AquaPage.Medals -> MedalsScreen(
                                    state,
                                    backdrop = pageBackdrop,
                                    dark = dark,
                                    filter = medalFilter,
                                    onFilterChange = { medalFilter = it },
                                    grid = medalGrid,
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
                                    dialogBackdrop = backdrop,
                                    dark = dark,
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                )
                                AquaSubpage.Reminder -> ReminderSettingsScreen(state, viewModel)
                                AquaSubpage.Permission -> PermissionSettingsScreen(state, viewModel)
                                AquaSubpage.Appearance -> AppearanceScreen(
                                    state,
                                    viewModel,
                                    backdrop = pageBackdrop,
                                    dialogBackdrop = backdrop,
                                    dark = dark,
                                )
                                AquaSubpage.Profile -> ProfileScreen(
                                    state = state,
                                    viewModel = viewModel,
                                    backdrop = backdrop,
                                    dark = dark,
                                )
                                AquaSubpage.About -> AboutScreen(
                                    backdrop = pageBackdrop,
                                    dialogBackdrop = backdrop,
                                    dark = dark,
                                )
                                else -> MedalDetailScreen(
                                    items = medalItems,
                                    startIndex = medalStart,
                                    backdrop = pageBackdrop,
                                    dark = dark,
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }

            TopAppBar(
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().doubleTapToTop(
                    enabled = page == AquaPage.Medals && !inSubpage,
                    onDoubleTap = { scrollToTopTick++ },
                ),
                backdrop = backdrop,
                // 背景遮罩只在无自定义背景时保留（浅色白／深色黑向下渐隐）；铺了背景图就取消遮罩，只留渐进模糊。
                colors = CouiTopAppBarDefaults.colors(
                    backgroundColor = if (backgroundImage != null) Color.Transparent else Color.Unspecified,
                ),
                navigationIcon = {
                    if (inSubpage) {
                        val onBack = { if (medalDetail) medalDetail = false else subpageIndex = -1 }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            TopAppBarButton(onClick = onBack) {
                                Icon(
                                    imageVector = CouiIcons.Filled.Back,
                                    contentDescription = stringResource(R.string.action_back),
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        } else {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = CouiIcons.Filled.Back,
                                    contentDescription = stringResource(R.string.action_back),
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                },
                title = { AquaTopBarContent(title = routeTitle, compactAlpha = collapse) },
            )

            if (!inSubpage) {
                val fabVisible = page == AquaPage.Home
                // 首页把整条底栏向左让位给加号，成就页向右让位给视图切换，其他页落回正中；弹簧参数取自 COUI 附件展开。
                val dockTarget = when (page) {
                    AquaPage.Home -> 0f
                    AquaPage.Medals -> 2f
                    else -> 1f
                }
                val dock by animateFloatAsState(dockTarget, DockSpring, label = "bottomBarDock")
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && floatingBar != null) {
                        NavigationBar(
                            modifier = Modifier.fillMaxWidth().dockBar(dock),
                            backdrop = backdrop,
                            renderer = floatingBar.bottom,
                            items = items,
                            selectedIndex = AquaPage.entries.indexOf(page),
                            // 视图切换用底栏的左附件，与右附件共用 COUI 的附件收拢动效；
                            // 隐藏期间照样传入参数，只切 leftVisible，否则没有内容可收，退场动画就没了。
                            leftIcon = if (medalGrid) Icons.Filled.List else CouiIcons.Filled.Grid,
                            leftDescription = stringResource(
                                if (medalGrid) R.string.medal_view_list else R.string.medal_view_grid,
                            ),
                            leftVisible = page == AquaPage.Medals,
                            onLeftClick = { medalGrid = !medalGrid },
                            // 加号用底栏的右附件：退场由 COUI 的附件收拢驱动。隐藏期间照样传入附件参数，
                            // 只切 rightVisible，否则附件没有内容可收，退场动画就没了。
                            rightIcon = CouiIcons.Filled.Add,
                            rightDescription = stringResource(R.string.action_add),
                            rightVisible = fabVisible,
                            onRightClick = { addOpen = true },
                            // 附件图标固定为白色，底色沿用强调色，只有接近纯白时才回退。
                            rightContainerColor = fabContainerColor(CouiTheme.colorScheme.accent),
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
                    backdrop = backdrop,
                    dark = dark,
                    onDismiss = { addOpen = false },
                    // 新建后直接进编辑器，名称、图标与颜色一次配好。
                    onCreateType = { label -> viewModel.addDrinkType(label) { created -> editorTypeId = created.id } },
                    onEditType = { editorTypeId = it.id },
                    onConfirm = { amount, type, at ->
                        viewModel.addDrink(amount, type, at)
                        addOpen = false
                    },
                )
            }

            editorTypeId?.let { id ->
                // 新建的类型要等 DataStore 回流后才出现，这里不要提前清空 id，否则编辑器一闪即过。
                val editing = state.metrics?.snapshot?.drinkTypes?.firstOrNull { it.id == id } ?: return@let
                DrinkTypeEditorDialog(
                    type = editing,
                    backdrop = backdrop,
                    dark = dark,
                    onDismiss = { editorTypeId = null },
                    onSave = { label, iconKey, tint ->
                        viewModel.updateDrinkType(editing.id, label, iconKey, tint)
                        editorTypeId = null
                    },
                    onDelete = {
                        viewModel.deleteDrinkType(editing.id)
                        editorTypeId = null
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

@Composable
private fun AquaSubpage.icon(): ImageVector = when (this) {
    AquaSubpage.Reminder -> Icons.Filled.Alarm
    AquaSubpage.Permission -> CouiIcons.Filled.Bell
    AquaSubpage.Appearance -> CouiIcons.Filled.DarkLight
    AquaSubpage.Profile -> CouiIcons.Filled.UserCircle
    AquaSubpage.About -> CouiIcons.Filled.BookOpen
}

private fun AquaSubpage.tint(): Color = when (this) {
    AquaSubpage.Reminder -> TintReminder
    AquaSubpage.Permission -> TintPermission
    AquaSubpage.Appearance -> TintTheme
    AquaSubpage.Profile -> TintProfile
    AquaSubpage.About -> TintAbout
}

/** 首页大标题换成按时段变化的问候语，页签身份仍由折叠后的顶栏小标题承担。 */
@Composable
private fun AquaGreeting(state: AquaUiState, collapse: Float) {
    val metrics = state.metrics ?: return
    val greeting = stringResource(
        when (state.now.hour) {
            in 5..10 -> R.string.home_greeting_morning
            in 11..13 -> R.string.home_greeting_noon
            in 14..17 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        },
    )
    Column(
        Modifier
            .padding(start = 4.dp, top = 4.dp, bottom = 14.dp)
            .graphicsLayer {
                alpha = (1f - collapse * 1.4f).coerceIn(0f, 1f)
                translationY = -collapse * 8.dp.toPx()
            },
    ) {
        Text(
            text = stringResource(
                R.string.home_greeting_format,
                greeting,
                profileDisplayName(metrics.snapshot.nickname),
            ),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        // 第二行在记录天数和当地天气之间轮播，取不到天气时退回只显示天数。
        val daysLine = stringResource(R.string.home_greeting_days, metrics.recordedDays)
        val weatherLines = state.weather?.let { weatherHintLines(it) }
        val hints = if (weatherLines == null) listOf(daysLine) else buildList {
            add(daysLine)
            addAll(weatherLines)
        }
        var hintIndex by remember(hints) { mutableIntStateOf(0) }
        LaunchedEffect(hints) {
            while (hints.size > 1) {
                delay(GREETING_HINT_MS)
                hintIndex = (hintIndex + 1) % hints.size
            }
        }
        Crossfade(targetState = hints[hintIndex % hints.size], label = "greetingHint") { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private const val GREETING_HINT_MS = 4200L

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

/** 只放顶栏标题；返回按钮交给 TopAppBar 的 navigationIcon 槽位，避免被栏高裁掉。 */
@Composable
private fun AquaTopBarContent(title: String, compactAlpha: Float) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.graphicsLayer { alpha = compactAlpha },
    )
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

/** 底栏让位弹簧，节奏与 COUI 附件展开一致。 */
private val DockSpring = spring<Float>(dampingRatio = 0.65f, stiffness = 130.5f)

/** 底栏为达到胶囊长度上限需要多测出的宽度，正好等于一个导航项的 70dp。 */
private val BottomBarMeasureExtra = 70.dp

/**
 * COUI 底栏的胶囊长度只由测量宽度决定：测量宽度不足时胶囊会被两侧 56dp 附件位挤短。
 * 这里多测出 70dp（正好一个导航项）让胶囊达到自然上限，再把多出的宽度从屏幕外
 * 收回来——首页整条栏左移让出加号，其他页落点归零让胶囊回到屏幕正中，长度全程不变。
 */
private fun Modifier.dockBar(progress: Float): Modifier = layout { measurable, constraints ->
    val extra = BottomBarMeasureExtra.roundToPx()
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.maxWidth + extra,
            maxWidth = constraints.maxWidth + extra,
        ),
    )
    layout(constraints.maxWidth, placeable.height) {
        placeable.place((-extra + extra / 2 * progress).roundToInt(), 0)
    }
}

/**
 * COUI 右侧附件图标固定为白色，底色必须够深。过亮的强调色压暗后保留色相，
 * 只有接近纯白（黑白主题深色档）才退回中性深色。
 */
private fun fabContainerColor(accent: Color): Color {
    val luminance = accent.luminance()
    return when {
        luminance > 0.72f -> Color(0xFF2C2C2E)
        luminance > 0.42f -> lerp(accent, Color.Black, 0.45f)
        else -> accent
    }
}

/** 只在成就页给顶栏挂双击手势，其他页面顶栏保持不消费点击。 */
private fun Modifier.doubleTapToTop(enabled: Boolean, onDoubleTap: () -> Unit): Modifier =
    if (!enabled) {
        this
    } else {
        pointerInput(enabled) { detectTapGestures(onDoubleTap = { onDoubleTap() }) }
    }
