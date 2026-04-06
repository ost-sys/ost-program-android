@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

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
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.ost.application.ui.component.ExpressiveShapeBackground
import com.ost.application.ui.component.ExpressiveShapeType
import com.ost.application.ui.screen.settings.SettingsUiState
import com.ost.application.ui.theme.OSTToolsTheme
import com.ost.application.util.AppPrefs
import com.ost.application.util.CardPosition
import com.ost.application.util.CustomCardItem
import com.ost.application.util.SectionTitle
import kotlin.math.roundToInt

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

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "permissions"

    var isEssentialGranted by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val targetProgress = when (currentRoute) {
        "permissions" -> 0.25f
        "timings" -> 0.50f
        "other" -> 0.75f
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
            title = { Text(stringResource(R.string.permissions)) },
            text = { Text("Please grant all essential permissions to proceed with the setup.") },
            confirmButton = {
                Button(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            if (currentRoute != "finish") {
                Column {
                    Spacer(Modifier.height(48.dp))
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
        },
        bottomBar = {
            if (currentRoute != "finish") {
                SetupBottomBar(
                    onNext = {
                        when (currentRoute) {
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
                    showBack = currentRoute != "permissions"
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "permissions",
            modifier = Modifier.padding(
                if (currentRoute == "finish") PaddingValues(0.dp) else paddingValues
            )
        ) {
            composable(
                "permissions",
                enterTransition = { slideInHorizontally { it } + fadeIn() },
                exitTransition = { slideOutHorizontally { -it } + fadeOut() },
                popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
                popExitTransition = { slideOutHorizontally { it } + fadeOut() }
            ) {
                PermissionsSetupScreen(onEssentialGrantedChange = { isEssentialGranted = it })
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
                    onBWNoiseDurationChange = { setupViewModel.updateBlackWhiteNoiseDuration(it.roundToInt()) },
                    onHorizontalDurationChange = { setupViewModel.updateHorizontalDuration(it.roundToInt()) },
                    onVerticalDurationChange = { setupViewModel.updateVerticalDuration(it.roundToInt()) },
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
                enterTransition = { scaleIn(initialScale = 0.9f) + fadeIn() }
            ) {
                FinishSetupScreen(onFinishAndNavigate = onFinishAndNavigate)
            }
        }
    }
}

@Composable
fun PermissionsSetupScreen(onEssentialGrantedChange: (Boolean) -> Unit) {
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

    fun checkPermissions() {
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

    val advancedList = remember(overlayGranted, installGranted, writeSettingsGranted) {
        listOf(
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
            ),
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
            ),
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
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                ScreenHeader(
                    title = stringResource(R.string.permissions),
                    description = stringResource(R.string.grant_permissions_to_unlock_full_potential),
                    shapeType = ExpressiveShapeType.SQUARE,
                    icon = R.drawable.ic_security_24dp
                )
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
    onVerticalDurationChange: (Float) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp, start = 16.dp, end = 16.dp, top = 16.dp)
    ) {
        item {
            ScreenHeader(
                title = stringResource(R.string.category_timings),
                description = stringResource(R.string.adjust_the_display_duration_for_different_patterns),
                shapeType = ExpressiveShapeType.CIRCLE,
                icon = R.drawable.ic_schedule_24dp
            )
        }

        item {
            SetupGroupCard {
                SeekBarPreference(
                    title = stringResource(R.string.total_recovery_time),
                    value = state.totalDuration,
                    range = 1f..120f,
                    steps = 58,
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
        contentPadding = PaddingValues(bottom = 100.dp)
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
fun FinishSetupScreen(onFinishAndNavigate: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1500, easing = EaseInOut), RepeatMode.Reverse),
        label = "scale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }) {
                    ExpressiveShapeBackground(
                        iconSize = 180.dp,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        forcedShape = ExpressiveShapeType.CLOVER_8
                    )
                }
                Icon(
                    painter = painterResource(R.drawable.ic_check_circle_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = stringResource(R.string.you_re_all_set),
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.the_application_is_ready_to_use),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(64.dp))

            LargeFloatingActionButton(
                onClick = onFinishAndNavigate,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.start),
                    modifier = Modifier.size(36.dp)
                )
            }
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
fun SetupBottomBar(onNext: () -> Unit, onBack: () -> Unit, showBack: Boolean) {
    val brush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = 0f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.surface
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBack) {
                OutlinedButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            } else {
                Spacer(Modifier.width(1.dp))
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
                    Icons.AutoMirrored.Filled.ArrowForward,
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
        modifier = Modifier.fillMaxWidth(),
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
                text = value.toString(),
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