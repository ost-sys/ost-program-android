@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    FlowPreview::class, ExperimentalSharedTransitionApi::class
)

package com.ost.application

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.ui.screen.batteryinfo.BatteryInfoScreen
import com.ost.application.ui.screen.converters.ConvertersScreen
import com.ost.application.ui.screen.deviceinfo.DeviceInfoScreen
import com.ost.application.ui.screen.display.DisplayInfoScreen
import com.ost.application.ui.screen.network.NetworkInfoScreen
import com.ost.application.ui.screen.powermenu.PowerMenuScreen
import com.ost.application.ui.screen.settings.SettingsAction
import com.ost.application.ui.screen.settings.SettingsScreen
import com.ost.application.ui.screen.settings.SettingsUiState
import com.ost.application.ui.screen.settings.SettingsViewModel
import com.ost.application.ui.screen.share.ShareScreen
import com.ost.application.ui.screen.stargazers.StargazersScreen
import com.ost.application.ui.screen.stargazers.StargazersViewModel
import com.ost.application.ui.state.FabController
import com.ost.application.ui.state.FabSize
import com.ost.application.ui.state.LocalFabController
import com.ost.application.ui.theme.OSTToolsTheme
import com.ost.application.util.CardPosition
import com.ost.application.util.CustomCardItem
import com.ost.application.util.toast
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

val LocalBottomSpacing = staticCompositionLocalOf { 0.dp }

data class MenuItemData(
    val id: String,
    @StringRes val titleResId: Int,
    @DrawableRes val iconResId: Int
)

private fun createMenuItems(): List<MenuItemData?> {
    return listOf(
        MenuItemData("tools", R.string.tools, R.drawable.ic_build_24dp),
        MenuItemData("power_menu", R.string.power_menu, R.drawable.ic_power_new_24dp),
        MenuItemData("share_files", R.string.share, R.drawable.ic_share_24dp),
        MenuItemData("stargazers", R.string.stargazers, R.drawable.ic_star_24dp),
        null,
        MenuItemData("about_device", R.string.about_device, R.drawable.ic_device_24dp),
        MenuItemData("battery", R.string.battery, R.drawable.ic_battery_full_24dp),
        MenuItemData("display", R.string.display, R.drawable.ic_display_24dp),
        MenuItemData("network", R.string.network, R.drawable.ic_wifi_24dp),
    )
}

private const val MORE_ITEM_ID = "more_button_id"

