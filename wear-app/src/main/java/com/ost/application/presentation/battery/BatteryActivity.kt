package com.ost.application.presentation.battery
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.TimeText
import com.ost.application.R
import com.ost.application.component.ExpressiveShapeType
import com.ost.application.core.battery.BatteryDisplayMode
import com.ost.application.theme.OSTToolsTheme
import com.ost.application.util.CardPosition
import com.ost.application.util.InfoListScreenContent
import com.ost.application.util.ListItem
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
class BatteryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSTToolsTheme {
                BatteryScreen()
            }
        }
    }
}
@Composable
fun BatteryScreen(viewModel: WearBatteryInfoViewModel = viewModel()) {
    val listState = rememberScalingLazyListState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val infiniteTransition = rememberInfiniteTransition(label = "infinite rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 10000, easing = LinearEasing)),
        label = "rotation"
    )
    val currentRotation = if (uiState.displayMode == BatteryDisplayMode.CHARGING) rotation else 0f
    var chargingShape by remember { mutableStateOf(ExpressiveShapeType.COOKIE_9) }
    LaunchedEffect(uiState.displayMode) {
        if (uiState.displayMode == BatteryDisplayMode.CHARGING) {
            while (true) {
                delay(1000.milliseconds)
                var newShape = ExpressiveShapeType.entries.random()
                while (newShape == chargingShape) newShape = ExpressiveShapeType.entries.random()
                chargingShape = newShape
            }
        }
    }
    val targetShape = when (uiState.displayMode) {
        BatteryDisplayMode.CHARGING -> chargingShape
        BatteryDisplayMode.POWER_SAVE -> ExpressiveShapeType.CLOVER_4
        BatteryDisplayMode.NORMAL -> ExpressiveShapeType.SQUARE
    }
    val (headerBg, headerIconTint) = when (uiState.displayMode) {
        BatteryDisplayMode.CHARGING -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        BatteryDisplayMode.POWER_SAVE -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        BatteryDisplayMode.NORMAL -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    AppScaffold(timeText = { TimeText() }) {
        ScreenScaffold(
            scrollState = listState,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        ) {
            InfoListScreenContent(
                listState = listState,
                screenTitle = uiState.levelText,
                icon = uiState.iconResId,
                items = buildBatteryItems(uiState),
                headerShape = targetShape,
                headerRotationDegrees = currentRotation,
                headerBackgroundColor = headerBg,
                headerIconTint = headerIconTint
            )
        }
    }
}
@Composable
private fun buildBatteryItems(uiState: WearBatteryInfoUiState): List<ListItem> = listOf(
    ListItem(stringResource(R.string.health), uiState.health, null, true, CardPosition.TOP, null),
    ListItem(stringResource(R.string.status), uiState.status, null, true, CardPosition.MIDDLE, null),
    ListItem(stringResource(R.string.temperature), uiState.temperature, null, true, CardPosition.MIDDLE, null),
    ListItem(stringResource(R.string.voltage), uiState.voltage, null, true, CardPosition.MIDDLE, null),
    ListItem(stringResource(R.string.technology), uiState.technology, null, true, CardPosition.MIDDLE, null),
    ListItem(stringResource(R.string.capacity), if (uiState.isLoadingCapacity) "..." else uiState.capacity, null, true, CardPosition.MIDDLE, null),
    ListItem(stringResource(R.string.cycle_count), uiState.cycleCount, null, true, CardPosition.MIDDLE, null),
    ListItem(stringResource(R.string.battery_current), uiState.current, null, true,
        if (uiState.chargeTimeRemaining != null) CardPosition.MIDDLE else CardPosition.BOTTOM, null)
) + listOfNotNull(
    uiState.chargeTimeRemaining?.let {
        ListItem(stringResource(R.string.time_to_full_charge), it, null, true, CardPosition.BOTTOM, null)
    }
)
@Preview(device = "id:wearos_small_round")
@Composable
fun BatteryPreview() {
    BatteryScreen()
}
