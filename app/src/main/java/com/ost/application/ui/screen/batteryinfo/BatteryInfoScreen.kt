package com.ost.application.ui.screen.batteryinfo

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.LocalBottomSpacing
import com.ost.application.R
import com.ost.application.ui.component.ExpressiveShapeBackground
import com.ost.application.ui.component.ExpressiveShapeType
import com.ost.application.util.CardPosition
import com.ost.application.util.CustomCardItem
import kotlinx.coroutines.delay

private data class BatteryInfoRow(
    val titleRes: Int,
    val summary: String,
    val isLoading: Boolean = false
)

@Composable
fun BatteryInfoScreen(
    modifier: Modifier = Modifier,
    viewModel: BatteryInfoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bottomSpacing = LocalBottomSpacing.current

    val infiniteTransition = rememberInfiniteTransition(label = "infinite rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    val currentRotation = if (uiState.displayMode == BatteryDisplayMode.CHARGING) rotation else 0f

    var chargingShape by remember { mutableStateOf(ExpressiveShapeType.COOKIE_9) }

    LaunchedEffect(uiState.displayMode) {
        if (uiState.displayMode == BatteryDisplayMode.CHARGING) {
            while (true) {
                delay(1000)
                var newShape = ExpressiveShapeType.entries.random()
                while (newShape == chargingShape) {
                    newShape = ExpressiveShapeType.entries.random()
                }
                chargingShape = newShape
            }
        }
    }

    val targetShape = when (uiState.displayMode) {
        BatteryDisplayMode.CHARGING -> chargingShape
        BatteryDisplayMode.POWER_SAVE -> ExpressiveShapeType.CLOVER_4
        BatteryDisplayMode.NORMAL -> ExpressiveShapeType.SQUARE
    }

    val (starColor, iconColor) = when (uiState.displayMode) {
        BatteryDisplayMode.CHARGING -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        BatteryDisplayMode.POWER_SAVE -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        BatteryDisplayMode.NORMAL -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }

    val batteryInfoRows = remember(uiState) {
        listOf(
            BatteryInfoRow(R.string.health, uiState.health),
            BatteryInfoRow(R.string.status, uiState.status),
            BatteryInfoRow(R.string.temperature, uiState.temperature),
            BatteryInfoRow(R.string.voltage, uiState.voltage),
            BatteryInfoRow(R.string.technology, uiState.technology),
            BatteryInfoRow(R.string.capacity, uiState.capacity, uiState.isLoadingCapacity)
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp + bottomSpacing),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 5.dp)
                ) {
                    Box(modifier = Modifier.graphicsLayer(rotationZ = currentRotation)) {
                        ExpressiveShapeBackground(
                            iconSize = 120.dp,
                            color = starColor,
                            forcedShape = targetShape
                        )
                    }

                    Image(
                        painter = painterResource(id = uiState.iconResId),
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        colorFilter = ColorFilter.tint(iconColor)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = uiState.levelText,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        itemsIndexed(batteryInfoRows) { index, item ->
            val position = when {
                batteryInfoRows.size == 1 -> CardPosition.SINGLE
                index == 0 -> CardPosition.TOP
                index == batteryInfoRows.lastIndex -> CardPosition.BOTTOM
                else -> CardPosition.MIDDLE
            }

            val summaryText = if (item.isLoading) stringResource(R.string.loading) else item.summary

            CustomCardItem(
                title = stringResource(item.titleRes),
                summary = summaryText,
                position = position
            )
        }
    }
}