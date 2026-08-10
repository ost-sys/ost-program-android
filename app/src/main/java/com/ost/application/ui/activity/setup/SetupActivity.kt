@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
package com.ost.application.ui.activity.setup
import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.sp
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ost.application.MainActivity
import com.ost.application.R
import com.ost.application.core.locale.LocaleHelper
import com.ost.application.ui.components.ExpressiveShapeBackground
import com.ost.application.ui.components.ExpressiveShapeType
import com.ost.application.ui.components.MeshGradientBackground
import com.ost.application.ui.screen.settings.SettingsUiState
import com.ost.application.ui.theme.OSTToolsTheme
import com.ost.application.util.AppPrefs
import com.ost.application.util.CardPosition
import com.ost.application.util.CustomCardItem
import com.ost.application.util.CustomRadioItem
import com.ost.application.ui.components.SectionTitle
import com.ost.application.core.settings.TemperatureUnit
import com.ost.application.ui.components.LanguagePickerDialog
import java.util.Locale
import com.ost.application.util.isRooted
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
class SetupViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SetupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SetupViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
data class PermissionItemData(
    val id: String,
    val title: String,
    val summary: String,
    val icon: Int,
    val isGranted: Boolean,
    val onClick: () -> Unit
)
class SetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OSTToolsTheme {
                SetupNavHost(onFinishAndNavigate = {
                    AppPrefs.setSetupComplete(this, true)
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                })
            }
        }
    }
}
@Composable
fun SetupNavHost(onFinishAndNavigate: () -> Unit) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val setupViewModel: SetupViewModel =
        viewModel(factory = SetupViewModelFactory(context.applicationContext as Application))
    val settingsState by setupViewModel.uiState.collectAsStateWithLifecycle()
    var appState by remember { mutableStateOf("welcome") }
    var isTransitioningToSetup by remember { mutableStateOf(false) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "regional"
    var isEssentialGranted by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    val isRootGranted by setupViewModel.isRootGranted.collectAsStateWithLifecycle()
    if (settingsState.isLanguageDialogVisible) {
        LanguagePickerDialog(
            selectedLocale = settingsState.selectedLanguageInDialog,
            supportedLocales = settingsState.supportedLocales,
            onLanguageSelected = { setupViewModel.onLanguageSelectedInDialog(it) },
            onConfirm = { setupViewModel.onLanguageDialogConfirm() },
            onDismiss = { setupViewModel.onLanguageDialogDismiss() }
        )
    }
    val targetProgress = when (currentRoute) {
        "regional" -> 0.20f
        "permissions" -> 0.40f
        "timings" -> 0.60f
        "other" -> 0.80f
        "finish" -> 1.0f
        else -> 0.0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 600),
        label = "progress"
    )
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            icon = {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    ExpressiveShapeBackground(
                        iconSize = 64.dp,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        forcedShape = ExpressiveShapeType.SQUARE
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_front_hand_24dp),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            },
            title = { Text(stringResource(R.string.permissions)) },
            text = { Text(stringResource(R.string.please_grant_all_essential_permissions_to_proceed_with_the_setup)) },
            confirmButton = {
                Button(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isLargeScreen = screenWidth > 600.dp
    @Composable
    fun MainContent(modifier: Modifier = Modifier) {
        val isFinish = currentRoute == "finish"
        Scaffold(
            modifier = modifier,
            containerColor = if (isFinish) Color.Transparent else MaterialTheme.colorScheme.surface
        ) { paddingValues ->
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "regional",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(
                        "regional",
                        enterTransition = { slideInHorizontally { it } + fadeIn() },
                        exitTransition = { slideOutHorizontally { -it } + fadeOut() },
                        popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
                        popExitTransition = { slideOutHorizontally { it } + fadeOut() }
                    ) {
                        RegionalSetupScreen(
                            state = settingsState,
                            onLanguageClick = { setupViewModel.onLanguagePreferenceClick() },
                            onTemperatureUnitChange = { setupViewModel.updateTemperatureUnit(it) }
                        )
                    }
                    composable(
                        "permissions",
                        enterTransition = { slideInHorizontally { it } + fadeIn() },
                        exitTransition = { slideOutHorizontally { -it } + fadeOut() },
                        popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
                        popExitTransition = { slideOutHorizontally { it } + fadeOut() }
                    ) {
                        PermissionsSetupScreen(
                            onEssentialGrantedChange = { isEssentialGranted = it },
                            isRootGranted = isRootGranted,
                            onRequestRoot = { setupViewModel.requestRootAccess() }
                        )
                    }
                    composable(
                        "timings",
                        enterTransition = { slideInHorizontally { it } + fadeIn() },
                        exitTransition = { slideOutHorizontally { -it } + fadeOut() },
                        popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
                        popExitTransition = { slideOutHorizontally { it } + fadeOut() }
                    ) {
                        TimingsSetupScreen(
                            state = settingsState,
                            onTotalDurationChange = { setupViewModel.updateTotalDuration(it.roundToInt()) },
                            onNoiseDurationChange = { setupViewModel.updateNoiseDuration(it.roundToInt()) },
                            onBWNoiseDurationChange = {
                                setupViewModel.updateBlackWhiteNoiseDuration(
                                    it.roundToInt()
                                )
                            },
                            onHorizontalDurationChange = {
                                setupViewModel.updateHorizontalDuration(
                                    it.roundToInt()
                                )
                            },
                            onVerticalDurationChange = { setupViewModel.updateVerticalDuration(it.roundToInt()) }
                        )
                    }
                    composable(
                        "other",
                        enterTransition = { slideInHorizontally { it } + fadeIn() },
                        exitTransition = { slideOutHorizontally { -it } + fadeOut() },
                        popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
                        popExitTransition = { slideOutHorizontally { it } + fadeOut() }
                    ) {
                        OthersSetupScreen(
                            state = settingsState,
                            onGithubTokenChange = { setupViewModel.updateGithubToken(it) },
                            onSaveGithubToken = { setupViewModel.saveAllSettings() }
                        )
                    }
                    composable(
                        "finish",
                        enterTransition = { if (isLargeScreen) EnterTransition.None else scaleIn(initialScale = 0.9f) + fadeIn() },
                        exitTransition = { ExitTransition.None }
                    ) {
                        FinishSetupScreen(
                            onFinishAndNavigate = onFinishAndNavigate,
                            isLargeScreen = isLargeScreen
                        )
                    }
                }
                if (currentRoute != "finish") {
                    val topBarBrush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                        )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(topBarBrush)
                            .then(if (isLargeScreen) Modifier else Modifier.statusBarsPadding())
                    ) {
                        Spacer(Modifier.height(16.dp))
                        LinearWavyProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
                if (currentRoute != "finish") {
                    SetupBottomBar(
                        onNext = {
                            when (currentRoute) {
                                "regional" -> {
                                    navController.navigate("permissions")
                                }
                                "permissions" -> {
                                    if (isEssentialGranted) {
                                        navController.navigate("timings")
                                    } else {
                                        showPermissionDialog = true
                                    }
                                }
                                "timings" -> {
                                    setupViewModel.saveAllSettings()
                                    navController.navigate("other")
                                }
                                "other" -> {
                                    setupViewModel.saveAllSettings()
                                    navController.navigate("finish")
                                }
                            }
                        },
                        onBack = { navController.popBackStack() },
                        showBack = currentRoute != "regional",
                        isLargeScreen = isLargeScreen,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
    @OptIn(ExperimentalSharedTransitionApi::class)
    Box(modifier = Modifier.fillMaxSize()) {
        MeshGradientBackground(
            modifier = Modifier.fillMaxSize(),
            animateEntrance = true
        )
        SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = appState,
                label = "app_state",
                transitionSpec = {
                    (fadeIn(tween(600)) + scaleIn(initialScale = 0.97f, animationSpec = tween(600, easing = EaseOutCubic))) togetherWith fadeOut(tween(400))
                }
            ) { state ->
                when (state) {
                    "welcome" -> {
                        WelcomeScreen(
                            animatedVisibilityScope = this@AnimatedContent,
                            isLargeScreen = isLargeScreen,
                            isTransitioning = isTransitioningToSetup,
                            onGetStartedClick = {
                                isTransitioningToSetup = true
                            },
                            onLanguageSelected = { locale -> LocaleHelper.setLocale(locale) }
                        )
                        LaunchedEffect(isTransitioningToSetup) {
                            if (isTransitioningToSetup) {
                                delay(600.milliseconds)
                                appState = "setup"
                            }
                        }
                    }
                    "setup" -> {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val isFinalScreen = currentRoute == "finish"
                            val configuration = LocalConfiguration.current
                            val view = LocalView.current
                            val density = LocalDensity.current
                            var screenCornerRadius by remember { mutableStateOf(0.dp) }
                            LaunchedEffect(view, density) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    var insets = view.rootWindowInsets
                                    var retries = 0
                                    while (insets == null && retries < 10) {
                                        kotlinx.coroutines.delay(50)
                                        insets = view.rootWindowInsets
                                        retries++
                                    }
                                    val radius = insets?.getRoundedCorner(android.view.RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0
                                    screenCornerRadius = with(density) { radius.toDp() }
                                }
                            }
                            val expandedWidth = if (isLargeScreen) 520.dp else maxWidth
                            val expandedHeight = if (isLargeScreen) (configuration.screenHeightDp.dp * 0.85f) else maxHeight
                            val expandedCorner = if (isLargeScreen) 28.dp else screenCornerRadius
                        val cardWidth by animateDpAsState(
                            targetValue = if (isFinalScreen) 64.dp else expandedWidth,
                            animationSpec = spring(dampingRatio = 0.7f, stiffness = 100f),
                            label = "cardWidth"
                        )
                        val cardHeight by animateDpAsState(
                            targetValue = if (isFinalScreen) 64.dp else expandedHeight,
                            animationSpec = spring(dampingRatio = 0.7f, stiffness = 100f),
                            label = "cardHeight"
                        )
                        val cardCorner by animateDpAsState(
                            targetValue = if (isFinalScreen) 100.dp else expandedCorner,
                            animationSpec = spring(dampingRatio = 0.7f, stiffness = 100f),
                            label = "cardCorner"
                        )
                        val cardColor by animateColorAsState(
                            targetValue = if (isFinalScreen) MaterialTheme.colorScheme.primary
                                          else MaterialTheme.colorScheme.surface,
                            animationSpec = tween(400),
                            label = "cardColor"
                        )
                        val borderAlpha by animateFloatAsState(
                            targetValue = if (isFinalScreen) 0f else if (isLargeScreen) 0.5f else 0f,
                            animationSpec = tween(300),
                            label = "borderAlpha"
                        )
                        val horizontalPad by animateDpAsState(if (isFinalScreen) 24.dp else 0.dp)
                        val verticalPad by animateDpAsState(if (isFinalScreen) 24.dp else 0.dp)
                        val topSpacerWeight by animateFloatAsState(
                            targetValue = if (isFinalScreen || isLargeScreen) 1f else 0.001f,
                            animationSpec = tween(400),
                            label = "topSpacerWeight"
                        )
                        val middleSpacerWeight by animateFloatAsState(
                            targetValue = if (isFinalScreen) 1f else 0.001f,
                            animationSpec = tween(400),
                            label = "middleSpacerWeight"
                        )
                        val bottomSpacerWeight by animateFloatAsState(
                            targetValue = if (!isFinalScreen && isLargeScreen) 1f else 0.001f,
                            animationSpec = tween(400),
                            label = "bottomSpacerWeight"
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (isFinalScreen || isLargeScreen) Modifier.systemBarsPadding() else Modifier)
                                .padding(horizontal = horizontalPad, vertical = verticalPad),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.weight(topSpacerWeight))
                            AnimatedVisibility(
                                visible = isFinalScreen,
                                enter = fadeIn(tween(500, delayMillis = 350)) +
                                        scaleIn(initialScale = 0.85f, animationSpec = tween(500, delayMillis = 350)),
                                exit = fadeOut(tween(150))
                            ) {
                                WaveAllSetText()
                            }
                            Spacer(modifier = Modifier.weight(middleSpacerWeight))
                            Card(
                                modifier = Modifier
                                    .width(cardWidth)
                                    .height(cardHeight)
                                    .sharedBounds(
                                        sharedContentState = rememberSharedContentState(key = "setup_container"),
                                        animatedVisibilityScope = this@AnimatedContent
                                    ),
                                shape = RoundedCornerShape(cardCorner.coerceAtLeast(0.dp)),
                                colors = CardDefaults.cardColors(containerColor = cardColor),
                                border = if (borderAlpha > 0.01f) BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderAlpha)
                                ) else null
                            ) {
                                MainContent(modifier = Modifier.fillMaxSize())
                            }
                            Spacer(modifier = Modifier.weight(bottomSpacerWeight))
                        }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun PermissionsSetupScreen(
    onEssentialGrantedChange: (Boolean) -> Unit,
    isRootGranted: Boolean,
    onRequestRoot: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var notifGranted by remember { mutableStateOf(false) }
    var bluetoothGranted by remember { mutableStateOf(false) }
    var phoneStateGranted by remember { mutableStateOf(false) }
    var mediaStateGranted by remember { mutableStateOf(false) }
    var overlayGranted by remember { mutableStateOf(false) }
    var installGranted by remember { mutableStateOf(false) }
    var storageGranted by remember { mutableStateOf(false) }
    var writeSettingsGranted by remember { mutableStateOf(false) }
    var accessibilityGranted by remember { mutableStateOf(false) }
    val isDeviceRooted = remember { isRooted() }
    fun checkPermissions() {
        accessibilityGranted = com.ost.application.core.service.OstAccessibilityService.isAccessibilityServiceEnabled(context)
        notifGranted = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        bluetoothGranted = if (Build.VERSION.SDK_INT >= 31) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mediaStateGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        }
        phoneStateGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        overlayGranted = Settings.canDrawOverlays(context)
        installGranted = context.packageManager.canRequestPackageInstalls()
        writeSettingsGranted = Settings.System.canWrite(context)
        storageGranted = if (Build.VERSION.SDK_INT >= 30) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        checkPermissions()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val multiplePermLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { checkPermissions() }
    val manageStorageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { checkPermissions() }
    val systemSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { checkPermissions() }
    val essentialList = remember(
        notifGranted,
        bluetoothGranted,
        phoneStateGranted,
        mediaStateGranted,
        storageGranted
    ) {
        val list = mutableListOf<PermissionItemData>()
        if (Build.VERSION.SDK_INT >= 33) {
            list.add(
                PermissionItemData(
                    id = "notif",
                    title = context.getString(R.string.notifications),
                    summary = context.getString(R.string.notif_perm_info),
                    icon = R.drawable.ic_notifications_24dp,
                    isGranted = notifGranted,
                    onClick = { multiplePermLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS)) }
                ))
        }
        if (Build.VERSION.SDK_INT >= 31) {
            list.add(
                PermissionItemData(
                    id = "bt",
                    title = context.getString(R.string.nearby_devices),
                    summary = context.getString(R.string.nd_perm_info),
                    icon = R.drawable.ic_wifi_24dp,
                    isGranted = bluetoothGranted,
                    onClick = {
                        multiplePermLauncher.launch(
                            arrayOf(
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                            )
                        )
                    }
                ))
        }
        list.add(
            PermissionItemData(
                id = "phone",
                title = context.getString(R.string.phone_state),
                summary = context.getString(R.string.ps_perm_info),
                icon = R.drawable.ic_phone_android_24dp,
                isGranted = phoneStateGranted,
                onClick = { multiplePermLauncher.launch(arrayOf(Manifest.permission.READ_PHONE_STATE)) }
            ))
        if (Build.VERSION.SDK_INT >= 33) {
            list.add(
                PermissionItemData(
                    id = "media",
                    title = context.getString(R.string.media_audio),
                    summary = context.getString(R.string.ma_perm_info),
                    icon = R.drawable.ic_music_note_24dp,
                    isGranted = mediaStateGranted,
                    onClick = { multiplePermLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_AUDIO)) }
                ))
        }
        list.add(
            PermissionItemData(
                id = "storage",
                title = context.getString(R.string.storage_access),
                summary =
                    if (Build.VERSION.SDK_INT >= 30)
                        context.getString(R.string.sdk30_sa_perm_info)
                    else
                        context.getString(R.string.sa_perm_info),
                icon = R.drawable.ic_folder_24dp,
                isGranted = storageGranted,
                onClick = {
                    if (Build.VERSION.SDK_INT >= 30) {
                        try {
                            val intent =
                                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                    data = "package:${context.packageName}".toUri()
                                }
                            manageStorageLauncher.launch(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            manageStorageLauncher.launch(intent)
                        }
                    } else {
                        multiplePermLauncher.launch(
                            arrayOf(
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            )
                        )
                    }
                }
            ))
        list
    }
    val allEssentialGranted = essentialList.all { it.isGranted }
    LaunchedEffect(allEssentialGranted) {
        onEssentialGrantedChange(allEssentialGranted)
    }
    val advancedList = remember(
        overlayGranted,
        installGranted,
        writeSettingsGranted,
        accessibilityGranted,
        isRootGranted,
        isDeviceRooted
    ) {
        val list = mutableListOf<PermissionItemData>()
        if (isDeviceRooted) {
            list.add(
                PermissionItemData(
                    id = "root",
                    title = context.getString(R.string.root_access),
                    summary = context.getString(R.string.ra_perm_info),
                    icon = R.drawable.ic_security_24dp,
                    isGranted = isRootGranted,
                    onClick = { onRequestRoot() }
                )
            )
        }
        list.add(
            PermissionItemData(
                id = "accessibility",
                title = context.getString(R.string.accessibility_service),
                summary = context.getString(R.string.accessibility_service_info),
                icon = com.ost.application.core.R.drawable.ic_accessibility_24dp,
                isGranted = accessibilityGranted,
                onClick = {
                    com.ost.application.core.service.OstAccessibilityService.openAccessibilitySettings(context)
                }
            )
        )
        list.add(
            PermissionItemData(
                id = "overlay",
                title = context.getString(R.string.display_over_other_apps),
                summary = context.getString(R.string.dooa_perm_info),
                icon = R.drawable.ic_layers_24dp,
                isGranted = overlayGranted,
                onClick = {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    systemSettingsLauncher.launch(intent)
                }
            )
        )
        list.add(
            PermissionItemData(
                id = "settings",
                title = context.getString(R.string.modify_system_settings),
                summary = context.getString(R.string.mys_perm_info),
                icon = R.drawable.ic_settings_24dp,
                isGranted = writeSettingsGranted,
                onClick = {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    systemSettingsLauncher.launch(intent)
                }
            )
        )
        list.add(
            PermissionItemData(
                id = "install",
                title = context.getString(R.string.install_unknown_apps),
                summary = context.getString(R.string.iua_perm_info),
                icon = R.drawable.ic_download_for_offline_24dp,
                isGranted = installGranted,
                onClick = {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    systemSettingsLauncher.launch(intent)
                }
            )
        )
        list
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp, top = 96.dp)
    ) {
        item {
            Column {
                Column(modifier = Modifier.padding(16.dp)) {
                    ScreenHeader(
                        title = stringResource(R.string.permissions),
                        description = stringResource(R.string.grant_permissions_to_unlock_full_potential),
                        shapeType = ExpressiveShapeType.SQUARE,
                        icon = R.drawable.ic_security_24dp
                    )
                }
            }
        }
        item { SectionTitle(title = stringResource(R.string.essential)) }
        itemsIndexed(essentialList) { index, item ->
            val position = when {
                essentialList.size == 1 -> CardPosition.SINGLE
                index == 0 -> CardPosition.TOP
                index == essentialList.lastIndex -> CardPosition.BOTTOM
                else -> CardPosition.MIDDLE
            }
            CustomCardItem(
                position = position,
                icon = item.icon,
                title = item.title,
                summary = item.summary,
                status = !item.isGranted,
                onClick = if (!item.isGranted) item.onClick else null
            )
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { SectionTitle(title = stringResource(R.string.advanced_permissions)) }
        itemsIndexed(advancedList) { index, item ->
            val position = when {
                advancedList.size == 1 -> CardPosition.SINGLE
                index == 0 -> CardPosition.TOP
                index == advancedList.lastIndex -> CardPosition.BOTTOM
                else -> CardPosition.MIDDLE
            }
            CustomCardItem(
                position = position,
                icon = item.icon,
                title = item.title,
                summary = item.summary,
                status = !item.isGranted,
                onClick = if (!item.isGranted) item.onClick else null
            )
        }
    }
}
@Composable
fun TimingsSetupScreen(
    state: SettingsUiState,
    onTotalDurationChange: (Float) -> Unit,
    onNoiseDurationChange: (Float) -> Unit,
    onBWNoiseDurationChange: (Float) -> Unit,
    onHorizontalDurationChange: (Float) -> Unit,
    onVerticalDurationChange: (Float) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp, top = 96.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                ScreenHeader(
                    title = stringResource(R.string.category_timings),
                    description = stringResource(R.string.adjust_the_display_duration_for_different_patterns),
                    shapeType = ExpressiveShapeType.CIRCLE,
                    icon = R.drawable.ic_schedule_24dp
                )
            }
        }
        item {
            SetupGroupCard {
                SeekBarPreference(
                    title = stringResource(R.string.total_recovery_time),
                    value = state.totalDuration,
                    range = 1f..121f,
                    steps = 59,
                    onValueChange = onTotalDurationChange
                )
                SeekBarPreference(
                    title = stringResource(R.string.noise),
                    value = state.noiseDuration,
                    range = 1f..10f,
                    steps = 8,
                    onValueChange = onNoiseDurationChange
                )
                SeekBarPreference(
                    title = stringResource(R.string.vertical_lines),
                    value = state.verticalDuration,
                    range = 1f..10f,
                    steps = 8,
                    onValueChange = onVerticalDurationChange
                )
                SeekBarPreference(
                    title = stringResource(R.string.black_white_noise),
                    value = state.blackWhiteNoiseDuration,
                    range = 1f..10f,
                    steps = 8,
                    onValueChange = onBWNoiseDurationChange
                )
                SeekBarPreference(
                    title = stringResource(R.string.horizontal_lines),
                    value = state.horizontalDuration,
                    range = 1f..10f,
                    steps = 8,
                    onValueChange = onHorizontalDurationChange
                )
            }
        }
    }
}
@Composable
fun OthersSetupScreen(
    state: SettingsUiState,
    onGithubTokenChange: (String) -> Unit,
    onSaveGithubToken: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp, top = 96.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                ScreenHeader(
                    title = stringResource(R.string.other),
                    description = stringResource(R.string.other_features_of_this_app),
                    shapeType = ExpressiveShapeType.COOKIE_4,
                    icon = R.drawable.ic_more_horiz_24dp
                )
            }
        }
        item {
            SectionTitle(title = stringResource(R.string.github_integration))
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    shape = RoundedCornerShape(
                        topStart = 24.dp, topEnd = 24.dp,
                        bottomStart = 4.dp, bottomEnd = 4.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = state.githubToken,
                            onValueChange = onGithubTokenChange,
                            label = { Text(stringResource(R.string.personal_access_token)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Button(
                    onClick = onSaveGithubToken,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(
                        topStart = 4.dp, topEnd = 4.dp,
                        bottomStart = 24.dp, bottomEnd = 24.dp
                    )
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}
@Composable
fun FinishSetupScreen(onFinishAndNavigate: () -> Unit, isLargeScreen: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onFinishAndNavigate() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = stringResource(R.string.start),
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}
@Composable
fun WaveAllSetText() {
    val fullText = stringResource(R.string.you_re_all_set)
    val chars = remember(fullText) { fullText.toList() }
    val haptic = LocalHapticFeedback.current
    var wavePeak by remember { mutableStateOf(-1) }
    var waveComplete by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(350L)
        for (i in chars.indices) {
            wavePeak = i
            delay(65L)
        }
        wavePeak = chars.size
        delay(120L)
        waveComplete = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        chars.forEachIndexed { index, char ->
            val dist = wavePeak - index
            val targetWeight: Float = when {
                waveComplete -> 700f
                wavePeak < 0 -> 300f
                dist == 0    -> 900f
                dist == 1    -> 620f
                dist == 2    -> 420f
                else         -> 300f
            }
            val animSpec: AnimationSpec<Float> = if (waveComplete) {
                spring(dampingRatio = 0.45f, stiffness = 200f)
            } else {
                tween(durationMillis = 90, easing = EaseOutCubic)
            }
            val animWeight by animateFloatAsState(
                targetValue = targetWeight,
                animationSpec = animSpec,
                label = "charWeight_$index"
            )
            val weightBucket = (animWeight / 25f).toInt()
            val fontFamily = remember(weightBucket) {
                FontFamily(
                    Font(
                        resId = R.font.google_sans_flex,
                        variationSettings = FontVariation.Settings(
                            FontVariation.weight((weightBucket * 25).coerceIn(100, 900)),
                            FontVariation.width(100f)
                        )
                    )
                )
            }
            Text(
                text = char.toString(),
                fontFamily = fontFamily,
                fontSize = 46.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
@Composable
fun ScreenHeader(title: String, description: String, shapeType: ExpressiveShapeType, icon: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            ExpressiveShapeBackground(
                iconSize = 64.dp,
                color = MaterialTheme.colorScheme.secondaryContainer,
                forcedShape = shapeType
            )
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
@Composable
fun SetupBottomBar(
    onNext: () -> Unit,
    onBack: () -> Unit,
    showBack: Boolean,
    isLargeScreen: Boolean,
    modifier: Modifier = Modifier
) {
    val brush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = 0f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.surface
        )
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(brush)
            .then(
                if (isLargeScreen) Modifier else Modifier.navigationBarsPadding()
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                val showAnim = showBack
                androidx.compose.animation.AnimatedVisibility(
                    visible = showAnim,
                    enter = fadeIn(tween(200)) + scaleIn(
                        initialScale = 0.6f,
                        animationSpec = tween(250, easing = EaseOutCubic)
                    ),
                    exit = fadeOut(tween(150)) + scaleOut(
                        targetScale = 0.6f,
                        animationSpec = tween(150)
                    )
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Rounded.ArrowBackIosNew,
                            stringResource(R.string.back)
                        )
                    }
                }
            }
            Button(
                onClick = onNext,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.next),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
@Composable
fun SetupGroupCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) { content() }
    }
}
@Composable
fun SeekBarPreference(
    title: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (value > 120) "∞" else "$value ${stringResource(R.string.min)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = {
                if (it.roundToInt() != value) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onValueChange(it)
            },
            valueRange = range, steps = steps
        )
    }
}
@Composable
fun RegionalSetupScreen(
    state: SettingsUiState,
    onLanguageClick: () -> Unit,
    onTemperatureUnitChange: (TemperatureUnit) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp, top = 96.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                ScreenHeader(
                    title = stringResource(R.string.regional_settings),
                    description = "Choose your preferred language and temperature unit",
                    shapeType = ExpressiveShapeType.COOKIE_9,
                    icon = R.drawable.ic_public_24dp
                )
            }
        }
        item { SectionTitle(title = stringResource(R.string.language)) }
        item {
            CustomCardItem(
                title = stringResource(R.string.language),
                summary = state.currentAppliedLocale.getDisplayName(state.currentAppliedLocale)
                    .replaceFirstChar { it.titlecase(state.currentAppliedLocale) },
                icon = R.drawable.ic_public_24dp,
                position = CardPosition.SINGLE,
                onClick = onLanguageClick
            )
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { SectionTitle(title = "Temperature unit") }
        item {
            val options = listOf(
                TemperatureUnit.SYSTEM to "System default",
                TemperatureUnit.CELSIUS to "Celsius",
                TemperatureUnit.FAHRENHEIT to "Fahrenheit"
            )
            Column {
                options.forEachIndexed { index, (unit, label) ->
                    val isSelected = state.temperatureUnit == unit
                    val position = when {
                        options.size == 1 -> CardPosition.SINGLE
                        index == 0 -> CardPosition.TOP
                        index == options.lastIndex -> CardPosition.BOTTOM
                        else -> CardPosition.MIDDLE
                    }
                    CustomRadioItem(
                        title = label,
                        selected = isSelected,
                        position = position,
                        onClick = { onTemperatureUnitChange(unit) }
                    )
                }
            }
        }
    }
}