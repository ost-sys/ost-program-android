@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)

package com.ost.application.ui.activity.about

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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ost.application.BuildConfig
import com.ost.application.R
import com.ost.application.ui.component.ExpressiveShapeBackground
import com.ost.application.ui.theme.OSTToolsTheme
import com.ost.application.util.CardPosition
import com.ost.application.util.CustomCardItem
import com.ost.application.util.SectionTitle
import com.ost.application.util.toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AboutActivity : ComponentActivity() {
    private val viewModel: AboutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OSTToolsTheme {
                AboutScreen(viewModel = viewModel, onNavigateBack = { finish() })
            }
        }
    }
}

private data class InfoCardData(
    val iconRes: Int,
    val title: String,
    val summary: String?,
    val onClick: () -> Unit
)

@Composable
fun AboutScreen(
    viewModel: AboutViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val launchers = rememberActionLaunchers(viewModel = viewModel)
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    val devicesList = remember(context) {
        listOf(
            InfoCardData(R.drawable.ic_iphone_24dp, "iPhone 13 mini", "MLK13ZA/A", viewModel::onIphoneClick),
            InfoCardData(R.drawable.ic_mobile_24dp, "Galaxy S10", "SM-G937F", viewModel::onAndroidPhoneClick),
            InfoCardData(R.drawable.ic_desktop_windows_24dp, context.getString(R.string.computer), context.getString(R.string.information_pc), viewModel::onPcClick),
            InfoCardData(R.drawable.ic_laptop_mac_24dp, "MacBook Air", "Apple M4, MC6T4PA/A, 16/256", viewModel::onLaptopClick),
            InfoCardData(R.drawable.ic_watch_24dp, "Galaxy Watch 7", "SM-L300", viewModel::onWatchClick),
            InfoCardData(R.drawable.ic_galaxy_buds_24dp, "Galaxy Buds 2", "SM-R177", viewModel::onGalaxyBudsClick),
            InfoCardData(R.drawable.ic_airpods_pro_24dp, "AirPods Pro (2nd generation)", "A2698", viewModel::onAirPodsClick)
        )
    }

    LaunchedEffect(key1 = viewModel.action) {
        viewModel.action.onEach { action ->
            handleAboutAction(action, context, launchers)
        }.launchIn(this)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                expandedHeight = 152.dp,
                title = { Text(stringResource(R.string.about_app), maxLines = 1, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    FilledTonalIconButton(onClick = onNavigateBack) {
                        Icon(painter = painterResource(R.drawable.ic_arrow_back_24dp), contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::onChangelogClick, enabled = !state.changelog.isNullOrEmpty()) {
                        Icon(painter = painterResource(R.drawable.ic_update_24dp), contentDescription = stringResource(R.string.changelog))
                    }
                    IconButton(onClick = { viewModel.checkUpdate(showToast = true) }) {
                        Icon(painter = painterResource(id = R.drawable.ic_refresh_24dp), contentDescription = stringResource(R.string.check_for_updates))
                    }
                    FilledTonalIconButton(onClick = viewModel::onAppInfoClick) {
                        Icon(painter = painterResource(R.drawable.ic_info_24dp), contentDescription = stringResource(R.string.about_app))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = paddingValues.calculateTopPadding())
                .padding(bottom = paddingValues.calculateBottomPadding() + 16.dp),
        ) {
            AboutHeaderContent(
                state = state,
                viewModel = viewModel,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            SectionTitle(stringResource(R.string.current_devices))

            devicesList.forEachIndexed { index, item ->
                val position = when {
                    devicesList.size == 1 -> CardPosition.SINGLE
                    index == 0 -> CardPosition.TOP
                    index == devicesList.lastIndex -> CardPosition.BOTTOM
                    else -> CardPosition.MIDDLE
                }
                CustomCardItem(
                    position = position,
                    icon = item.iconRes,
                    iconPainter = null,
                    title = item.title,
                    summary = item.summary,
                    status = true,
                    onClick = item.onClick
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

    WearUpdateInstructionsDialog(
        show = state.showWearUpdateInstructionsDialog,
        wearApkUrl = state.latestWearApkUrl,
        onDismiss = viewModel::dismissWearUpdateInstructions,
        onDownloadClick = viewModel::openWearApkDownloadLink
    )

    PcInfoDialog(
        showDialog = state.showPcInfoDialog,
        onDismiss = viewModel::dismissPcDialog
    )
}

@Composable
fun AboutHeaderContent(
    state: AboutUiState,
    viewModel: AboutViewModel,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 24.dp)
        ) {
            ExpressiveShapeBackground(
                iconSize = 150.dp,
                color = MaterialTheme.colorScheme.primaryContainer,
                forcedShape = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.onAppIconClick()
                }
            )

            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground_app),
                contentDescription = stringResource(id = R.string.app_name),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer),
                modifier = Modifier.size(125.dp)
            )
        }

        Text(
            text = stringResource(id = R.string.app_name),
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily(Font(R.font.google_sans_bold)),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Text(
            modifier = Modifier.alpha(0.8f),
            text = BuildConfig.APPLICATION_ID,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.Center) {
            VersionInfoBadge(
                isWearBadge = false,
                versionText = state.currentVersionName,
                phoneUpdateState = state.updateState,
                wearUpdateState = null,
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onPhoneUpdateClick = if (state.updateState == UpdateState.AVAILABLE) viewModel::showUpdateConfirmation else null,
                onWearUpdateClick = null
            )

            Spacer(modifier = Modifier.size(8.dp))

            AnimatedVisibility(
                visible = state.wearNodeConnected,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                val wearVersionDisplay = when {
                    state.installedWearVersionName != null -> state.installedWearVersionName
                    state.wearUpdateCheckState == WearUpdateCheckState.ERROR -> stringResource(R.string.wear_version_not_found)
                    state.wearUpdateCheckState == WearUpdateCheckState.IDLE ||
                            state.wearUpdateCheckState == WearUpdateCheckState.CHECKING_GITHUB ||
                            state.wearUpdateCheckState == WearUpdateCheckState.REQUESTING_INSTALLED -> stringResource(R.string.checking_for_updates)
                    else -> stringResource(R.string.wear_version_unknown)
                }

                VersionInfoBadge(
                    isWearBadge = true,
                    versionText = wearVersionDisplay,
                    phoneUpdateState = null,
                    wearUpdateState = state.wearUpdateCheckState,
                    backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onPhoneUpdateClick = null,
                    onWearUpdateClick = if (state.wearUpdateCheckState == WearUpdateCheckState.AVAILABLE) {
                        viewModel::showWearUpdateInstructions
                    } else {
                        null
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        UnifiedSocialLinks(onLinkClick = viewModel::onSocialClick)
    }
}

@Composable
fun PcInfoDialog(showDialog: Boolean, onDismiss: () -> Unit) {
    AnimatedVisibility(
        visible = showDialog,
        enter = scaleIn(animationSpec = tween(durationMillis = 300)) + fadeIn(animationSpec = tween(durationMillis = 200)),
        exit = scaleOut(animationSpec = tween(durationMillis = 300)) + fadeOut(animationSpec = tween(durationMillis = 200))
    ) {
        AlertDialog(
            icon = {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    ExpressiveShapeBackground(
                        iconSize = 64.dp,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_desktop_windows_24dp),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            },
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.computer)) },
            text = { LazyColumn { item { PcInfoContent() } } },
            confirmButton = {
                Button(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) }
            }
        )
    }
}

@Composable
fun PcInfoContent() {
    Column {
        PcSpecItem("${stringResource(R.string.cpu)}:", "Intel Core i5-10400F")
        PcSpecItem("${stringResource(R.string.gpu)}:", "ASUS Strix GeForce GTX 1070")
        PcSpecItem("${stringResource(R.string.ram)}:", "x4 Lexar 8192 MB 2666 MHz")
        PcSpecItem("${stringResource(R.string.motherboard)}:", "ASUS Prime B560-PLUS")
        PcSpecItem("${stringResource(R.string.rom)}:", "HDD 1TB\nM2_2 SATA 256GB\nSATA SSD 240 GB")
    }
}

@Composable
fun PcSpecItem(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp))
        Text(value)
    }
}

