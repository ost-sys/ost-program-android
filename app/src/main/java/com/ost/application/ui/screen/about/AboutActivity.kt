@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)

package com.ost.application.ui.screen.about

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ost.application.R
import com.ost.application.ui.theme.OSTToolsTheme
import com.ost.application.utils.toast
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class AboutActivity : ComponentActivity() {

    private val viewModel: AboutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSTToolsTheme {
                AboutScreen(viewModel = viewModel, onNavigateBack = { finish() })
            }
        }
    }
}

@Composable
fun AboutScreen(
    viewModel: AboutViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val launchers = rememberActionLaunchers(viewModel = viewModel)
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    LaunchedEffect(key1 = viewModel.action) {
        viewModel.action.onEach { action ->
            handleAboutAction(action, context, launchers)
        }.launchIn(this)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Log.d("AboutScreen", "ON_RESUME triggered")
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.about_app), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back_24dp),
                            contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::onChangelogClick, enabled = !state.changelog.isNullOrEmpty()) {
                        Icon(
                            painterResource(R.drawable.ic_update_24dp),
                            contentDescription = stringResource(R.string.changelog))
                    }
                    IconButton(onClick = viewModel::onAppInfoClick) {
                        Icon(
                            painterResource(R.drawable.ic_info_24dp),
                            contentDescription = stringResource(R.string.about_app))
                    }
                    IconButton(onClick = { viewModel.checkUpdate(showToast = true) }) {
                        Icon(
                            painterResource(id = R.drawable.ic_refresh_24dp),
                            contentDescription = stringResource(R.string.check_for_updates)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                AboutHeaderContent(
                    state = state,
                    viewModel = viewModel,
                    onSocialClick = viewModel::onSocialClick,
                    showUpdateConfirmationDialog = viewModel::showUpdateConfirmation,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }

    UpdateConfirmationDialog(
        show = state.showUpdateConfirmDialog,
        latestVersion = state.latestPhoneVersionName,
        changelog = state.changelog,
        onDismiss = viewModel::dismissUpdateConfirmation,
        onConfirm = viewModel::startDownload
    )

    DownloadProgressDialog(
        show = state.showDownloadDialog,
        progress = state.downloadProgress,
        onCancel = viewModel::cancelDownload
    )

    ChangelogDialog(
        show = state.showChangelogDialog,
        version = state.latestPhoneVersionName ?: state.currentVersionName,
        changelog = state.changelog,
        onDismiss = viewModel::dismissChangelogDialog
    )
}

@Composable
fun AboutHeaderContent(
    state: AboutUiState,
    viewModel: AboutViewModel,
    onSocialClick: (String) -> Unit,
    showUpdateConfirmationDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var rotationState by remember { mutableFloatStateOf(0f) }

    val rotationDegrees by animateFloatAsState(
        targetValue = rotationState,
        animationSpec = tween(durationMillis = 500),
        label = "iconRotation"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(vertical = 24.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    rotationState += 60f
                    viewModel.onAppIconClick()
                }
        ) {
            Image(
                painter = painterResource(id = R.drawable.star_background),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .size(150.dp)
                    .graphicsLayer(rotationZ = rotationDegrees)
            )
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(id = R.string.app_name),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer),
                modifier = Modifier
                    .size(170.dp)
            )
        }

        Text(
            text = stringResource(id = R.string.app_name),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            VersionInfoBadge(
                isWearBadge = false,
                versionText = "v${state.currentVersionName}",
                phoneUpdateState = state.updateState,
                wearUpdateState = null,
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onUpdateIconClick = if (state.updateState == UpdateState.AVAILABLE) showUpdateConfirmationDialog else null
            )

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(visible = state.wearNodeConnected) {
                VersionInfoBadge(
                    isWearBadge = true,
                    versionText = state.installedWearVersionName ?: stringResource(
                        id = when(state.wearUpdateCheckState) {
                            WearUpdateCheckState.ERROR -> R.string.wear_version_not_found
                            else -> R.string.checking_for_updates
                        }
                    ),
                    phoneUpdateState = null,
                    wearUpdateState = state.wearUpdateCheckState,
                    backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onUpdateIconClick = null
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SocialButton(R.drawable.about_page_github, R.string.github) { onSocialClick("https://github.com/ost-sys/") }
            SocialButton(R.drawable.about_page_telegram, R.string.telegram) { onSocialClick("https://t.me/ost_news5566") }
            SocialButton(R.drawable.about_page_youtube, R.string.youtube) { onSocialClick("https://www.youtube.com/channel/UC6wNi6iQFVSnd-eJivuG3_Q") }
            SocialButton(R.drawable.about_page_tt, R.string.tiktok) { onSocialClick("https://www.tiktok.com/@ost5566") }
            SocialButton(R.drawable.ic_internet_24dp, R.string.website) { onSocialClick("https://ost-sys.github.io") }
        }
    }
}

