package com.ost.application

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.ui.screen.about.AboutActivity
import com.ost.application.ui.screen.applist.AppListScreen
import com.ost.application.ui.screen.batteryinfo.BatteryInfoScreen
import com.ost.application.ui.screen.camerainfo.CameraInfoScreen
import com.ost.application.ui.screen.converters.ConvertersScreen
import com.ost.application.ui.screen.cpuinfo.CpuInfoScreen
import com.ost.application.ui.screen.deviceinfo.DeviceInfoScreen
import com.ost.application.ui.screen.display.DisplayInfoScreen
import com.ost.application.ui.screen.info.InfoScreen
import com.ost.application.ui.screen.network.NetworkInfoScreen
import com.ost.application.ui.screen.powermenu.PowerMenuScreen
import com.ost.application.ui.screen.settings.SettingsAction
import com.ost.application.ui.screen.settings.SettingsListContent
import com.ost.application.ui.screen.settings.SettingsUiState
import com.ost.application.ui.screen.settings.SettingsViewModel
import com.ost.application.ui.screen.share.ShareScreen
import com.ost.application.ui.screen.stargazers.StargazersScreen
import com.ost.application.ui.theme.OSTToolsTheme
import com.ost.application.utils.appSettingOpen
import com.ost.application.utils.toast
import com.ost.application.utils.warningPermissionDialog
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
        MenuItemData("app_list", R.string.app_list, R.drawable.ic_apps_24dp),
        MenuItemData("stargazers", R.string.stargazers, R.drawable.ic_star_24dp),
        null,
        MenuItemData("about_device", R.string.about_device, R.drawable.ic_device_24dp),
        MenuItemData("cpu", R.string.cpu, R.drawable.ic_cpu_24dp),
        MenuItemData("battery", R.string.battery, R.drawable.ic_battery_full_24dp),
        MenuItemData("display", R.string.display, R.drawable.ic_display_24dp),
        MenuItemData("network", R.string.network, R.drawable.ic_wifi_24dp),
        MenuItemData("camera", R.string.camera, R.drawable.ic_camera_24dp),
        null,
        MenuItemData("info", R.string.information, R.drawable.ic_help_24dp),
    )
}

private const val MORE_ITEM_ID = "more_button_id"

