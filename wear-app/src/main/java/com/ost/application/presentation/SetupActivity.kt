package com.ost.application.presentation.setup
import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.PagerScaffoldDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Slider
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import com.ost.application.R
import com.ost.application.component.ExpressiveShapeBackground
import com.ost.application.component.ExpressiveShapeType
import com.ost.application.core.settings.TimingSettings
import com.ost.application.core.settings.sync.SettingsSyncClient
import com.ost.application.core.settings.sync.WearSyncState
import com.ost.application.settings.WearTemperatureUnitRepository
import com.ost.application.settings.WearTimingSettingsRepository
import com.ost.application.theme.OSTToolsTheme
import com.ost.application.util.CardListItem
import com.ost.application.util.CardPosition
import androidx.compose.foundation.selection.selectableGroup
import androidx.wear.compose.material3.RadioButton
import com.ost.application.core.settings.TemperatureUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
data class WearSetupUiState(
    val timing: TimingSettings = TimingSettings.DEFAULT,
    val syncState: WearSyncState = WearSyncState.Unavailable,
    val githubTokenFound: Boolean = false,
    val phoneConnected: Boolean = false,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.DEFAULT
)
class WearSetupViewModel(application: Application) : AndroidViewModel(application) {
    private val timingRepository = WearTimingSettingsRepository(application, viewModelScope)
    private val temperatureRepository =
        WearTemperatureUnitRepository(application, viewModelScope, timingRepository.syncState)
    private val syncClient = SettingsSyncClient(application)
    private val _githubTokenFound = MutableStateFlow(false)
    private val _phoneConnected = MutableStateFlow(false)
    val uiState: StateFlow<WearSetupUiState> = combine(
        timingRepository.settings,
        timingRepository.syncState,
        _githubTokenFound,
        _phoneConnected,
        temperatureRepository.unit
    ) { timing, syncState, tokenFound, phoneConnected, tempUnit ->
        WearSetupUiState(timing, syncState, tokenFound, phoneConnected, tempUnit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WearSetupUiState())
    init {
        viewModelScope.launch {
            _phoneConnected.value = syncClient.isCounterpartNodeConnected()
            if (_phoneConnected.value) {
                _githubTokenFound.value = syncClient.getLastSyncedGithubTokenPresence()
            }
        }
    }
    fun onSyncToggle(enabled: Boolean) = timingRepository.setSyncEnabled(enabled)
    fun onTotalDurationChange(v: Int) = timingRepository.updateTotalDuration(v)
    fun onNoiseDurationChange(v: Int) = timingRepository.updateNoiseDuration(v)
    fun onBWNoiseDurationChange(v: Int) = timingRepository.updateBlackWhiteNoiseDuration(v)
    fun onHorizontalDurationChange(v: Int) = timingRepository.updateHorizontalDuration(v)
    fun onVerticalDurationChange(v: Int) = timingRepository.updateVerticalDuration(v)
    fun onTemperatureUnitChange(unit: TemperatureUnit) = temperatureRepository.updateUnit(unit)
    fun requestOpenSettingsOnPhone() {
        viewModelScope.launch { syncClient.requestOpenSettingsOnPhone() }
    }
}
class WearSetupViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WearSetupViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WearSetupViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
class SetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSTToolsTheme {
                SetupPagerHost(onFinishAndNavigate = {
                    getSharedPreferences("wear_app_prefs", MODE_PRIVATE)
                        .edit()
                        .putBoolean("setup_complete", true)
                        .apply()
                    startActivity(Intent(this, com.ost.application.MainActivity::class.java))
                    finish()
                })
            }
        }
    }
}
private enum class SetupPage { REGIONAL, PERMISSIONS, TIMINGS, OTHER }
@Composable
fun SetupPagerHost(onFinishAndNavigate: () -> Unit) {
    val context = LocalContext.current
    val viewModel: WearSetupViewModel = viewModel(
        factory = WearSetupViewModelFactory(context.applicationContext as Application)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFinish by remember { mutableStateOf(false) }
    var isEssentialGranted by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    if (showFinish) {
        FinishSetupScreen(onFinishAndNavigate = onFinishAndNavigate)
        return
    }
    if (showPermissionDialog) {
        AlertDialog(
            visible = true,
            onDismissRequest = { showPermissionDialog = false },
            title = { Text(stringResource(R.string.permissions)) },
            text = { Text(stringResource(R.string.please_grant_all_essential_permissions_to_proceed_with_the_setup)) },
            confirmButton = {
                Button(onClick = { showPermissionDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }
    val pages = SetupPage.entries
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val regionalListState = rememberScalingLazyListState()
    val permissionsListState = rememberScalingLazyListState()
    val timingsListState = rememberScalingLazyListState()
    val otherListState = rememberScalingLazyListState()
    AppScaffold {
        HorizontalPagerScaffold(pagerState = pagerState) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = if (pages[pagerState.currentPage] == SetupPage.PERMISSIONS) isEssentialGranted else true,
                flingBehavior = PagerScaffoldDefaults.snapWithSpringFlingBehavior(state = pagerState),
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val currentListState = when (pages[pageIndex]) {
                    SetupPage.REGIONAL -> regionalListState
                    SetupPage.PERMISSIONS -> permissionsListState
                    SetupPage.TIMINGS -> timingsListState
                    SetupPage.OTHER -> otherListState
                }
                ScreenScaffold(
                    scrollState = currentListState,
                    edgeButton = {
                        EdgeButton(
                            onClick = {
                                if (pages[pageIndex] == SetupPage.PERMISSIONS && !isEssentialGranted) {
                                    showPermissionDialog = true
                                } else if (pageIndex == pages.lastIndex) {
                                    showFinish = true
                                } else {
                                    scope.launch { pagerState.animateScrollToPage(pageIndex + 1) }
                                }
                            },
                            buttonSize = EdgeButtonSize.Large
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_forward_24dp),
                                contentDescription = "Next"
                            )
                        }
                    }
                ) {
                    when (pages[pageIndex]) {
                        SetupPage.REGIONAL -> RegionalSetupScreen(
                            listState = currentListState,
                            state = uiState,
                            onTemperatureUnitChange = viewModel::onTemperatureUnitChange
                        )
                        SetupPage.PERMISSIONS -> PermissionsSetupScreen(
                            listState = currentListState,
                            onEssentialGrantedChange = { isEssentialGranted = it }
                        )
                        SetupPage.TIMINGS -> TimingsSetupScreen(
                            listState = currentListState,
                            state = uiState,
                            onSyncToggle = viewModel::onSyncToggle,
                            onTotalDurationChange = viewModel::onTotalDurationChange,
                            onNoiseDurationChange = viewModel::onNoiseDurationChange,
                            onBWNoiseDurationChange = viewModel::onBWNoiseDurationChange,
                            onHorizontalDurationChange = viewModel::onHorizontalDurationChange,
                            onVerticalDurationChange = viewModel::onVerticalDurationChange
                        )
                        SetupPage.OTHER -> OtherSetupScreen(
                            listState = currentListState,
                            state = uiState,
                            onOpenSettingsOnPhone = viewModel::requestOpenSettingsOnPhone
                        )
                    }
                }
            }
        }
    }
}
private data class WearPermissionItemData(
    val id: String,
    val title: String,
    val summary: String?,
    val icon: Int,
    val isGranted: Boolean,
    val requiresPhoneInstructions: Boolean,
    val onClick: () -> Unit
)
@Composable
private fun PermissionsSetupScreen(
    listState: androidx.wear.compose.foundation.lazy.ScalingLazyListState,
    onEssentialGrantedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showPhoneInstructionsDialog by remember { mutableStateOf(false) }
    var notifGranted by remember { mutableStateOf(false) }
    var bluetoothGranted by remember { mutableStateOf(false) }
    var mediaAudioGranted by remember { mutableStateOf(false) }
    var accessibilityGranted by remember { mutableStateOf(false) }
    fun checkPermissions() {
        accessibilityGranted = com.ost.application.core.service.OstAccessibilityService.isAccessibilityServiceEnabled(context)
        notifGranted = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
        bluetoothGranted = if (Build.VERSION.SDK_INT >= 31) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else true
        mediaAudioGranted = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) checkPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        checkPermissions()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val multiplePermLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { checkPermissions() }
    val requestable = remember(notifGranted, bluetoothGranted, mediaAudioGranted, accessibilityGranted) {
        buildList {
            add(
                WearPermissionItemData(
                    id = "accessibility",
                    title = context.getString(R.string.accessibility_service),
                    summary = null,
                    icon = com.ost.application.core.R.drawable.ic_accessibility_24dp,
                    isGranted = accessibilityGranted,
                    requiresPhoneInstructions = false,
                    onClick = { com.ost.application.core.service.OstAccessibilityService.openAccessibilitySettings(context) }
                )
            )
            if (Build.VERSION.SDK_INT >= 33) {
                add(
                    WearPermissionItemData(
                        id = "notif",
                        title = context.getString(R.string.notifications),
                        summary = null,
                        icon = R.drawable.ic_notifications_24dp,
                        isGranted = notifGranted,
                        requiresPhoneInstructions = false,
                        onClick = { multiplePermLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS)) }
                    )
                )
            }
            if (Build.VERSION.SDK_INT >= 31) {
                add(
                    WearPermissionItemData(
                        id = "bluetooth",
                        title = context.getString(R.string.nearby_devices),
                        summary = null,
                        icon = R.drawable.ic_travel_explore_24dp,
                        isGranted = bluetoothGranted,
                        requiresPhoneInstructions = false,
                        onClick = {
                            multiplePermLauncher.launch(
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                    Manifest.permission.BLUETOOTH_SCAN
                                )
                            )
                        }
                    )
                )
            }
            add(
                WearPermissionItemData(
                    id = "media",
                    title = context.getString(R.string.media_audio),
                    summary = null,
                    icon = R.drawable.ic_music_24dp,
                    isGranted = mediaAudioGranted,
                    requiresPhoneInstructions = false,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            multiplePermLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_MEDIA_AUDIO,
                                    Manifest.permission.READ_MEDIA_VIDEO,
                                    Manifest.permission.READ_MEDIA_IMAGES
                                )
                            )
                        } else {
                            multiplePermLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                )
                            )
                        }
                    }
                )
            )
        }
    }
    val essentialGranted = notifGranted && bluetoothGranted && mediaAudioGranted
    LaunchedEffect(essentialGranted) {
        onEssentialGrantedChange(essentialGranted)
    }
    val phoneOnly = remember {
        listOf(
            WearPermissionItemData(
                id = "storage",
                title = context.getString(R.string.storage_access),
                summary = null,
                icon = R.drawable.ic_folder_24dp,
                isGranted = false,
                requiresPhoneInstructions = true,
                onClick = { showPhoneInstructionsDialog = true }
            )
        )
    }
    if (showPhoneInstructionsDialog) {
        AlertDialog(
            visible = true,
            onDismissRequest = { showPhoneInstructionsDialog = false },
            title = { Text(stringResource(R.string.permissions)) },
            text = {
                Text("This permission can't be granted from the watch. Open the app's GitHub repository for setup instructions.")
            },
            confirmButton = {
                Button(onClick = { showPhoneInstructionsDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }
    ScalingLazyColumn(
        state = listState,
        anchorType = ScalingLazyListAnchorType.ItemCenter,
        modifier = Modifier.fillMaxSize()
    ) {
        item { ListHeader { Text(stringResource(R.string.permissions)) } }
        itemsIndexed(requestable) { index, item ->
            CardListItem(
                title = item.title,
                summary = item.summary,
                icon = item.icon,
                status = !item.isGranted,
                position = positionFor(index, requestable.size),
                onClick = if (!item.isGranted) item.onClick else null
            )
        }
        item { ListHeader { Text(stringResource(R.string.advanced_permissions)) } }
        itemsIndexed(phoneOnly) { index, item ->
            CardListItem(
                title = item.title,
                summary = item.summary,
                icon = item.icon,
                status = true,
                position = positionFor(index, phoneOnly.size),
                onClick = item.onClick
            )
        }
    }
}
private fun positionFor(index: Int, size: Int): CardPosition = when {
    size == 1 -> CardPosition.SINGLE
    index == 0 -> CardPosition.TOP
    index == size - 1 -> CardPosition.BOTTOM
    else -> CardPosition.MIDDLE
}
@Composable
private fun TimingsSetupScreen(
    listState: androidx.wear.compose.foundation.lazy.ScalingLazyListState,
    state: WearSetupUiState,
    onSyncToggle: (Boolean) -> Unit,
    onTotalDurationChange: (Int) -> Unit,
    onNoiseDurationChange: (Int) -> Unit,
    onBWNoiseDurationChange: (Int) -> Unit,
    onHorizontalDurationChange: (Int) -> Unit,
    onVerticalDurationChange: (Int) -> Unit
) {
    val slidersEnabled = state.syncState != WearSyncState.Enabled
    ScalingLazyColumn(
        state = listState,
        anchorType = ScalingLazyListAnchorType.ItemCenter,
        modifier = Modifier.fillMaxSize()
    ) {
        item { ListHeader { Text(stringResource(R.string.category_timings)) } }
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SwitchButton(
                    checked = state.syncState == WearSyncState.Enabled,
                    onCheckedChange = onSyncToggle,
                    enabled = state.syncState != WearSyncState.Unavailable,
                    label = { Text("Sync with phone") }
                )
                if (state.syncState == WearSyncState.Unavailable) {
                    Text(
                        text = "Phone not connected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (slidersEnabled) {
            item { Text(stringResource(R.string.total_recovery_time)) }
            item {
                Slider(
                    value = state.timing.totalDuration,
                    onValueChange = onTotalDurationChange,
                    valueProgression = 1..121,
                    enabled = true
                )
            }
            item { Text(stringResource(R.string.noise)) }
            item {
                Slider(
                    value = state.timing.noiseDuration,
                    onValueChange = onNoiseDurationChange,
                    valueProgression = 1..10,
                    enabled = true
                )
            }
            item { Text(stringResource(R.string.black_white_noise)) }
            item {
                Slider(
                    value = state.timing.blackWhiteNoiseDuration,
                    onValueChange = onBWNoiseDurationChange,
                    valueProgression = 1..10,
                    enabled = true
                )
            }
            item { Text(stringResource(R.string.horizontal_lines)) }
            item {
                Slider(
                    value = state.timing.horizontalDuration,
                    onValueChange = onHorizontalDurationChange,
                    valueProgression = 1..10,
                    enabled = true
                )
            }
            item { Text(stringResource(R.string.vertical_lines)) }
            item {
                Slider(
                    value = state.timing.verticalDuration,
                    onValueChange = onVerticalDurationChange,
                    valueProgression = 1..10,
                    enabled = true
                )
            }
        }
    }
}
@Composable
private fun RegionalSetupScreen(
    listState: androidx.wear.compose.foundation.lazy.ScalingLazyListState,
    state: WearSetupUiState,
    onTemperatureUnitChange: (TemperatureUnit) -> Unit
) {
    val currentLocale = java.util.Locale.getDefault()
    val languageDisplayName = currentLocale.getDisplayName(currentLocale)
        .replaceFirstChar { it.titlecase(currentLocale) }
    val tempOptions = remember {
        listOf(
            TemperatureUnit.SYSTEM to ("System default" to CardPosition.TOP),
            TemperatureUnit.CELSIUS to ("Celsius (°C)" to CardPosition.MIDDLE),
            TemperatureUnit.FAHRENHEIT to ("Fahrenheit (°F)" to CardPosition.BOTTOM)
        )
    }
    ScalingLazyColumn(
        state = listState,
        anchorType = ScalingLazyListAnchorType.ItemCenter,
        modifier = Modifier.fillMaxSize()
    ) {
        item { ListHeader { Text(stringResource(R.string.regional_settings)) } }
        item { ListHeader { Text(stringResource(R.string.language)) } }
        item {
            CardListItem(
                title = "Language",
                summary = languageDisplayName,
                icon = R.drawable.ic_language_24dp,
                status = false,
                position = CardPosition.SINGLE,
                onClick = {}
            )
        }
        item { ListHeader { Text("Temperature unit") } }
        tempOptions.forEach { (unit, pair) ->
            val (label, position) = pair
            item {
                val largeCornerRadius = 24.dp
                val smallCornerRadius = 4.dp
                val shape = when (position) {
                    CardPosition.TOP -> RoundedCornerShape(topStart = largeCornerRadius, topEnd = largeCornerRadius, bottomStart = smallCornerRadius, bottomEnd = smallCornerRadius)
                    CardPosition.MIDDLE -> RoundedCornerShape(smallCornerRadius)
                    CardPosition.BOTTOM -> RoundedCornerShape(topStart = smallCornerRadius, topEnd = smallCornerRadius, bottomStart = largeCornerRadius, bottomEnd = largeCornerRadius)
                    CardPosition.SINGLE -> RoundedCornerShape(largeCornerRadius)
                }
                RadioButton(
                    selected = state.temperatureUnit == unit,
                    onSelect = { onTemperatureUnitChange(unit) },
                    enabled = true,
                    label = { Text(label, fontWeight = FontWeight.Medium) },
                    shape = shape,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
@Composable
private fun OtherSetupScreen(
    listState: androidx.wear.compose.foundation.lazy.ScalingLazyListState,
    state: WearSetupUiState,
    onOpenSettingsOnPhone: () -> Unit
) {
    if (!state.phoneConnected) {
        ScalingLazyColumn(
            state = listState,
            anchorType = ScalingLazyListAnchorType.ItemCenter,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    text = "Connect to your phone to manage GitHub integration",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
        return
    }
    ScalingLazyColumn(
        state = listState,
        anchorType = ScalingLazyListAnchorType.ItemCenter,
        modifier = Modifier.fillMaxSize()
    ) {
        item { ListHeader { Text(stringResource(R.string.github_integration)) } }
        item {
            if (state.githubTokenFound) {
                CardListItem(
                    title = "Token found",
                    summary = null,
                    icon = R.drawable.ic_check_circle_24dp,
                    status = false,
                    position = CardPosition.SINGLE,
                    onClick = null
                )
            } else {
                CardListItem(
                    title = "Open on phone",
                    summary = "Set up your GitHub token on the phone app",
                    icon = R.drawable.ic_phone_android_24dp,
                    status = true,
                    position = CardPosition.SINGLE,
                    onClick = onOpenSettingsOnPhone
                )
            }
        }
    }
}
@Composable
private fun FinishSetupScreen(onFinishAndNavigate: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1500, easing = EaseInOut), RepeatMode.Reverse),
        label = "scale"
    )
    AppScaffold {
        val listState = rememberScalingLazyListState()
        ScreenScaffold(
            scrollState = listState,
            edgeButton = {
                EdgeButton(
                    onClick = onFinishAndNavigate,
                    buttonSize = EdgeButtonSize.Large
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_forward_24dp),
                        contentDescription = "Start"
                    )
                }
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                ScalingLazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    anchorType = ScalingLazyListAnchorType.ItemCenter
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
                                    ExpressiveShapeBackground(
                                        iconSize = 90.dp,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        forcedShape = ExpressiveShapeType.CLOVER_8
                                    )
                                }
                                Icon(
                                    painter = painterResource(R.drawable.ic_check_circle_24dp),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = "We are ready!",
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