@Composable
fun VersionInfoBadge(
    isWearBadge: Boolean,
    versionText: String,
    phoneUpdateState: UpdateState?,
    wearUpdateState: WearUpdateCheckState?,
    backgroundColor: Color,
    contentColor: Color,
    onUpdateIconClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val isLoading = if (isWearBadge) {
        wearUpdateState == WearUpdateCheckState.CHECKING_GITHUB || wearUpdateState == WearUpdateCheckState.REQUESTING_INSTALLED
    } else {
        phoneUpdateState == UpdateState.CHECKING
    }

    val statusIconInfo: Pair<Int, Int>? = remember(phoneUpdateState, wearUpdateState) {
        when {
            isWearBadge && wearUpdateState == WearUpdateCheckState.AVAILABLE ->
                R.drawable.ic_download_for_offline_24dp to R.string.update_available
            isWearBadge && wearUpdateState == WearUpdateCheckState.UP_TO_DATE ->
                R.drawable.ic_check_circle_24dp to R.string.latest_version_installed

            !isWearBadge && phoneUpdateState == UpdateState.AVAILABLE && onUpdateIconClick != null ->
                R.drawable.ic_download_for_offline_24dp to R.string.update_available
            !isWearBadge && phoneUpdateState == UpdateState.NOT_AVAILABLE ->
                R.drawable.ic_check_circle_24dp to R.string.latest_version_installed

            else -> null
        }
    }


    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .heightIn(min = 32.dp)
    ) {
        val iconRes = if (isWearBadge) R.drawable.ic_watch_24dp else R.drawable.ic_phone_android_24dp
        val iconDesc = if (isWearBadge) R.string.wear_os_app else R.string.android_version
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = stringResource(iconDesc),
            tint = contentColor.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = versionText,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (isLoading || statusIconInfo != null) {
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (isLoading || statusIconInfo != null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(20.dp)) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = contentColor
                    )
                } else {
                    statusIconInfo?.let { (iconResId, descResId) ->
                        val tint = if (iconResId == R.drawable.ic_download_for_offline_24dp) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            contentColor.copy(alpha = 0.9f)
                        }
                        val clickableModifier = if (!isWearBadge && iconResId == R.drawable.ic_download_for_offline_24dp && onUpdateIconClick != null) {
                            Modifier.clickable(onClick = onUpdateIconClick)
                        } else {
                            Modifier
                        }
                        Icon(
                            painter = painterResource(id = iconResId),
                            contentDescription = stringResource(id = descResId),
                            tint = tint,
                            modifier = clickableModifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun SocialButton(iconRes: Int, contentDescRes: Int, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.onPrimary,
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = stringResource(id = contentDescRes),
        )
    }
}

@Composable
fun UpdateConfirmationDialog(
    show: Boolean,
    latestVersion: String?,
    changelog: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (show && latestVersion != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(painterResource(R.drawable.ic_download_for_offline_24dp), contentDescription = null) },
            title = { Text(stringResource(R.string.update_available)) },
            text = {
                Column(modifier = Modifier.heightIn(max= 400.dp)) {
                    Text(stringResource(R.string.install_update_q, latestVersion))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.changelog).uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    LazyColumn {
                        item {
                            Text(changelog ?: stringResource(R.string.no_changelog_available), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(stringResource(R.string.install))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun DownloadProgressDialog(
    show: Boolean,
    progress: Int,
    onCancel: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.update)) },
            text = {
                Column {
                    Text(stringResource(R.string.downloading_update, progress))
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun ChangelogDialog(
    show: Boolean,
    version: String,
    changelog: String?,
    onDismiss: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.version, version)) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    item {
                        Text(
                            text = changelog?.takeIf { it.isNotBlank() } ?: stringResource(R.string.no_changelog_available),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )
    }
}

data class AboutActionLaunchers(
    val permissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    val activityResultLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    val installPermissionLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>,
    val storagePermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
)

@Composable
fun rememberActionLaunchers(viewModel: AboutViewModel): AboutActionLaunchers {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        viewModel.refreshPermissions()
    }

    val activityResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult -> }

    val installPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshPermissions()
        val canInstall = context.packageManager.canRequestPackageInstalls()
        if (canInstall) {
            context.toast(context.getString(R.string.permission_granted))
        } else {
            context.toast(context.getString(R.string.install_unknown_apps_permission))
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.refreshPermissions()
        val writeGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
        if (writeGranted) {
            context.toast(context.getString(R.string.permission_granted))
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            context.toast(context.getString(R.string.storage_permission_r))
        }
    }

    return remember {
        AboutActionLaunchers(
            permissionLauncher = permissionLauncher,
            activityResultLauncher = activityResultLauncher,
            installPermissionLauncher = installPermissionLauncher,
            storagePermissionLauncher = storagePermissionLauncher
        )
    }
}

fun handleAboutAction(
    action: AboutAction,
    context: Context,
    launchers: AboutActionLaunchers
) {
    when (action) {
        is AboutAction.ShowToast -> context.toast(action.message)
        is AboutAction.ShowToastRes -> context.toast(context.getString(action.messageResId))
        is AboutAction.LaunchIntent -> {
            try {
                context.startActivity(action.intent)
            } catch (_: ActivityNotFoundException) {
                context.toast(context.getString(R.string.no_suitable_activity_found))
                Log.w("AboutScreenAction", "No activity found for intent: ${action.intent}")
            } catch (e: SecurityException) {
                context.toast(context.getString(R.string.permission_not_granted))
                Log.e("AboutScreenAction", "SecurityException launching intent: ${action.intent}", e)
            } catch (e: Exception) {
                context.toast(context.getString(R.string.error))
                Log.e("AboutScreenAction", "Error launching intent: ${action.intent}", e)
            }
        }
        is AboutAction.RequestPermission -> {
            launchers.permissionLauncher.launch(action.permission)
        }
        AboutAction.RequestInstallPermission -> {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = "package:${context.packageName}".toUri()
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                launchers.installPermissionLauncher.launch(intent)
            } else {
                Log.e("AboutScreenAction", "Cannot resolve ACTION_MANAGE_UNKNOWN_APP_SOURCES intent")
                context.toast(context.getString(R.string.error))
            }
        }
        AboutAction.RequestStoragePermissionLegacy -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                launchers.storagePermissionLauncher.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            } else {
                Log.i("AboutScreenAction", "RequestStoragePermissionLegacy called on API >= 29")
            }
        }
    }
}