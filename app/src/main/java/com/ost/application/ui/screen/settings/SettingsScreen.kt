package com.ost.application.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ost.application.R

@Composable
fun SettingsListContent(
    state: SettingsUiState,
    onTotalDurationChange: (Float) -> Unit,
    onNoiseDurationChange: (Float) -> Unit,
    onBWNoiseDurationChange: (Float) -> Unit,
    onHorizontalDurationChange: (Float) -> Unit,
    onVerticalDurationChange: (Float) -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .padding(bottom = 8.dp)
    ) {
        item {
            SeekBarPreference(
                title = stringResource(R.string.total_recovery_time),
                summary = stringResource(R.string.recovery_time),
                value = state.totalDuration,
                range = 1f..60f,
                steps = 58,
                onValueChange = onTotalDurationChange
            )
        }
        item {
            SeekBarPreference(
                title = stringResource(R.string.noise),
                summary = stringResource(R.string.noise_display_time),
                value = state.noiseDuration,
                range = 1f..10f,
                steps = 8,
                onValueChange = onNoiseDurationChange
            )
        }
        item {
            SeekBarPreference(
                title = stringResource(R.string.black_white_noise),
                summary = stringResource(R.string.black_white_noise_display_time),
                value = state.blackWhiteNoiseDuration,
                range = 1f..10f,
                steps = 8,
                onValueChange = onBWNoiseDurationChange
            )
        }
        item {
            SeekBarPreference(
                title = stringResource(R.string.horizontal_lines),
                summary = stringResource(R.string.horizontal_lines_display_time),
                value = state.horizontalDuration,
                range = 1f..10f,
                steps = 8,
                onValueChange = onHorizontalDurationChange
            )
        }
        item {
            SeekBarPreference(
                title = stringResource(R.string.vertical_lines),
                summary = stringResource(R.string.vertical_lines_display_time),
                value = state.verticalDuration,
                range = 1f..10f,
                steps = 8,
                onValueChange = onVerticalDurationChange
            )
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun SeekBarPreference(
    title: String,
    summary: String? = null,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (summary != null) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Top)
            )
        }
        Spacer(Modifier.height(8.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}