class MainActivity : ComponentActivity() {
    private val multiplePermissionId = 14
    private val multiplePermissionNameList = if (Build.VERSION.SDK_INT >= 33) {
        arrayListOf(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CAMERA,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CAMERA,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        installSplashScreen()

        setContent {
            OSTToolsTheme {
                MainAppStructure()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == multiplePermissionId) {
            if (grantResults.isNotEmpty()) {
                var isGrant = true
                for (element in grantResults) {
                    if (element == PackageManager.PERMISSION_DENIED) {
                        isGrant = false
                    }
                }
                if (!isGrant) {
                    var someDenied = false
                    for (permission in permissions) {
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                permission
                            )
                        ) {
                            if (ActivityCompat.checkSelfPermission(
                                    this,
                                    permission
                                ) == PackageManager.PERMISSION_DENIED
                            ) {
                                someDenied = true
                            }
                        }
                    }
                    if (someDenied) {
                        appSettingOpen(this)
                    } else {
                        warningPermissionDialog(this) { _: DialogInterface, which: Int ->
                            when (which) {
                                DialogInterface.BUTTON_POSITIVE ->
                                    checkMultiplePermission()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStart() {
        super.onStart()
        checkMultiplePermission()
    }

    private fun checkMultiplePermission(): Boolean {
        val listPermissionNeeded = arrayListOf<String>()
        for (permission in multiplePermissionNameList) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                listPermissionNeeded.add(permission)
            }
        }
        if (listPermissionNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionNeeded.toTypedArray(),
                multiplePermissionId
            )
            return false
        }
        return true
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("AutoboxingStateCreation")
fun MainAppStructure() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val allMenuItems = remember { createMenuItems() }
    val allValidMenuItems = remember(allMenuItems) { allMenuItems.filterNotNull() }
    val bottomNavDirectItems = remember { allValidMenuItems.take(3) }
    val moreMenuItems = remember { allMenuItems.drop(bottomNavDirectItems.size) }
    var selectedScreenId by rememberSaveable { mutableStateOf(bottomNavDirectItems.first().id) }
    val moreBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showMoreBottomSheet by remember { mutableStateOf(false) }

    val settingsViewModel: SettingsViewModel = viewModel()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSettingsSheet by remember { mutableStateOf(false) }

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
                            Log.w("MainAppStructure", "Prevented launching MainActivity or non-existent Activity: ${action.intent}")
                        }
                    } else {
                        Log.e("MainAppStructure", "No Activity found to handle intent: ${action.intent}")
                        context.toast("Could not open the requested screen.")
                    }
                }
                is SettingsAction.ShowToast -> {
                    context.toast(context.getString(action.messageResId))
                }
            }
        }.launchIn(this)
    }

    val currentSelectedItemData = allValidMenuItems.find { it.id == selectedScreenId }
    val currentTitle = currentSelectedItemData?.let { stringResource(it.titleResId) } ?: stringResource(id = R.string.app_name)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = settingsSheetState,
        ) {
            SettingsSheetContent(
                state = settingsState,
                sheetState = settingsSheetState,
                onClose = { scope.launch { settingsSheetState.hide() }.invokeOnCompletion { showSettingsSheet = false } },
                onTotalDurationChange = { floatValue -> settingsViewModel.updateTotalDuration(floatValue.roundToInt()) },
                onNoiseDurationChange = { floatValue -> settingsViewModel.updateNoiseDuration(floatValue.roundToInt()) },
                onBWNoiseDurationChange = { floatValue -> settingsViewModel.updateBlackWhiteNoiseDuration(floatValue.roundToInt()) },
                onHorizontalDurationChange = { floatValue -> settingsViewModel.updateHorizontalDuration(floatValue.roundToInt()) },
                onVerticalDurationChange = { floatValue -> settingsViewModel.updateVerticalDuration(floatValue.roundToInt()) },
                onAboutClick = {
                    scope.launch { settingsSheetState.hide() }.invokeOnCompletion {
                        if (!settingsSheetState.isVisible) {
                            showSettingsSheet = false
                            settingsViewModel.onAboutAppClicked()
                        }
                    }
                }
            )
        }
    }

    if (showMoreBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreBottomSheet = false },
            sheetState = moreBottomSheetState,
        ) {
            MoreBottomSheetContent(
                menuItems = moreMenuItems,
                currentSelectedScreenId = selectedScreenId,
                onItemClick = { itemId ->
                    scope.launch { moreBottomSheetState.hide() }.invokeOnCompletion {
                        if (!moreBottomSheetState.isVisible) {
                            showMoreBottomSheet = false
                        }
                    }
                    selectedScreenId = itemId
                },
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            )
        }
    }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(currentTitle) },
                actions = {
                    IconButton(onClick = {
                        showSettingsSheet = true
                    }) {
                        Icon(
                            painterResource(R.drawable.ic_settings_24dp),
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            AppBottomNavigation(
                directItems = bottomNavDirectItems,
                selectedItemId = if (bottomNavDirectItems.any { it.id == selectedScreenId }) selectedScreenId else MORE_ITEM_ID,
                onItemClick = { itemId ->
                    if (itemId == MORE_ITEM_ID) {
                        scope.launch {
                            if (moreBottomSheetState.isVisible) {
                                moreBottomSheetState.hide()
                                showMoreBottomSheet = false
                            } else {
                                if(showSettingsSheet) {
                                    settingsSheetState.hide()
                                    showSettingsSheet = false
                                }
                                showMoreBottomSheet = true
                            }
                        }
                    } else {
                        if (showMoreBottomSheet) {
                            scope.launch { moreBottomSheetState.hide() }.invokeOnCompletion {
                                if (!moreBottomSheetState.isVisible) showMoreBottomSheet = false
                            }
                        }
                        if (showSettingsSheet) {
                            scope.launch { settingsSheetState.hide() }.invokeOnCompletion {
                                if (!settingsSheetState.isVisible) showSettingsSheet = false
                            }
                        }
                        selectedScreenId = itemId
                    }
                }
            )
        }
    ) { innerPadding ->
        ContentArea(
            selectedItemId = selectedScreenId,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheetContent(
    state: SettingsUiState,
    sheetState: SheetState,
    onClose: () -> Unit,
    onTotalDurationChange: (Float) -> Unit,
    onNoiseDurationChange: (Float) -> Unit,
    onBWNoiseDurationChange: (Float) -> Unit,
    onHorizontalDurationChange: (Float) -> Unit,
    onVerticalDurationChange: (Float) -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .padding(bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Row {
                IconButton(onClick = {
                    val intent = Intent(context, AboutActivity::class.java)
                    context.startActivity(intent)
                }) {
                    Icon(painterResource(R.drawable.ic_info_24dp), contentDescription = stringResource(R.string.about_app))
                }
                IconButton(onClick = onClose) {
                    Icon(painterResource(R.drawable.ic_cancel_24dp), contentDescription = stringResource(R.string.close))
                }
            }

        }
        SettingsListContent(
            state = state,
            onTotalDurationChange = onTotalDurationChange,
            onNoiseDurationChange = onNoiseDurationChange,
            onBWNoiseDurationChange = onBWNoiseDurationChange,
            onHorizontalDurationChange = onHorizontalDurationChange,
            onVerticalDurationChange = onVerticalDurationChange,
            onAboutClick = onAboutClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun AppBottomNavigation(
    directItems: List<MenuItemData>,
    selectedItemId: String,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        directItems.forEach { item ->
            NavigationBarItem(
                selected = item.id == selectedItemId,
                onClick = { onItemClick(item.id) },
                icon = {
                    Icon(
                        painter = painterResource(id = item.iconResId),
                        contentDescription = stringResource(item.titleResId),
                    )
                },
                label = { Text(stringResource(item.titleResId)) }
            )
        }
        NavigationBarItem(
            selected = selectedItemId == MORE_ITEM_ID,
            onClick = { onItemClick(MORE_ITEM_ID) },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_more_horiz_24dp),
                    contentDescription = "More",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text(stringResource(R.string.more)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreBottomSheetContent(
    menuItems: List<MenuItemData?>,
    currentSelectedScreenId: String?,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 8.dp, horizontal = 8.dp)) {
        menuItems.forEach { item ->
            if (item != null) {
                val isSelected = item.id == currentSelectedScreenId
                ListItem(
                    headlineContent = { Text(stringResource(item.titleResId)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = item.iconResId),
                            contentDescription = stringResource(item.titleResId),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        } else {
                            Color.Transparent
                        }
                    ),
                    modifier = Modifier
                        .clickable { onItemClick(item.id) }
                        .clip(CircleShape)
                )
            } else {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }
        }
    }
}


@Composable
fun ContentArea(
    selectedItemId: String?,
    modifier: Modifier = Modifier
) {
    when (selectedItemId) {
        "tools" -> ConvertersScreen(modifier = modifier)
        "power_menu" -> PowerMenuScreen(modifier = modifier)
        "app_list" -> AppListScreen(modifier = modifier)
        "stargazers" -> StargazersScreen(modifier = modifier)
        "about_device" -> DeviceInfoScreen(modifier = modifier)
        "cpu" -> CpuInfoScreen(modifier = modifier)
        "battery" -> BatteryInfoScreen(modifier = modifier)
        "display" -> DisplayInfoScreen(modifier = modifier)
        "network" -> NetworkInfoScreen(modifier = modifier)
        "camera" -> CameraInfoScreen(modifier = modifier)
        "info" -> InfoScreen(modifier = modifier)
        "share_files" -> ShareScreen(modifier = modifier)

        else -> {
            Column(modifier = modifier
                .padding(16.dp)
                .fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Select an option from the bottom bar.")
            }
        }
    }
}

@Preview(showBackground = true, name = "Main App Preview")
@Composable
fun DefaultPreview() {
    OSTToolsTheme {
        MainAppStructure()
    }
}