private data class SocialLinkData(
    @DrawableRes val iconRes: Int,
    @StringRes val contentDescRes: Int,
    val url: String
)

@Composable
fun UnifiedSocialLinks(onLinkClick: (String) -> Unit) {
    val cornerRadiusLarge = 50.dp
    val cornerRadiusMedium = 8.dp

    val cornerValues = remember {
        listOf(
            listOf(cornerRadiusLarge, cornerRadiusMedium, cornerRadiusMedium, cornerRadiusLarge),
            listOf(cornerRadiusMedium, cornerRadiusMedium, cornerRadiusMedium, cornerRadiusMedium),
            listOf(cornerRadiusMedium, cornerRadiusMedium, cornerRadiusMedium, cornerRadiusMedium),
            listOf(cornerRadiusMedium, cornerRadiusMedium, cornerRadiusMedium, cornerRadiusMedium),
            listOf(cornerRadiusMedium, cornerRadiusLarge, cornerRadiusLarge, cornerRadiusMedium)
        )
    }

    val socialLinks = remember {
        listOf(
            SocialLinkData(R.drawable.about_page_github, R.string.github, "https://github.com/ost-sys/"),
            SocialLinkData(R.drawable.about_page_telegram, R.string.telegram, "https://t.me/ost_news5566"),
            SocialLinkData(R.drawable.about_page_youtube, R.string.youtube, "https://www.youtube.com/channel/UC6wNi6iQFVSnd-eJivuG3_Q"),
            SocialLinkData(R.drawable.about_page_tt, R.string.tiktok, "https://www.tiktok.com/@your_profile"),
            SocialLinkData(R.drawable.ic_internet_24dp, R.string.website, "https://ost-sys.github.io")
        )
    }

    val scope = rememberCoroutineScope()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        socialLinks.forEachIndexed { index, linkData ->
            StyledSocialButton(
                iconRes = linkData.iconRes,
                contentDesc = stringResource(linkData.contentDescRes),
                defaultCorners = cornerValues[index],
                onClick = {
                    scope.launch {
                        delay(1L)
                        onLinkClick(linkData.url)
                    }
                }
            )
        }
    }
}

