package com.ost.application.ui.screen.batteryinfo

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.LocalBottomSpacing
import com.ost.application.R
import com.ost.application.core.battery.BatteryDisplayMode
import com.ost.application.core.battery.BatteryHealth
import com.ost.application.core.settings.convertTemperature
import com.ost.application.core.settings.formatTemperatureFloat
import com.ost.application.util.CardPosition
import com.ost.application.util.CustomCardItem
import java.util.Locale
import kotlin.math.max

private enum class GridCardKind { HEALTH, TEMPERATURE, VOLTAGE, CYCLE_COUNT }
private enum class GridLayoutTier { NARROW, WIDE_STACK, WIDE_ROW }

private fun BatteryHealth.toHeartIconResId(): Int = when (this) {
    BatteryHealth.GOOD, BatteryHealth.COLD -> R.drawable.ic_favorite_fill_24dp
    BatteryHealth.OVERHEAT, BatteryHealth.DEAD,
    BatteryHealth.OVER_VOLTAGE, BatteryHealth.UNSPECIFIED_FAILURE -> R.drawable.ic_heart_broken_24dp
    BatteryHealth.UNKNOWN -> R.drawable.ic_favorite_outline_24dp
}

@Composable
private fun BatteryHealth.toHeartColor(): Color = when (this) {
    BatteryHealth.GOOD -> MaterialTheme.colorScheme.primary
    BatteryHealth.COLD -> MaterialTheme.colorScheme.tertiary
    BatteryHealth.OVERHEAT, BatteryHealth.DEAD,
    BatteryHealth.OVER_VOLTAGE, BatteryHealth.UNSPECIFIED_FAILURE -> MaterialTheme.colorScheme.error
    BatteryHealth.UNKNOWN -> MaterialTheme.colorScheme.outline
}

