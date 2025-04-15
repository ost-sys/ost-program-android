package com.ost.application.ui.screen.applist

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.Coil
import coil.compose.rememberAsyncImagePainter
import com.ost.application.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var appList by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorLoading by remember { mutableStateOf<String?>(null) }
    var showSystemApps by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    val isRootAvailable by produceState(initialValue = false) {
        value = RootUtils.isRootAvailable
    }
    LaunchedEffect(isRootAvailable) {
        Log.i("AppListScreen", "Root available check completed: $isRootAvailable")
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val showSnackbar: suspend (String, SnackbarDuration) -> Unit = { message, duration ->
        snackbarHostState.showSnackbar(message = message, duration = duration)
    }

    val refreshAppList: suspend (showLoadingIndicator: Boolean) -> Unit = { showLoadingIndicator ->
        if (showLoadingIndicator) isLoading = true
        errorLoading = null
        try {
            val freshList = getInstalledApps(context)
            appList = freshList
        } catch (e: Exception) {
            Log.e("AppListScreen", "Error refreshing apps", e)
            errorLoading = context.getString(R.string.error_loading_app_list, e.localizedMessage ?: "Unknown")
            appList = emptyList()
        } finally {
            if (showLoadingIndicator) isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshAppList(true)
    }

    val filteredAppList = remember(appList, showSystemApps, searchQuery) {
        val systemFiltered = if (showSystemApps) {
            appList
        } else {
            appList.filter { !it.isSystemApp }
        }
        if (searchQuery.isBlank()) {
            systemFiltered
        } else {
            systemFiltered.filter { appInfo ->
                appInfo.name.contains(searchQuery, ignoreCase = true) ||
                        appInfo.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val searchBarHeight = 64.dp

    Box(modifier = modifier.fillMaxSize()) {

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = searchBarHeight + 8.dp,
                bottom = 120.dp,
                start = 8.dp,
                end = 8.dp
            )
        ) {
            itemsIndexed(
                items = filteredAppList,
                key = { _, appInfo -> appInfo.packageName }
            ) { _, appInfo ->
                AppListItem(
                    appInfo = appInfo,
                    isRootAvailable = isRootAvailable,
                    coroutineScope = coroutineScope,
                    showSnackbar = { msg, dur ->
                        coroutineScope.launch { showSnackbar(msg, dur) }
                    },
                    onActionTriggered = { packageName, isRootUninstall ->
                        coroutineScope.launch {
                            val delayMillis = if (isRootUninstall) 1500L else 3000L
                            delay(delayMillis)
                            refreshAppList(false)
                        }
                    }
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .height(searchBarHeight)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 6.dp,
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                placeholder = { Text(stringResource(R.string.search)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_search_24dp), contentDescription = stringResource(R.string.search)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(painterResource(R.drawable.ic_cancel_24dp), contentDescription = stringResource(R.string.clear_close))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { showSystemApps = !showSystemApps },
                containerColor = if (showSystemApps) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (showSystemApps) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Icon(
                    painter = painterResource(if (showSystemApps) R.drawable.ic_visibility_off_24dp else R.drawable.ic_visibility_24dp),
                    contentDescription = stringResource(if (showSystemApps) R.string.hide_system_apps else R.string.show_system_apps)
                )
            }
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch { refreshAppList(true) }
                }
            ) {
                Icon(painterResource(R.drawable.ic_refresh_24dp), contentDescription = stringResource(R.string.refresh_app_list))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
                .imePadding()
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (errorLoading != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(errorLoading ?: stringResource(R.string.unknown_error), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { coroutineScope.launch { refreshAppList(true) } }) { Text(stringResource(R.string.retry)) }
            }
        } else if (filteredAppList.isEmpty()) {
            Text(
                text = if (searchQuery.isNotBlank()) {
                    stringResource(R.string.no_results_found)
                } else {
                    stringResource(if (showSystemApps) R.string.no_apps_found else R.string.no_non_system_apps_found)
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

private data class BackgroundIconInfo( val painter: Painter? = null, val imageVector: ImageVector? = null, val tint: Color, val alignment: Alignment, val contentDescription: String? )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListItem(
    appInfo: AppInfo,
    isRootAvailable: Boolean,
    coroutineScope: CoroutineScope,
    showSnackbar: suspend (String, SnackbarDuration) -> Unit,
    onActionTriggered: (packageName: String, isRootUninstall: Boolean) -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val view = LocalView.current
    val enableUninstallSwipe = isRootAvailable || !appInfo.isSystemApp

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_ON)
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = "package:${appInfo.packageName}".toUri()
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        coroutineScope.launch { showSnackbar(context.getString(R.string.failed_to_open_settings), SnackbarDuration.Short) }
                    }
                    return@rememberSwipeToDismissBoxState false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_OFF)
                    if (isRootAvailable) {
                        coroutineScope.launch {
                            if (appInfo.isSystemApp) {
                                showSnackbar(context.getString(R.string.warning_uninstalling_system_app, appInfo.name), SnackbarDuration.Long); delay(1500)
                            }
                            showSnackbar(context.getString(R.string.uninstalling_root, appInfo.name), SnackbarDuration.Short)
                            val uninstallSuccess = RootUtils.uninstallAppRoot(appInfo.packageName)
                            if (uninstallSuccess) {
                                showSnackbar(context.getString(R.string.uninstalled_root, appInfo.name), SnackbarDuration.Long)
                                onActionTriggered(appInfo.packageName, true)
                            } else {
                                showSnackbar(context.getString(R.string.failed_to_uninstall_root, appInfo.name), SnackbarDuration.Long)
                            }
                            Coil.reset()
                        }
                    } else {
                        if (!appInfo.isSystemApp) {
                            try {
                                val intent = Intent(Intent.ACTION_DELETE)
                                intent.data = "package:${appInfo.packageName}".toUri()
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                                coroutineScope.launch { showSnackbar(context.getString(R.string.uninstall_request_sent, appInfo.name), SnackbarDuration.Short) }
                                onActionTriggered(appInfo.packageName, false)
                            } catch (e: ActivityNotFoundException) {
                                coroutineScope.launch { showSnackbar(context.getString(R.string.failed_to_launch_uninstall), SnackbarDuration.Short)}
                            } catch (e: Exception) {
                                coroutineScope.launch { showSnackbar(context.getString(R.string.uninstall_error), SnackbarDuration.Short)}
                            }
                        } else {
                            Log.w("AppListItem", "[STD] Uninstall action triggered for system app without root!")
                        }
                        coroutineScope.launch { Coil.reset() }
                    }
                    return@rememberSwipeToDismissBoxState false
                }
                SwipeToDismissBoxValue.Settled -> {
                    return@rememberSwipeToDismissBoxState true
                }
            }
        },
        positionalThreshold = { distance -> distance * 0.25f }
    )

    LaunchedEffect(dismissState.currentValue, appInfo.packageName) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd || dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = enableUninstallSwipe,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val backgroundColor by animateColorAsState(
                targetValue = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                }, label = "background_color_anim"
            )

            val infoIconPainter: Painter = painterResource(id = R.drawable.ic_info_24dp)
            val deleteIconPainter: Painter = painterResource(id = R.drawable.ic_delete_forever_24dp)
            val warningIconPainter: Painter = painterResource(id = R.drawable.ic_warning_24dp)
            val uninstallIconPainter = if (isRootAvailable && appInfo.isSystemApp) warningIconPainter else deleteIconPainter
            val uninstallContentDesc = stringResource(if (isRootAvailable && appInfo.isSystemApp) R.string.delete_system_application else R.string.delete)

            val iconInfo = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> BackgroundIconInfo(
                    painter = infoIconPainter,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    alignment = Alignment.CenterStart,
                    contentDescription = stringResource(R.string.information)
                )
                SwipeToDismissBoxValue.EndToStart -> BackgroundIconInfo(
                    painter = uninstallIconPainter,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    alignment = Alignment.CenterEnd,
                    contentDescription = uninstallContentDesc
                )
                SwipeToDismissBoxValue.Settled -> BackgroundIconInfo(
                    tint = Color.Transparent, alignment = Alignment.CenterStart, contentDescription = null
                )
            }

            val progress = dismissState.progress
            val iconScale by animateFloatAsState(
                targetValue = if (direction != SwipeToDismissBoxValue.Settled) progress.coerceIn(0.5f, 1f) else 0f,
                label = "icon_scale_anim"
            )
            val iconAlpha by animateFloatAsState(
                targetValue = if (direction != SwipeToDismissBoxValue.Settled) progress.coerceIn(0.5f, 1f) else 0f,
                label = "icon_alpha_anim"
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(backgroundColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = iconInfo.alignment
            ) {
                val tint = iconInfo.tint.copy(alpha = iconAlpha)
                if (iconInfo.painter != null) {
                    Icon(iconInfo.painter, iconInfo.contentDescription, Modifier.scale(iconScale), tint = tint)
                } else if (iconInfo.imageVector != null) {
                    Icon(iconInfo.imageVector, iconInfo.contentDescription, Modifier.scale(iconScale), tint = tint)
                }
            }
        }
    ) {
        ElevatedCard(
            onClick = {
                val launchIntent: Intent? = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                if (launchIntent != null) {
                    try { context.startActivity(launchIntent) }
                    catch (e: ActivityNotFoundException) { coroutineScope.launch { showSnackbar( context.getString( R.string.could_not_find_activity_to_launch, appInfo.name ), SnackbarDuration.Short) } }
                    catch (e: SecurityException) { coroutineScope.launch { showSnackbar( context.getString( R.string.no_permission_to_run, appInfo.name ), SnackbarDuration.Short) } }
                    catch (e: Exception) { coroutineScope.launch { showSnackbar( context.getString( R.string.failed_to_launch, appInfo.name ), SnackbarDuration.Short) } }
                } else {
                    coroutineScope.launch { showSnackbar( context.getString( R.string.is_not_a_startup_application, appInfo.name ), SnackbarDuration.Short) }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = appInfo.icon ?: R.drawable.ic_android_24dp,
                        error = painterResource(id = R.drawable.ic_error_24dp),
                        placeholder = painterResource(R.drawable.ic_refresh_24dp)
                    ),
                    contentDescription = stringResource(R.string.app_icon, appInfo.name),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(appInfo.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(appInfo.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(formatBytes(appInfo.sizeBytes), style = MaterialTheme.typography.bodySmall)
                }
                if (appInfo.isSystemApp) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(painterResource(R.drawable.ic_security_24dp), contentDescription = stringResource(R.string.system_app), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}