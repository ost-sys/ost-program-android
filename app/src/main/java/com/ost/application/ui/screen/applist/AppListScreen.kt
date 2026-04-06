@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.ost.application.ui.screen.applist

import android.app.Application
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.ost.application.LocalBottomSpacing
import com.ost.application.R
import com.ost.application.ui.state.FabSize
import com.ost.application.ui.state.LocalFabController
import com.ost.application.util.CardPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@ExperimentalMaterial3ExpressiveApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    modifier: Modifier = Modifier,
    viewModel: AppListViewModel = viewModel(
        factory = AppListViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val bottomSpacing = LocalBottomSpacing.current
    val snackbarHostState = remember { SnackbarHostState() }
    val fabController = LocalFabController.current

    val showSnackbar: suspend (String, SnackbarDuration) -> Unit = { message, duration ->
        snackbarHostState.showSnackbar(message = message, duration = duration)
    }

    LaunchedEffect(Unit) {
        fabController.setFab(
            icon = R.drawable.ic_refresh_24dp,
            description = "Refresh",
            fabSize = FabSize.Small,
            action = { viewModel.refresh(true) }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {

        if (state.isLoading) {
            CircularWavyProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (state.error != null) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(state.error!!, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.refresh(true) }) { Text(stringResource(R.string.retry)) }
            }
        } else if (state.apps.isEmpty()) {
            Text(
                text = if (state.searchQuery.isNotBlank()) stringResource(R.string.no_results_found)
                else stringResource(if (state.showSystemApps) R.string.no_apps_found else R.string.no_non_system_apps_found),
                modifier = Modifier.align(Alignment.Center).padding(16.dp),
                textAlign = TextAlign.Center
            )
        } else {
            val searchBarHeight = 72.dp
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = searchBarHeight + 16.dp,
                    bottom = bottomSpacing + 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(
                    items = state.apps,
                    key = { _, appInfo -> appInfo.packageName }
                ) { index, appInfo ->
                    val position = when {
                        state.apps.size == 1 -> CardPosition.SINGLE
                        index == 0 -> CardPosition.TOP
                        index == state.apps.lastIndex -> CardPosition.BOTTOM
                        else -> CardPosition.MIDDLE
                    }

                    AppListItem(
                        appInfo = appInfo,
                        isRootAvailable = state.isRootAvailable,
                        position = position,
                        coroutineScope = coroutineScope,
                        showSnackbar = showSnackbar,
                        onRootUninstall = { pkg ->
                            coroutineScope.launch {
                                showSnackbar(context.getString(R.string.uninstalling_root, appInfo.name), SnackbarDuration.Short)
                                viewModel.uninstallAppRoot(pkg) { success ->
                                    coroutineScope.launch {
                                        val msgId = if (success) R.string.uninstalled_root else R.string.failed_to_uninstall_root
                                        showSnackbar(context.getString(msgId, appInfo.name), SnackbarDuration.Long)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }

        TopSearchBar(
            searchQuery = state.searchQuery,
            onQueryChange = viewModel::updateSearchQuery,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .height(56.dp)
                .zIndex(1f)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = bottomSpacing + 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FloatingActionButton(
                onClick = { viewModel.toggleSystemApps() },
                containerColor = if (state.showSystemApps) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (state.showSystemApps) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Icon(
                    painter = painterResource(if (state.showSystemApps) R.drawable.ic_visibility_off_24dp else R.drawable.ic_visibility_24dp),
                    contentDescription = stringResource(if (state.showSystemApps) R.string.hide_system_apps else R.string.show_system_apps)
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomSpacing)
                .imePadding()
        )
    }
}

@Composable
fun TopSearchBar(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 4.dp,
        shadowElevation = 2.dp
    ) {
        TextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxSize(),
            placeholder = { Text(stringResource(R.string.search)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_search_24dp), contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
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
}

private data class BackgroundIconInfo(val painter: Painter? = null, val imageVector: ImageVector? = null, val tint: Color, val alignment: Alignment, val contentDescription: String?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListItem(
    appInfo: AppInfo,
    isRootAvailable: Boolean,
    position: CardPosition,
    coroutineScope: CoroutineScope,
    showSnackbar: suspend (String, SnackbarDuration) -> Unit,
    onRootUninstall: (String) -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val view = LocalView.current
    val enableUninstallSwipe = isRootAvailable || !appInfo.isSystemApp

    val largeRadius = 24.dp
    val smallRadius = 4.dp
    val shape = when (position) {
        CardPosition.TOP -> RoundedCornerShape(topStart = largeRadius, topEnd = largeRadius, bottomStart = smallRadius, bottomEnd = smallRadius)
        CardPosition.MIDDLE -> RoundedCornerShape(smallRadius)
        CardPosition.BOTTOM -> RoundedCornerShape(topStart = smallRadius, topEnd = smallRadius, bottomStart = largeRadius, bottomEnd = largeRadius)
        CardPosition.SINGLE -> RoundedCornerShape(largeRadius)
    }

    val verticalPadding = when (position) {
        CardPosition.TOP, CardPosition.SINGLE -> PaddingValues(top = 4.dp, bottom = 1.dp)
        CardPosition.MIDDLE -> PaddingValues(vertical = 1.dp)
        CardPosition.BOTTOM -> PaddingValues(top = 1.dp, bottom = 4.dp)
    }

    var lastActionTime by remember { mutableLongStateOf(0L) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.Settled) return@rememberSwipeToDismissBoxState true

            val now = System.currentTimeMillis()
            if (now - lastActionTime < 1500L) {
                return@rememberSwipeToDismissBoxState false
            }
            lastActionTime = now

            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_ON)
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = "package:${appInfo.packageName}".toUri()
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        coroutineScope.launch { showSnackbar(context.getString(R.string.failed_to_open_settings), SnackbarDuration.Short) }
                    }
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_OFF)
                    if (isRootAvailable) {
                        coroutineScope.launch {
                            if (appInfo.isSystemApp) {
                                showSnackbar(context.getString(R.string.warning_uninstalling_system_app, appInfo.name), SnackbarDuration.Long)
                            }
                        }
                        onRootUninstall(appInfo.packageName)
                    } else {
                        if (!appInfo.isSystemApp) {
                            try {
                                val intent = Intent(Intent.ACTION_DELETE).apply {
                                    data = "package:${appInfo.packageName}".toUri()
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                                coroutineScope.launch { showSnackbar(context.getString(R.string.uninstall_request_sent, appInfo.name), SnackbarDuration.Short) }
                            } catch (e: ActivityNotFoundException) {
                                coroutineScope.launch { showSnackbar(context.getString(R.string.failed_to_launch_uninstall), SnackbarDuration.Short) }
                            } catch (e: Exception) {
                                coroutineScope.launch { showSnackbar(context.getString(R.string.uninstall_error), SnackbarDuration.Short) }
                            }
                        } else {
                            Log.w("AppListItem", "[STD] Uninstall action triggered for system app without root!")
                        }
                    }
                }
                SwipeToDismissBoxValue.Settled -> {}
            }
            return@rememberSwipeToDismissBoxState false
        },
        positionalThreshold = { distance -> distance * 0.40f }
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
            .padding(horizontal = 16.dp)
            .padding(verticalPadding)
            .clip(shape),
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
                    painter = infoIconPainter, tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    alignment = Alignment.CenterStart, contentDescription = stringResource(R.string.information)
                )
                SwipeToDismissBoxValue.EndToStart -> BackgroundIconInfo(
                    painter = uninstallIconPainter, tint = MaterialTheme.colorScheme.onErrorContainer,
                    alignment = Alignment.CenterEnd, contentDescription = uninstallContentDesc
                )
                SwipeToDismissBoxValue.Settled -> BackgroundIconInfo(
                    tint = Color.Transparent, alignment = Alignment.CenterStart, contentDescription = null
                )
            }

            val progress = dismissState.progress
            val iconScale by animateFloatAsState(
                targetValue = if (direction != SwipeToDismissBoxValue.Settled) progress.coerceIn(0.5f, 1f) else 0f, label = "icon_scale_anim"
            )
            val iconAlpha by animateFloatAsState(
                targetValue = if (direction != SwipeToDismissBoxValue.Settled) progress.coerceIn(0.5f, 1f) else 0f, label = "icon_alpha_anim"
            )

            Box(
                Modifier.fillMaxSize().background(backgroundColor).padding(horizontal = 20.dp),
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
        Card(
            onClick = {
                val launchIntent: Intent? = packageManager.getLaunchIntentForPackage(appInfo.packageName)
                if (launchIntent != null) {
                    try { context.startActivity(launchIntent) }
                    catch (e: Exception) { coroutineScope.launch { showSnackbar(context.getString(R.string.failed_to_launch, appInfo.name), SnackbarDuration.Short) } }
                } else {
                    coroutineScope.launch { showSnackbar(context.getString(R.string.is_not_a_startup_application, appInfo.name), SnackbarDuration.Short) }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
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