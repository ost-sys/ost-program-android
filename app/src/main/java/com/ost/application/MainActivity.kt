@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    FlowPreview::class, ExperimentalSharedTransitionApi::class
)
package com.ost.application
import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Games
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.ui.activity.main.AppBottomNavigation
import com.ost.application.ui.activity.main.ContentArea
import com.ost.application.ui.activity.converters.ConvertersActivity
import com.ost.application.ui.activity.main.MORE_ITEM_ID
import com.ost.application.ui.activity.main.MenuItemIcon
import com.ost.application.ui.activity.main.MoreBottomSheetContent
import com.ost.application.ui.activity.main.SettingsSheetContent
import com.ost.application.ui.activity.main.createMenuItems
import com.ost.application.ui.screen.settings.SettingsAction
import com.ost.application.ui.screen.settings.SettingsViewModel
import com.ost.application.ui.screen.stargazers.StargazersViewModel
import com.ost.application.ui.state.FabController
import com.ost.application.ui.state.FabSize
import com.ost.application.ui.state.LocalFabController
import com.ost.application.ui.theme.OSTToolsTheme
import com.ost.application.util.TooltipState
import com.ost.application.util.TooltipWrapper
import com.ost.application.util.AppPrefs
import com.ost.application.util.isRooted
import com.ost.application.util.rememberTooltipState
import com.ost.application.util.toast
import com.ost.application.util.tooltip
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
val LocalBottomSpacing = staticCompositionLocalOf { 0.dp }
class MainActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_OPEN_SETTINGS = "com.ost.application.extra.OPEN_SETTINGS"
    }
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        installSplashScreen()
        val openSettingsRequested = intent?.getBooleanExtra(EXTRA_OPEN_SETTINGS, false) == true
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val isExpandedScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
            OSTToolsTheme {
                MainAppStructure(
                    isExpandedScreen = isExpandedScreen,
                    initialShowSettingsSheet = openSettingsRequested
                )
            }
        }
    }
}
class SettingsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@SuppressLint("AutoboxingStateCreation", "LocalContextGetResourceValueCall")
fun MainAppStructure(isExpandedScreen: Boolean = false, initialShowSettingsSheet: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val fabController = remember { FabController() }
    val isDeviceRooted = remember { isRooted() }
    val allMenuItems = remember(isDeviceRooted) { createMenuItems(isDeviceRooted) }
    val allValidMenuItems = remember(allMenuItems) { allMenuItems.filterNotNull() }
    val bottomNavDirectItems = remember { allValidMenuItems.take(3) }
    val moreMenuItems = remember(allValidMenuItems) { allValidMenuItems.drop(bottomNavDirectItems.size) }
    var selectedScreenId by rememberSaveable { mutableStateOf(bottomNavDirectItems.first().id) }
    val scaffoldState = rememberBottomSheetScaffoldState()
    val settingsViewModel: SettingsViewModel =
        viewModel(factory = SettingsViewModelFactory(context.applicationContext as Application))
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val stargazersViewModel: StargazersViewModel = viewModel()
    val starSelectedRepo by stargazersViewModel.selectedRepo.collectAsStateWithLifecycle()
    var showSettingsSheet by remember { mutableStateOf(initialShowSettingsSheet) }
    BackHandler(enabled = showSettingsSheet) { showSettingsSheet = false }
    LaunchedEffect(key1 = settingsViewModel.action) {
        settingsViewModel.action.onEach { action ->
            when (action) {
                is SettingsAction.StartActivity -> {
                    if (action.intent.resolveActivity(context.packageManager) != null) {
                        val targetClassName = action.intent.component?.className
                        val mainActivityClassName = (context as? MainActivity)?.javaClass?.name
                        if (targetClassName != null && targetClassName != mainActivityClassName) {
                            context.startActivity(action.intent)
                        } else {
                            Log.w(
                                "MainAppStructure",
                                "Prevented launching MainActivity: ${action.intent}"
                            )
                        }
                    } else {
                        context.toast("Could not open the requested screen.")
                    }
                }
                is SettingsAction.ShowToast -> context.toast(context.getString(action.messageResId))
            }
        }.launchIn(this)
    }
    val currentSelectedItemData = allValidMenuItems.find { it.id == selectedScreenId }
    val defaultTitle = currentSelectedItemData?.let { stringResource(it.titleResId) }
        ?: stringResource(id = R.string.app_name)
    val displayTitle =
        if (selectedScreenId == "stargazers" && starSelectedRepo != null) starSelectedRepo!!.name else defaultTitle
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomSpacing = if (isExpandedScreen) navBarPadding else (64.dp + 16.dp + navBarPadding)

    CompositionLocalProvider(
        LocalBottomSpacing provides bottomSpacing,
        LocalFabController provides fabController
    ) {
        SharedTransitionLayout {
            val toolsTooltipState = rememberTooltipState()
            val gamesTooltipState = rememberTooltipState()
            TooltipWrapper(state = toolsTooltipState) {
                TooltipWrapper(state = gamesTooltipState) { tooltipState ->
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    if (isExpandedScreen) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            PermanentNavigationDrawer(
                                drawerContent = {
                                    PermanentDrawerSheet(
                                        modifier = Modifier.width(300.dp),
                                        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp)
                                                .size(64.dp),
                                            Arrangement.SpaceBetween,
                                            Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    modifier = Modifier.size(20.dp),
                                                    painter = painterResource(R.drawable.ic_launcher_foreground_app),
                                                    contentDescription = "Logo"
                                                )
                                                Spacer(modifier = Modifier.size(4.dp))
                                                Text(
                                                    stringResource(R.string.app_name),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 24.sp,
                                                    fontFamily = FontFamily(Font(R.font.google_sans_bold))
                                                )
                                            }
                                            AnimatedVisibility(visible = !showSettingsSheet, enter = fadeIn(), exit = fadeOut()) {
                                                val animatedVisibilityScope = this
                                                FilledTonalIconButton(
                                                    onClick = { showSettingsSheet = true },
                                                    modifier = Modifier.sharedBounds(
                                                        sharedContentState = rememberSharedContentState(key = "settings_morph"),
                                                        animatedVisibilityScope = animatedVisibilityScope,
                                                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                                                    )
                                                ) {
                                                    Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings))
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(16.dp))
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            allValidMenuItems.forEach { item ->
                                                NavigationDrawerItem(
                                                    icon = { MenuItemIcon(icon = item.icon) },
                                                    label = {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(stringResource(item.titleResId))
                                                            if (item.id == "storage" || item.id == "ram") {
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                com.ost.application.util.BetaBadge()
                                                            }
                                                        }
                                                    },
                                                    selected = selectedScreenId == item.id,
                                                    onClick = { selectedScreenId = item.id },
                                                    modifier = Modifier.padding(horizontal = 12.dp)
                                                )
                                            }
                                            Spacer(Modifier
                                                .height(16.dp)
                                                .windowInsetsPadding(WindowInsets.navigationBars))
                                        }
                                    }
                                }
                            ) {
                                Scaffold(
                                    modifier = Modifier.fillMaxSize(),
                                    topBar = {
                                        TopAppBar(
                                            title = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        displayTitle,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (selectedScreenId == "storage" || selectedScreenId == "ram") {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        com.ost.application.util.BetaBadge()
                                                    }
                                                }
                                            },
                                            actions = {
                                                MainTopBarActions(
                                                    toolsTooltipState = toolsTooltipState,
                                                    gamesTooltipState = tooltipState
                                                )
                                            },
                                            scrollBehavior = scrollBehavior
                                        )
                                    },
                                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                                ) { paddingValues ->
                                    Box(
                                        modifier = Modifier
                                            .padding(paddingValues)
                                            .fillMaxSize(),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        MainContentTransition(
                                            selectedScreenId = selectedScreenId,
                                            stargazersViewModel = stargazersViewModel,
                                            fabController = fabController,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .widthIn(max = 840.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        BottomSheetScaffold(
                            scaffoldState = scaffoldState,
                            sheetPeekHeight = bottomSpacing,
                            sheetContainerColor = Color.Transparent,
                            sheetContentColor = MaterialTheme.colorScheme.onSurface,
                            sheetTonalElevation = 0.dp,
                            sheetShadowElevation = 0.dp,
                            sheetDragHandle = null,
                            sheetContent = {
                                val isSheetExpanded =
                                    scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded || scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                            .windowInsetsPadding(WindowInsets.navigationBars)
                                    ) {
                                        AppBottomNavigation(
                                            directItems = bottomNavDirectItems,
                                            selectedItemId = if (bottomNavDirectItems.any { it.id == selectedScreenId }) selectedScreenId else MORE_ITEM_ID,
                                            onItemClick = { itemId ->
                                                if (itemId == MORE_ITEM_ID) {
                                                    scope.launch { if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) scaffoldState.bottomSheetState.partialExpand() else scaffoldState.bottomSheetState.expand() }
                                                } else {
                                                    scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                                                    selectedScreenId = itemId
                                                }
                                            },
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                        this@Column.AnimatedVisibility(
                                            visible = fabController.isVisible && !isSheetExpanded,
                                            enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut(),
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .padding(end = 16.dp)
                                        ) {
                                            if (fabController.size == FabSize.Small) {
                                                SmallFloatingActionButton(
                                                    onClick = fabController.onClick,
                                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                                ) {
                                                    if (fabController.iconRes != null) Icon(
                                                        painter = painterResource(
                                                            id = fabController.iconRes!!
                                                        ),
                                                        contentDescription = fabController.contentDescription
                                                    )
                                                }
                                            } else {
                                                FloatingActionButton(
                                                    onClick = fabController.onClick,
                                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                                ) {
                                                    if (fabController.iconRes != null) Icon(
                                                        painter = painterResource(
                                                            id = fabController.iconRes!!
                                                        ),
                                                        contentDescription = fabController.contentDescription
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 200.dp),
                                        shape = RoundedCornerShape(40.dp, 40.dp, 0.dp, 0.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        tonalElevation = 2.dp
                                    ) {
                                        Column(Modifier.padding(0.dp, 16.dp, 0.dp, 0.dp)) {
                                            MoreBottomSheetContent(
                                                menuItems = moreMenuItems,
                                                currentSelectedScreenId = selectedScreenId,
                                                onItemClick = { itemId ->
                                                    scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                                                    selectedScreenId = itemId
                                                })
                                            Spacer(modifier = Modifier.height(24.dp))
                                        }
                                    }
                                }
                            }
                        ) { _ ->
                            val isSheetExpanded =
                                scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded || scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
                            val scrimAlpha by animateFloatAsState(
                                targetValue = if (isSheetExpanded) 0.32f else 0f,
                                label = "scrim",
                                animationSpec = tween(300)
                            )
                            Box(modifier = Modifier.fillMaxSize()) {
                                Scaffold(
                                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                                    contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
                                    topBar = {
                                        LargeFlexibleTopAppBar(
                                            title = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        displayTitle,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (selectedScreenId == "storage" || selectedScreenId == "ram") {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        com.ost.application.util.BetaBadge()
                                                    }
                                                }
                                            },
                                            expandedHeight = 152.dp,
                                            actions = {
                                                MainTopBarActions(
                                                    toolsTooltipState = toolsTooltipState,
                                                    gamesTooltipState = tooltipState
                                                )
                                                AnimatedVisibility(
                                                    visible = !showSettingsSheet,
                                                    enter = fadeIn(),
                                                    exit = fadeOut()
                                                ) {
                                                    val animatedVisibilityScope = this
                                                    FilledTonalIconButton(
                                                        onClick = { showSettingsSheet = true },
                                                        modifier = Modifier.sharedBounds(
                                                            sharedContentState = rememberSharedContentState(
                                                                key = "settings_morph"
                                                            ),
                                                            animatedVisibilityScope = animatedVisibilityScope,
                                                            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                                                        )
                                                    ) {
                                                        Icon(
                                                            Icons.Rounded.Settings,
                                                            contentDescription = stringResource(R.string.settings)
                                                        )
                                                    }
                                                }
                                            },
                                            scrollBehavior = scrollBehavior
                                        )
                                    },
                                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                                ) { scaffoldPadding ->
                                    MainContentTransition(
                                        selectedScreenId = selectedScreenId,
                                        stargazersViewModel = stargazersViewModel,
                                        fabController = fabController,
                                        modifier = Modifier
                                            .padding(scaffoldPadding)
                                            .fillMaxSize()
                                    )
                                }
                                if (scrimAlpha > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = scrimAlpha))
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                scope.launch {
                                                    scaffoldState.bottomSheetState.partialExpand()
                                                }
                                            }
                                    )
                                }
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = showSettingsSheet,
                        enter = fadeIn(tween(400)),
                        exit = fadeOut(tween(400)),
                        modifier = Modifier.zIndex(100f)
                    ) {
                        val animatedVisibilityScope = this
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { showSettingsSheet = false }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier
                                    .then(
                                        if (isExpandedScreen) Modifier
                                            .width(420.dp)
                                            .fillMaxHeight(0.85f)
                                        else Modifier
                                            .fillMaxSize()
                                    )
                                    .sharedBounds(
                                        sharedContentState = rememberSharedContentState(key = "settings_morph"),
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {}
                                    ),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                tonalElevation = 6.dp,
                                shadowElevation = 8.dp
                            ) {
                                LaunchedEffect(showSettingsSheet) {
                                    if (showSettingsSheet) {
                                        settingsViewModel.refreshDeveloperMode()
                                    }
                                }
                                SettingsSheetContent(
                                    modifier = Modifier.padding(
                                        top = WindowInsets.systemBars.asPaddingValues()
                                            .calculateTopPadding(),
                                        bottom = WindowInsets.systemBars.asPaddingValues()
                                            .calculateTopPadding(),
                                    ),
                                    state = settingsState,
                                    onTotalDurationChange = { floatValue ->
                                        settingsViewModel.updateTotalDuration(
                                            floatValue.roundToInt()
                                        )
                                    },
                                    onNoiseDurationChange = { floatValue ->
                                        settingsViewModel.updateNoiseDuration(
                                            floatValue.roundToInt()
                                        )
                                    },
                                    onBWNoiseDurationChange = { floatValue ->
                                        settingsViewModel.updateBlackWhiteNoiseDuration(
                                            floatValue.roundToInt()
                                        )
                                    },
                                    onHorizontalDurationChange = { floatValue ->
                                        settingsViewModel.updateHorizontalDuration(
                                            floatValue.roundToInt()
                                        )
                                    },
                                    onVerticalDurationChange = { floatValue ->
                                        settingsViewModel.updateVerticalDuration(
                                            floatValue.roundToInt()
                                        )
                                    },
                                    onGithubTokenChange = { token ->
                                        settingsViewModel.updateGithubToken(
                                            token
                                        )
                                    },
                                    onSaveGithubToken = {
                                        settingsViewModel.saveGithubToken(); stargazersViewModel.login(
                                        settingsState.githubToken
                                    )
                                    },
                                    onClearGithubToken = { settingsViewModel.clearGithubToken(); stargazersViewModel.logout() },
                                    onAboutClick = {
                                        showSettingsSheet = false; settingsViewModel.onAboutAppClicked()
                                    },
                                    onCloseClick = { showSettingsSheet = false },
                                    onLanguagePreferenceClick = { settingsViewModel.onLanguagePreferenceClick() },
                                    onLanguageSelected = { locale ->
                                        settingsViewModel.onLanguageSelectedInDialog(
                                            locale
                                        )
                                    },
                                    onLanguageConfirm = { settingsViewModel.onLanguageDialogConfirm() },
                                    onLanguageDismiss = { settingsViewModel.onLanguageDialogDismiss() },
                                    onTemperatureUnitChange = { unit -> settingsViewModel.updateTemperatureUnit(unit) },
                                    onDeveloperOptionsClick = { settingsViewModel.showDeveloperOptionsDialog() },
                                    onDismissDeveloperOptionsDialog = { settingsViewModel.dismissDeveloperOptionsDialog() },
                                    onShowLogcatClick = { settingsViewModel.showLogcatDialog() },
                                    onDismissLogcatDialog = { settingsViewModel.dismissLogcatDialog() }
                                )
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}
@Composable
private fun MainTopBarActions(
    toolsTooltipState: TooltipState,
    gamesTooltipState: TooltipState
) {
    val context = LocalContext.current
    val showToolsTooltip = remember { !AppPrefs.isToolsTooltipShown(context) }
    LaunchedEffect(toolsTooltipState.isVisible) {
        if (toolsTooltipState.isVisible) {
            AppPrefs.setToolsTooltipShown(context, true)
        }
    }
    IconButton(
        modifier = Modifier.tooltip(
            state = toolsTooltipState,
            title = "Tools is now here!",
            initialVisibility = showToolsTooltip
        ),
        onClick = {
            val intent = Intent(context, ConvertersActivity::class.java)
            context.startActivity(intent)
        }
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_build_24dp),
            contentDescription = stringResource(R.string.tools)
        )
    }
    IconButton(
        onClick = {
            val intent = Intent(context, com.ost.application.minigames.activity.MiniGamesMainActivity::class.java)
            context.startActivity(intent)
        }
    ) {
        Icon(
            Icons.Rounded.Games,
            contentDescription = "Mini Games"
        )
    }
}
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun MainContentTransition(
    selectedScreenId: String,
    stargazersViewModel: StargazersViewModel,
    fabController: FabController,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = selectedScreenId,
        label = "ScreenTransition",
        modifier = modifier,
        transitionSpec = {
            (fadeIn(tween(300)) + scaleIn(
                initialScale = 0.95f,
                animationSpec = tween(300)
            )).togetherWith(
                fadeOut(tween(300)) + scaleOut(
                    targetScale = 1.05f,
                    animationSpec = tween(300)
                )
            )
        }
    ) { targetId ->
        LaunchedEffect(targetId) { fabController.hideFab() }
        ContentArea(
            selectedItemId = targetId,
            stargazersViewModel = stargazersViewModel,
            modifier = Modifier.fillMaxSize()
        )
    }
}
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Preview(showBackground = true, name = "Main App Preview")
@Composable
fun DefaultPreview() {
    OSTToolsTheme {
        MainAppStructure()
    }
}