@Composable
private fun HealthCardContent(
    healthStatus: BatteryHealth,
    healthText: String,
    modifier: Modifier = Modifier
) {
    val heartColor = healthStatus.toHeartColor()
    val heartIcon = healthStatus.toHeartIconResId()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(heartIcon),
                contentDescription = null,
                tint = heartColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(R.string.health),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = healthText,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = heartColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = heartColor.copy(alpha = 0.15f),
            contentColor = heartColor
        ) {
            Text(
                text = if (healthStatus == BatteryHealth.GOOD) "100%" else healthStatus.name,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun SparklineGraph(
    values: List<Float>,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gradientTop = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    val gradientBottom = MaterialTheme.colorScheme.primary.copy(alpha = 0.0f)
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val minValue = values.min()
        val maxValue = values.max()
        val hasRange = maxValue > minValue
        val range = if (hasRange) maxValue - minValue else 1f
        val stepX = size.width / (values.size - 1)
        val points = values.mapIndexed { index, value ->
            val x = index * stepX
            val y = if (hasRange) {
                val normalized = (value - minValue) / range
                size.height - (normalized * size.height * 0.7f) - (size.height * 0.1f)
            } else {
                size.height / 2f
            }
            Offset(x, y)
        }
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
        }
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(gradientTop, gradientBottom),
                startY = 0f,
                endY = size.height
            )
        )
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun GraphMetricCardContent(
    iconRes: Int,
    title: String,
    currentValueText: String,
    minText: String,
    maxText: String,
    history: List<Float>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (history.size >= 2) {
            SparklineGraph(
                values = history,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .align(Alignment.BottomCenter)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = currentValueText,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$minText • $maxText",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CycleCountCardContent(
    cycleCountText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_refresh_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(R.string.cycle_count),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = cycleCountText,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(R.string.total),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun BatteryLevelIndicator(
    levelPercent: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = levelPercent.coerceIn(0, 100) / 100f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "battery_level_progress"
    )
    val animatedAmplitude by animateFloatAsState(
        targetValue = if (isCharging) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "wave_amplitude"
    )
    Box(
        modifier = modifier.size(150.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularWavyProgressIndicator(
            progress = { animatedProgress },
            amplitude = { animatedAmplitude },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
            text = "$levelPercent%",
            fontFamily = FontFamily(Font(R.font.google_sans_flex)),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BatteryHeroCard(
    levelPercent: Int,
    isCharging: Boolean,
    statusText: String,
    chargeTimeRemaining: String?,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isCharging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isCharging) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(if (isCharging) R.drawable.ic_charger_24dp else R.drawable.ic_battery_full_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isCharging) statusText else stringResource(R.string.discharging),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            BatteryLevelIndicator(
                levelPercent = levelPercent,
                isCharging = isCharging
            )
            if (isCharging && chargeTimeRemaining != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = chargeTimeRemaining,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun BatteryInfoScreen(
    modifier: Modifier = Modifier,
    viewModel: BatteryInfoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bottomSpacing = LocalBottomSpacing.current
    val minCardSize = 150.dp
    val wideRowMinWidth = 850.dp
    val gridItems = listOf(
        GridCardKind.HEALTH,
        GridCardKind.TEMPERATURE,
        GridCardKind.VOLTAGE,
        GridCardKind.CYCLE_COUNT
    )
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val availableWidth = maxWidth - 32.dp
        val columnsCount = max(2, (availableWidth / minCardSize).toInt())
        val isNarrow = columnsCount == 2
        val tier = when {
            isNarrow -> GridLayoutTier.NARROW
            maxWidth >= wideRowMinWidth -> GridLayoutTier.WIDE_ROW
            else -> GridLayoutTier.WIDE_STACK
        }
        val gridColumns = when (tier) {
            GridLayoutTier.NARROW -> columnsCount
            GridLayoutTier.WIDE_STACK -> 6
            GridLayoutTier.WIDE_ROW -> 12
        }
        val cardSpacing = 8.dp
        val unitWidth = (availableWidth - cardSpacing * (gridColumns - 1)) / gridColumns
        val isCharging = uiState.displayMode == BatteryDisplayMode.CHARGING

        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 0.dp, end = 0.dp,
                top = 16.dp, bottom = 16.dp + bottomSpacing
            ),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BatteryHeroCard(
                    levelPercent = uiState.levelPercent,
                    isCharging = isCharging,
                    statusText = uiState.status,
                    chargeTimeRemaining = uiState.chargeTimeRemaining,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)
                )
            }

            itemsIndexed(
                items = gridItems,
                span = { _, kind ->
                    when (tier) {
                        GridLayoutTier.NARROW -> GridItemSpan(1)
                        GridLayoutTier.WIDE_STACK, GridLayoutTier.WIDE_ROW -> when (kind) {
                            GridCardKind.HEALTH, GridCardKind.CYCLE_COUNT -> GridItemSpan(2)
                            GridCardKind.TEMPERATURE, GridCardKind.VOLTAGE -> GridItemSpan(4)
                        }
                    }
                }
            ) { index, kind ->
                val cardShape = RoundedCornerShape(20.dp)
                val paddingModifier = when (tier) {
                    GridLayoutTier.NARROW, GridLayoutTier.WIDE_STACK -> {
                        if (index % 2 == 0) Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                        else Modifier.padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
                    }
                    GridLayoutTier.WIDE_ROW -> {
                        when (index) {
                            0 -> Modifier.padding(start = 16.dp, end = 4.dp)
                            3 -> Modifier.padding(start = 4.dp, end = 16.dp)
                            else -> Modifier.padding(horizontal = 4.dp)
                        }
                    }
                }
                val cardModifier = if (tier == GridLayoutTier.NARROW) {
                    paddingModifier.fillMaxWidth().aspectRatio(1f)
                } else {
                    val cardHeight = unitWidth * 2 + cardSpacing
                    paddingModifier.fillMaxWidth().height(cardHeight)
                }
                Card(
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = cardModifier
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (kind) {
                            GridCardKind.HEALTH -> HealthCardContent(
                                healthStatus = uiState.healthStatus,
                                healthText = uiState.health
                            )
                            GridCardKind.TEMPERATURE -> {
                                val tempUnit = uiState.temperatureUnit
                                val tempValues = uiState.temperatureHistory.map { convertTemperature(it.temperatureCelsius, tempUnit) }
                                val currentTempStr = uiState.temperatureHistory.lastOrNull()
                                    ?.temperatureCelsius
                                    ?.let { formatTemperatureFloat(it, tempUnit) }
                                    ?: uiState.temperature
                                GraphMetricCardContent(
                                    iconRes = R.drawable.ic_offline_bolt_24dp,
                                    title = stringResource(R.string.temperature),
                                    currentValueText = currentTempStr,
                                    minText = uiState.temperatureMin?.let { stringResource(R.string.min_value, formatTemperatureFloat(it, tempUnit)) } ?: "...",
                                    maxText = uiState.temperatureMax?.let { stringResource(R.string.max_value, formatTemperatureFloat(it, tempUnit)) } ?: "...",
                                    history = tempValues
                                )
                            }
                            GridCardKind.VOLTAGE -> {
                                val voltValues = uiState.voltageHistory.map { it.voltageVolts }
                                val currentVoltStr = uiState.voltageHistory.lastOrNull()
                                    ?.voltageVolts
                                    ?.let { String.format(Locale.getDefault(), "%.2f V", it) }
                                    ?: uiState.voltage
                                GraphMetricCardContent(
                                    iconRes = R.drawable.ic_flash_on_24dp,
                                    title = stringResource(R.string.voltage),
                                    currentValueText = currentVoltStr,
                                    minText = uiState.voltageMin?.let { stringResource(R.string.min_value_decimal, it) } ?: "...",
                                    maxText = uiState.voltageMax?.let { stringResource(R.string.max_value_decimal, it) } ?: "...",
                                    history = voltValues
                                )
                            }
                            GridCardKind.CYCLE_COUNT -> CycleCountCardContent(
                                cycleCountText = uiState.cycleCount
                            )
                        }
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(4.dp))
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                val summaryCapacity = if (uiState.isLoadingCapacity) stringResource(R.string.loading) else uiState.capacity
                CustomCardItem(
                    title = stringResource(R.string.capacity),
                    summary = summaryCapacity,
                    icon = R.drawable.ic_storage_24dp,
                    position = CardPosition.TOP
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                CustomCardItem(
                    title = stringResource(R.string.technology),
                    summary = uiState.technology,
                    icon = R.drawable.ic_build_24dp,
                    position = CardPosition.MIDDLE
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                val hasChargeTime = uiState.chargeTimeRemaining != null
                CustomCardItem(
                    title = stringResource(R.string.battery_current),
                    summary = uiState.current,
                    icon = R.drawable.ic_power_new_24dp,
                    position = if (hasChargeTime) CardPosition.MIDDLE else CardPosition.BOTTOM
                )
            }
            uiState.chargeTimeRemaining?.let { remaining ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CustomCardItem(
                        title = stringResource(R.string.time_to_full_charge),
                        summary = remaining,
                        icon = R.drawable.ic_schedule_24dp,
                        position = CardPosition.BOTTOM
                    )
                }
            }
        }
    }
}