@Composable
fun StyledSocialButton(
    iconRes: Int,
    contentDesc: String,
    defaultCorners: List<Dp>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val shapeAnimationDuration = 100
    val colorAnimationDuration = 100

    val containerColor by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(durationMillis = colorAnimationDuration),
        label = "ContainerColorAnimation"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = colorAnimationDuration),
        label = "ContentColorAnimation"
    )

    val buttonWidth = 64.dp
    val pressedRadius = buttonWidth / 2

    val topStart by animateDpAsState(targetValue = if (isPressed) pressedRadius else defaultCorners[0], animationSpec = tween(shapeAnimationDuration), label = "TopStart")
    val topEnd by animateDpAsState(targetValue = if (isPressed) pressedRadius else defaultCorners[1], animationSpec = tween(shapeAnimationDuration), label = "TopEnd")
    val bottomEnd by animateDpAsState(targetValue = if (isPressed) pressedRadius else defaultCorners[2], animationSpec = tween(shapeAnimationDuration), label = "BottomEnd")
    val bottomStart by animateDpAsState(targetValue = if (isPressed) pressedRadius else defaultCorners[3], animationSpec = tween(shapeAnimationDuration), label = "BottomStart")

    val finalShape = RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)

    Surface(
        modifier = modifier.size(width = buttonWidth, height = 64.dp),
        shape = finalShape,
        color = containerColor,
        contentColor = contentColor,
        onClick = onClick,
        interactionSource = interactionSource,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(painter = painterResource(id = iconRes), contentDescription = contentDesc, modifier = Modifier.size(28.dp))
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
    onPhoneUpdateClick: (() -> Unit)?,
    onWearUpdateClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val isLoading = if (isWearBadge) {
        wearUpdateState == WearUpdateCheckState.CHECKING_GITHUB || wearUpdateState == WearUpdateCheckState.REQUESTING_INSTALLED
    } else {
        phoneUpdateState == UpdateState.CHECKING
    }

    val statusIconInfo: Pair<Int, Int>? = remember(phoneUpdateState, wearUpdateState) {
        when {
            isWearBadge && wearUpdateState == WearUpdateCheckState.AVAILABLE -> R.drawable.ic_info_24dp to R.string.wear_update_instructions_button
            isWearBadge && wearUpdateState == WearUpdateCheckState.UP_TO_DATE -> R.drawable.ic_check_circle_24dp to R.string.latest_version_installed
            !isWearBadge && phoneUpdateState == UpdateState.AVAILABLE && onPhoneUpdateClick != null -> R.drawable.ic_download_for_offline_24dp to R.string.update_available
            !isWearBadge && phoneUpdateState == UpdateState.NOT_AVAILABLE -> R.drawable.ic_check_circle_24dp to R.string.latest_version_installed
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

        if (isLoading) {
            CircularWavyProgressIndicator(modifier = Modifier.size(18.dp), color = contentColor)
        } else {
            statusIconInfo?.let { (iconResId, descResId) ->
                val clickModifier = if ((!isWearBadge && onPhoneUpdateClick != null) || (isWearBadge && onWearUpdateClick != null)) Modifier.clickable(onClick = if(!isWearBadge) onPhoneUpdateClick!! else onWearUpdateClick!!) else Modifier
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = stringResource(id = descResId),
                    tint = contentColor.copy(alpha = 0.9f),
                    modifier = clickModifier.size(20.dp)
                )
            }
        }
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
                    LinearWavyProgressIndicator(
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
            title = { Text("${stringResource(R.string.version, version)} ${BuildConfig.VERSION_NAME}") },
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

@Composable
fun WearUpdateInstructionsDialog(
    show: Boolean,
    wearApkUrl: String?,
    onDismiss: () -> Unit,
    onDownloadClick: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(painterResource(R.drawable.ic_watch_24dp), contentDescription = null) },
            title = { Text(stringResource(R.string.wear_update_manual_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.wear_update_manual_instructions))
                    if (wearApkUrl != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.wear_update_manual_download_apk), style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                if (wearApkUrl != null) {
                    TextButton(onClick = onDownloadClick) {
                        Text(stringResource(R.string.wear_update_manual_download_button))
                    }
                } else {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            },
            dismissButton = {
                if (wearApkUrl != null) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
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