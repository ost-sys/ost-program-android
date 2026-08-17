package com.ost.application.presentation
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Slider
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.ost.application.R
import com.ost.application.core.settings.TemperatureUnit
import com.ost.application.core.settings.sync.WearSyncState
import com.ost.application.settings.WearSettingsViewModel
import com.ost.application.theme.OSTToolsTheme
import com.ost.application.util.CardListItem
import com.ost.application.util.CardPosition
import com.ost.application.util.WavyDivider
import com.ost.application.util.startActivity
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSTToolsTheme {
                SettingsScreen()
            }
        }
    }
}
@Composable
private fun CenteredWavyDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        WavyDivider()
    }
}
@Composable
fun SettingsScreen(viewModel: WearSettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()
    val uiState by viewModel.uiState.collectAsState()
    val tempOptions = remember {
        listOf(
            TemperatureUnit.SYSTEM to ("System default" to CardPosition.TOP),
            TemperatureUnit.CELSIUS to ("Celsius (°C)" to CardPosition.MIDDLE),
            TemperatureUnit.FAHRENHEIT to ("Fahrenheit (°F)" to CardPosition.BOTTOM)
        )
    }
    AppScaffold(timeText = { TimeText() }) {
        ScreenScaffold(scrollState = listState) {
            ScalingLazyColumn(
                state = listState,
                anchorType = ScalingLazyListAnchorType.ItemCenter
            ) {
                item { ListHeader { Text("Settings") } }
                item {
                    CardListItem(
                        title = "Language",
                        summary = uiState.currentLocale.getDisplayName(uiState.currentLocale)
                            .replaceFirstChar { it.titlecase(uiState.currentLocale) },
                        icon = R.drawable.ic_language_24dp,
                        status = false,
                        position = CardPosition.SINGLE,
                        onClick = {}
                    )
                }
                item { CenteredWavyDivider() }
                val slidersEnabled = uiState.syncState != WearSyncState.Enabled
                if (slidersEnabled) {
                    item { Text("Total recovery time") }
                    item {
                        Slider(
                            value = uiState.timing.totalDuration,
                            onValueChange = viewModel::onTotalDurationChange,
                            valueProgression = 1..121,
                            enabled = slidersEnabled
                        )
                    }
                    item { Text("Noise") }
                    item {
                        Slider(
                            value = uiState.timing.noiseDuration,
                            onValueChange = viewModel::onNoiseDurationChange,
                            valueProgression = 1..10,
                            enabled = slidersEnabled
                        )
                    }
                    item { Text("Black & white noise") }
                    item {
                        Slider(
                            value = uiState.timing.blackWhiteNoiseDuration,
                            onValueChange = viewModel::onBWNoiseDurationChange,
                            valueProgression = 1..10,
                            enabled = slidersEnabled
                        )
                    }
                    item { Text("Horizontal lines") }
                    item {
                        Slider(
                            value = uiState.timing.horizontalDuration,
                            onValueChange = viewModel::onHorizontalDurationChange,
                            valueProgression = 1..10,
                            enabled = slidersEnabled
                        )
                    }
                    item { Text("Vertical lines") }
                    item {
                        Slider(
                            value = uiState.timing.verticalDuration,
                            onValueChange = viewModel::onVerticalDurationChange,
                            valueProgression = 1..10,
                            enabled = slidersEnabled
                        )
                    }
                }
                item { CenteredWavyDivider() }
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
                            selected = uiState.temperatureUnit == unit,
                            onSelect = { viewModel.onTemperatureUnitChange(unit) },
                            enabled = true,
                            label = { Text(label, fontWeight = FontWeight.Medium) },
                            shape = shape,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                if (uiState.phoneConnected) {
                    item { CenteredWavyDivider() }
                    item { ListHeader { Text("GitHub integration") } }
                    item {
                        if (uiState.githubTokenFound) {
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
                                onClick = { viewModel.requestOpenSettingsOnPhone() }
                            )
                        }
                    }
                }
                item { CenteredWavyDivider() }
                item {
                    CardListItem(
                        title = context.getString(R.string.shortcuts),
                        summary = null,
                        icon = R.drawable.ic_apps_24dp,
                        status = true,
                        position = CardPosition.SINGLE,
                        onClick = { startActivity(context, ShortcutsActivity::class.java) }
                    )
                }
                item {
                    CardListItem(
                        title = "Check Updates",
                        summary = null,
                        icon = R.drawable.ic_update_24dp,
                        status = true,
                        position = CardPosition.SINGLE,
                        onClick = { startActivity(context, UpdateActivity::class.java) }
                    )
                }
                item {
                    CardListItem(
                        title = "About",
                        summary = null,
                        icon = R.drawable.ic_info_24dp,
                        status = true,
                        position = CardPosition.SINGLE,
                        onClick = { startActivity(context, AboutActivity::class.java) }
                    )
                }
            }
        }
    }
}