class MainActivity : AppCompatActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        installSplashScreen()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val isExpandedScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

            OSTToolsTheme {
                MainAppStructure(isExpandedScreen = isExpandedScreen)
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
@SuppressLint("AutoboxingStateCreation")
fun MainAppStructure(isExpandedScreen: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val fabController = remember { FabController() }

    val allMenuItems = remember { createMenuItems() }
    val allValidMenuItems = remember(allMenuItems) { allMenuItems.filterNotNull() }
    val bottomNavDirectItems = remember { allValidMenuItems.take(3) }
    val moreMenuItems = remember { allMenuItems.drop(bottomNavDirectItems.size) }
    var selectedScreenId by rememberSaveable { mutableStateOf(bottomNavDirectItems.first().id) }

    val scaffoldState = rememberBottomSheetScaffoldState()

    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context.applicationContext as Application))
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val stargazersViewModel: StargazersViewModel = viewModel()
    val starSelectedRepo by stargazersViewModel.selectedRepo.collectAsStateWithLifecycle()

    var showSettingsSheet by remember { mutableStateOf(false) }

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
                            Log.w("MainAppStructure", "Prevented launching MainActivity: ${action.intent}")
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
    val defaultTitle = currentSelectedItemData?.let { stringResource(it.titleResId) } ?: stringResource(id = R.string.app_name)
    val displayTitle = if (selectedScreenId == "stargazers" && starSelectedRepo != null) starSelectedRepo!!.name else defaultTitle
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomSpacing = if (isExpandedScreen) navBarPadding else (88.dp + 16.dp + navBarPadding)

    CompositionLocalProvider(
        LocalBottomSpacing provides bottomSpacing,
        LocalFabController provides fabController
    ) {
        SharedTransitionLayout {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

                if (isExpandedScreen) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        PermanentNavigationDrawer(
                            drawerContent = {
                                PermanentDrawerSheet(
                                    modifier = Modifier.width(280.dp),
                                    drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                ) {
                                    Spacer(Modifier.height(32.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.End
                                    ) {
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
                                                Icon(painterResource(R.drawable.ic_settings_24dp), contentDescription = stringResource(R.string.settings))
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    allValidMenuItems.forEach { item ->
                                        NavigationDrawerItem(
                                            selected = selectedScreenId == item.id,
                                            onClick = { selectedScreenId = item.id },
                                            icon = { Icon(painterResource(item.iconResId), contentDescription = stringResource(item.titleResId)) },
                                            label = { Text(stringResource(item.titleResId)) },
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            colors = NavigationDrawerItemDefaults.colors(
                                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        )
                                    }
                                }
                            }
                        ) {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                topBar = {
                                    LargeFlexibleTopAppBar(
                                        title = { Text(displayTitle, fontWeight = FontWeight.Bold) },
                                        expandedHeight = 152.dp,
                                        scrollBehavior = scrollBehavior
                                    )
                                },
                                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                            ) { paddingValues ->
                                AnimatedContent(
                                    targetState = selectedScreenId, label = "ScreenTransition",
                                    modifier = Modifier.padding(paddingValues).fillMaxSize(),
                                    transitionSpec = { (fadeIn(tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300))).togetherWith(fadeOut(tween(300)) + scaleOut(targetScale = 1.05f, animationSpec = tween(300))) }
                                ) { targetId ->
                                    LaunchedEffect(targetId) { fabController.hideFab() }
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                        ContentArea(selectedItemId = targetId, stargazersViewModel = stargazersViewModel, modifier = Modifier.fillMaxSize().widthIn(max = 840.dp))
                                    }
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
                            val isSheetExpanded = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded || scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).windowInsetsPadding(WindowInsets.navigationBars)) {
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
                                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
                                    ) {
                                        if (fabController.size == FabSize.Small) {
                                            SmallFloatingActionButton(onClick = fabController.onClick, containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer) {
                                                if (fabController.iconRes != null) Icon(painter = painterResource(id = fabController.iconRes!!), contentDescription = fabController.contentDescription)
                                            }
                                        } else {
                                            FloatingActionButton(onClick = fabController.onClick, containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer) {
                                                if (fabController.iconRes != null) Icon(painter = painterResource(id = fabController.iconRes!!), contentDescription = fabController.contentDescription)
                                            }
                                        }
                                    }
                                }
                                Surface(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp), shape = RoundedCornerShape(40.dp, 40.dp, 0.dp, 0.dp), color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 2.dp) {
                                    Column {
                                        MoreBottomSheetContent(menuItems = moreMenuItems, currentSelectedScreenId = selectedScreenId, onItemClick = { itemId ->
                                            scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                                            selectedScreenId = itemId
                                        })
                                        Spacer(modifier = Modifier.height(24.dp))
                                    }
                                }
                            }
                        }
                    ) { _ ->
                        val isSheetExpanded = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded || scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded
                        val scrimAlpha by animateFloatAsState(targetValue = if (isSheetExpanded) 0.32f else 0f, label = "scrim", animationSpec = tween(300))

                        Box(modifier = Modifier.fillMaxSize()) {
                            Scaffold(
                                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                                contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
                                topBar = {
                                    LargeFlexibleTopAppBar(
                                        title = { Text(displayTitle, fontWeight = FontWeight.Bold) },
                                        expandedHeight = 152.dp,
                                        actions = {
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
                                                    Icon(painterResource(R.drawable.ic_settings_24dp), contentDescription = stringResource(R.string.settings))
                                                }
                                            }
                                        },
                                        scrollBehavior = scrollBehavior
                                    )
                                },
                                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                            ) { scaffoldPadding ->
                                AnimatedContent(
                                    targetState = selectedScreenId, label = "ScreenTransition",
                                    modifier = Modifier.padding(scaffoldPadding).fillMaxSize(),
                                    transitionSpec = { (fadeIn(tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300))).togetherWith(fadeOut(tween(300)) + scaleOut(targetScale = 1.05f, animationSpec = tween(300))) }
                                ) { targetId ->
                                    LaunchedEffect(targetId) { fabController.hideFab() }
                                    ContentArea(selectedItemId = targetId, stargazersViewModel = stargazersViewModel, modifier = Modifier.fillMaxSize())
                                }
                            }

                            if (scrimAlpha > 0f) {
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = scrimAlpha)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { scope.launch { scaffoldState.bottomSheetState.partialExpand() } })
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
                                    if (isExpandedScreen) Modifier.width(420.dp).fillMaxHeight(0.85f)
                                    else Modifier.fillMaxSize().padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 8.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
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
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 6.dp,
                            shadowElevation = 8.dp
                        ) {
                            SettingsSheetContent(
                                state = settingsState,
                                onTotalDurationChange = { floatValue -> settingsViewModel.updateTotalDuration(floatValue.roundToInt()) },
                                onNoiseDurationChange = { floatValue -> settingsViewModel.updateNoiseDuration(floatValue.roundToInt()) },
                                onBWNoiseDurationChange = { floatValue -> settingsViewModel.updateBlackWhiteNoiseDuration(floatValue.roundToInt()) },
                                onHorizontalDurationChange = { floatValue -> settingsViewModel.updateHorizontalDuration(floatValue.roundToInt()) },
                                onVerticalDurationChange = { floatValue -> settingsViewModel.updateVerticalDuration(floatValue.roundToInt()) },
                                onGithubTokenChange = { token -> settingsViewModel.updateGithubToken(token) },
                                onSaveGithubToken = { settingsViewModel.saveGithubToken(); stargazersViewModel.login(settingsState.githubToken) },
                                onClearGithubToken = { settingsViewModel.clearGithubToken(); stargazersViewModel.logout() },
                                onAboutClick = { showSettingsSheet = false; settingsViewModel.onAboutAppClicked() },
                                onCloseClick = { showSettingsSheet = false },
                                onLanguagePreferenceClick = { settingsViewModel.onLanguagePreferenceClick() },
                                onLanguageSelected = { locale -> settingsViewModel.onLanguageSelectedInDialog(locale) },
                                onLanguageConfirm = { settingsViewModel.onLanguageDialogConfirm() },
                                onLanguageDismiss = { settingsViewModel.onLanguageDialogDismiss() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSheetContent(
    state: SettingsUiState,
    onTotalDurationChange: (Float) -> Unit,
    onNoiseDurationChange: (Float) -> Unit,
    onBWNoiseDurationChange: (Float) -> Unit,
    onHorizontalDurationChange: (Float) -> Unit,
    onVerticalDurationChange: (Float) -> Unit,
    onGithubTokenChange: (String) -> Unit,
    onSaveGithubToken: () -> Unit,
    onClearGithubToken: () -> Unit,
    onAboutClick: () -> Unit,
    onCloseClick: () -> Unit,
    onLanguagePreferenceClick: () -> Unit,
    onLanguageSelected: (Locale?) -> Unit,
    onLanguageConfirm: () -> Unit,
    onLanguageDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Row {
                IconButton(onClick = onAboutClick) { Icon(painterResource(R.drawable.ic_info_24dp), contentDescription = stringResource(R.string.about_app)) }
                IconButton(onClick = onCloseClick) { Icon(painterResource(R.drawable.ic_close_24dp), contentDescription = "Close") }
            }
        }
        SettingsScreen(
            state = state,
            onTotalDurationChange = onTotalDurationChange,
            onNoiseDurationChange = onNoiseDurationChange,
            onBWNoiseDurationChange = onBWNoiseDurationChange,
            onHorizontalDurationChange = onHorizontalDurationChange,
            onVerticalDurationChange = onVerticalDurationChange,
            onGithubTokenChange = onGithubTokenChange,
            onSaveGithubToken = onSaveGithubToken,
            onClearGithubToken = onClearGithubToken,
            onLanguagePreferenceClick = onLanguagePreferenceClick,
            onLanguageSelected = onLanguageSelected,
            onLanguageConfirm = onLanguageConfirm,
            onLanguageDismiss = onLanguageDismiss,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppBottomNavigation(
    directItems: List<MenuItemData>,
    selectedItemId: String,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    HorizontalFloatingToolbar(
        modifier = modifier.shadow(5.dp, shape = MaterialTheme.shapes.extraLarge),
        expanded = true,
    ) {
        directItems.forEach { item ->
            val isSelected = item.id == selectedItemId
            Surface(
                onClick = { onItemClick(item.id) },
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = item.iconResId),
                        contentDescription = stringResource(item.titleResId),
                        modifier = Modifier.size(24.dp)
                    )
                    AnimatedVisibility(
                        visible = isSelected,
                        enter = expandHorizontally() + fadeIn(),
                        exit = shrinkHorizontally() + fadeOut()
                    ) {
                        Row {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(item.titleResId),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        val isMoreSelected = selectedItemId == MORE_ITEM_ID
        IconButton(
            onClick = { onItemClick(MORE_ITEM_ID) },
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (isMoreSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                contentColor = if (isMoreSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_more_horiz_24dp),
                contentDescription = stringResource(R.string.more),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreBottomSheetContent(menuItems: List<MenuItemData?>, currentSelectedScreenId: String?, onItemClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        menuItems.forEachIndexed { index, item ->
            val position = when {
                menuItems.size == 1 -> CardPosition.SINGLE
                index == 0 -> CardPosition.TOP
                index == menuItems.lastIndex -> CardPosition.BOTTOM
                else -> CardPosition.MIDDLE
            }
            if (item != null) {
                val isSelected = item.id == currentSelectedScreenId
                CustomCardItem(
                    position = position,
                    title = stringResource(item.titleResId),
                    icon = item.iconResId,
                    colors = if (isSelected)
                        CardDefaults.cardColors( containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                    else
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    onClick = { onItemClick(item.id) }
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun ContentArea(
    selectedItemId: String?,
    stargazersViewModel: StargazersViewModel,
    modifier: Modifier = Modifier
) {
    when (selectedItemId) {
        "tools" -> ConvertersScreen(modifier = modifier)
        "power_menu" -> PowerMenuScreen(modifier = modifier)
        "stargazers" -> StargazersScreen(viewModel = stargazersViewModel)
        "about_device" -> DeviceInfoScreen(modifier = modifier)
        "battery" -> BatteryInfoScreen(modifier = modifier)
        "display" -> DisplayInfoScreen(modifier = modifier)
        "network" -> NetworkInfoScreen(modifier = modifier)
        "share_files" -> ShareScreen(modifier = modifier)
        else -> {
            Column(modifier = modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Select an option from the bottom bar.")
            }
        }
    }
}

@Composable
fun CordItem(label: String, value: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = CircleShape, modifier = Modifier.padding(bottom = 8.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        }
        Text(text = value.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
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