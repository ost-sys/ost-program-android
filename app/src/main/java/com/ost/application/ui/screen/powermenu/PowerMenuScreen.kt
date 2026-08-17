package com.ost.application.ui.screen.powermenu
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.LocalBottomSpacing
import com.ost.application.R
import com.ost.application.core.service.OstAccessibilityService
import com.ost.application.ui.components.ExpressiveShapeBackground
import com.ost.application.ui.components.ExpressiveShapeType
import com.ost.application.ui.components.SectionTitle
import com.ost.application.util.AdaptiveSquareCard
import com.ost.application.util.TooltipAction
import com.ost.application.util.TooltipState
import com.ost.application.util.TooltipWrapper
import com.ost.application.util.tooltip
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds
private data class PowerMenuUiItem(
    val iconRes: Int,
    val titleRes: Int,
    val enabled: Boolean,
    val action: PowerAction
)
@Composable
fun PowerMenuScreen(
    modifier: Modifier = Modifier,
    viewModel: PowerMenuViewModel = viewModel()
) {
    val context = LocalContext.current
    val bottomSpacing = LocalBottomSpacing.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        viewModel.hapticEvent.collect { event ->
            val feedbackType = when (event) {
                HapticEvent.CONFIRM -> HapticFeedbackType.Confirm
                HapticEvent.REJECT -> HapticFeedbackType.Reject
            }
            haptic.performHapticFeedback(feedbackType)
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAccessibilityState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "checking_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing)
        ),
        label = "rotation"
    )
    val currentRotation = if (uiState.rootState == RootAccessState.CHECKING) rotation else 0f
    val (statusColor, containerColor, shapeType) = when (uiState.rootState) {
        RootAccessState.CHECKING -> Triple(
            MaterialTheme.colorScheme.onTertiaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer,
            ExpressiveShapeType.COOKIE_9
        )
        RootAccessState.GRANTED -> Triple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer,
            ExpressiveShapeType.PILL
        )
        RootAccessState.DENIED -> Triple(
            MaterialTheme.colorScheme.onSecondaryContainer,
            MaterialTheme.colorScheme.secondaryContainer,
            ExpressiveShapeType.SQUARE
        )
    }
    val rootItems = remember(uiState.rootState, uiState.isSamsungDevice) {
        listOf(
            PowerMenuUiItem(
                R.drawable.ic_power_new_24dp,
                R.string.turn_off,
                uiState.isPowerOffEnabled,
                PowerAction.POWER_OFF
            ),
            PowerMenuUiItem(
                R.drawable.ic_restart_24dp,
                R.string.reboot,
                uiState.isRebootEnabled,
                PowerAction.REBOOT
            ),
            PowerMenuUiItem(
                R.drawable.ic_flash_on_24dp,
                R.string.reboot_recovery,
                uiState.isRecoveryEnabled,
                PowerAction.RECOVERY
            ),
            PowerMenuUiItem(
                R.drawable.ic_download_for_offline_24dp,
                R.string.reboot_download,
                uiState.isDownloadModeEnabled,
                PowerAction.DOWNLOAD
            ),
            PowerMenuUiItem(
                R.drawable.ic_offline_bolt_24dp,
                R.string.reboot_fastboot,
                uiState.isFastbootEnabled,
                PowerAction.FASTBOOT
            ),
            PowerMenuUiItem(
                R.drawable.ic_offline_bolt_24dp,
                R.string.reboot_fastbootd,
                uiState.isFastbootdEnabled,
                PowerAction.FASTBOOTD
            )
        )
    }
    val accessibilityItems = remember {
        listOf(
            PowerMenuUiItem(
                R.drawable.ic_power_new_24dp,
                R.string.turn_off,
                true,
                PowerAction.ACCESSIBILITY_POWER
            ),
            PowerMenuUiItem(
                com.ost.application.core.R.drawable.ic_lock_24dp,
                R.string.lock_screen,
                OstAccessibilityService.isLockScreenSupported(),
                PowerAction.ACCESSIBILITY_LOCK
            )
        )
    }
    val bigRadius = 24.dp
    val smallRadius = 4.dp
    if (uiState.showAccessibilityPromptDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAccessibilityPromptDialog() },
            title = { Text(stringResource(R.string.accessibility_required_title)) },
            text = { Text(stringResource(R.string.accessibility_required_msg)) },
            confirmButton = {
                Button(onClick = {
                    viewModel.dismissAccessibilityPromptDialog()
                    OstAccessibilityService.openAccessibilitySettings(context)
                }) {
                    Text(stringResource(R.string.open_settings))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissAccessibilityPromptDialog() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    TooltipWrapper(modifier = modifier.fillMaxSize()) { tooltipState ->
        LaunchedEffect(uiState.showDialogFor) {
            if (uiState.showDialogFor != null) {
                tooltipState.show()
            } else {
                tooltipState.hide()
            }
        }
        LaunchedEffect(tooltipState.isVisible) {
            if (!tooltipState.isVisible && uiState.showDialogFor != null) {
                kotlinx.coroutines.delay(50.milliseconds)
                if (!tooltipState.isVisible) {
                    viewModel.dismissDialog()
                }
            }
        }
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val availableWidth = maxWidth - 32.dp
            val minCardSize = 150.dp
            val columnsCount = max(2, (availableWidth / minCardSize).toInt())
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnsCount),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 16.dp + bottomSpacing
                ),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp, top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 5.dp)
                        ) {
                            Box(modifier = Modifier.graphicsLayer(rotationZ = currentRotation)) {
                                ExpressiveShapeBackground(
                                    iconSize = 120.dp,
                                    color = containerColor,
                                    forcedShape = shapeType,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.checkRootAccess()
                                    }
                                )
                            }
                            Image(
                                painter = painterResource(id = R.drawable.ic_power_new_24dp),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                colorFilter = ColorFilter.tint(statusColor)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = containerColor
                            )
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                text = stringResource(id = uiState.statusTextResId),
                                color = statusColor,
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                val isRootDenied = uiState.rootState == RootAccessState.DENIED
                val isAccessibilityDisabled = !uiState.isAccessibilityEnabled
                if (isRootDenied && isAccessibilityDisabled) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            onClick = { OstAccessibilityService.openAccessibilitySettings(context) }
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.accessibility_required_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.accessibility_required_msg),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { OstAccessibilityService.openAccessibilitySettings(context) }) {
                                    Text(stringResource(R.string.open_settings))
                                }
                            }
                        }
                    }
                } else {
                    if (!isRootDenied) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(contentAlignment = Alignment.CenterStart) {
                                SectionTitle(title = "Root Actions")
                            }
                        }
                        itemsIndexed(rootItems) { index, item ->
                            GridCardItem(
                                index = index,
                                itemsCount = rootItems.size,
                                columnsCount = columnsCount,
                                item = item,
                                uiState = uiState,
                                tooltipState = tooltipState,
                                viewModel = viewModel,
                                bigRadius = bigRadius,
                                smallRadius = smallRadius
                            )
                        }
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(modifier = Modifier.padding(top = 8.dp), contentAlignment = Alignment.CenterStart) {
                                SectionTitle(title = "Accessibility Actions")
                            }
                        }
                        itemsIndexed(accessibilityItems) { index, item ->
                            GridCardItem(
                                index = index,
                                itemsCount = accessibilityItems.size,
                                columnsCount = columnsCount,
                                item = item,
                                uiState = uiState,
                                tooltipState = tooltipState,
                                viewModel = viewModel,
                                bigRadius = bigRadius,
                                smallRadius = smallRadius
                            )
                        }
                    } else if (!isAccessibilityDisabled) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(contentAlignment = Alignment.CenterStart) {
                                SectionTitle(title = "Accessibility Actions")
                            }
                        }
                        itemsIndexed(accessibilityItems) { index, item ->
                            GridCardItem(
                                index = index,
                                itemsCount = accessibilityItems.size,
                                columnsCount = columnsCount,
                                item = item,
                                uiState = uiState,
                                tooltipState = tooltipState,
                                viewModel = viewModel,
                                bigRadius = bigRadius,
                                smallRadius = smallRadius
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun GridCardItem(
    index: Int,
    itemsCount: Int,
    columnsCount: Int,
    item: PowerMenuUiItem,
    uiState: PowerMenuUiState,
    tooltipState: TooltipState,
    viewModel: PowerMenuViewModel,
    bigRadius: Dp,
    smallRadius: Dp
) {
    val context = LocalContext.current
    val row = index / columnsCount
    val col = index % columnsCount
    val totalRows = (itemsCount + columnsCount - 1) / columnsCount
    val isFirstRow = row == 0
    val isLastRow = row == totalRows - 1
    val isLeftColumn = col == 0
    val isRightColumn = col == columnsCount - 1
    val shape = RoundedCornerShape(
        topStart = if (isFirstRow && isLeftColumn) bigRadius else smallRadius,
        topEnd = if (isFirstRow && isRightColumn) bigRadius else smallRadius,
        bottomStart = if (isLastRow && isLeftColumn) bigRadius else smallRadius,
        bottomEnd = if (isLastRow && isRightColumn) bigRadius else smallRadius
    )
    val isSelectedCard = uiState.showDialogFor == item.action
    AdaptiveSquareCard(
        modifier = Modifier.tooltip(
            state = tooltipState,
            title = stringResource(R.string.attention),
            subtitle = stringResource(id = item.action.messageResId),
            primaryAction = TooltipAction(stringResource(R.string.yes)) {
                viewModel.executeCommand(item.action, context)
                viewModel.dismissDialog()
            },
            secondaryAction = TooltipAction(stringResource(R.string.no)) {
                viewModel.dismissDialog()
            },
            isAnchor = isSelectedCard
        ),
        title = stringResource(item.titleRes),
        icon = item.iconRes,
        status = item.enabled,
        shape = shape,
        onClick = {
            if (uiState.showDialogFor == item.action) {
                viewModel.dismissDialog()
            } else {
                viewModel.onPowerActionClick(item.action)
            }
        }
    )
}
