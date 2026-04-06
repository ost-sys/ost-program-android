package com.ost.application.ui.screen.converters.timezone

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.ost.application.ui.component.TimePickerDialog
import com.ost.application.util.CardPosition
import com.ost.application.util.CustomCardItem
import com.ost.application.util.SectionTitle
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeZoneConverterPage(
    modifier: Modifier = Modifier,
    viewModel: TimeZoneConverterViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bottomSpacing = LocalBottomSpacing.current
    val haptic = LocalHapticFeedback.current

    var showTimePicker by remember { mutableStateOf(false) }
    var showSourceZonePicker by remember { mutableStateOf(false) }
    var showTargetZonePicker by remember { mutableStateOf(false) }

    val timePickerState = rememberTimePickerState(
        initialHour = uiState.selectedTime.hour,
        initialMinute = uiState.selectedTime.minute,
        is24Hour = true
    )

    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp, bottom = 16.dp + bottomSpacing),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 5.dp)
                ) {
                    ExpressiveShapeBackground(
                        iconSize = 120.dp,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        forcedShape = ExpressiveShapeType.COOKIE_9,
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                    )

                    Image(
                        painter = painterResource(id = R.drawable.ic_browse_gallery_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = uiState.resultText ?: stringResource(R.string.result),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                    minLines = 2
                )
            }

            SectionTitle(stringResource(R.string.first_time_zone))

            CustomCardItem(
                title = stringResource(R.string.time_zone),
                summary = uiState.sourceTimeZoneId,
                position = CardPosition.SINGLE,
                icon = R.drawable.ic_public_24dp,
                onClick = { showSourceZonePicker = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SectionTitle(stringResource(R.string.second_time_zone))

            CustomCardItem(
                title = stringResource(R.string.time_zone),
                summary = uiState.targetTimeZoneId,
                position = CardPosition.TOP,
                icon = R.drawable.ic_public_24dp,
                onClick = { showTargetZonePicker = true }
            )

            CustomCardItem(
                title = stringResource(R.string.time),
                summary = uiState.selectedTime.format(timeFormatter),
                position = CardPosition.BOTTOM,
                icon = R.drawable.ic_schedule_24dp,
                onClick = { showTimePicker = true }
            )
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setSelectedTime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(android.R.string.cancel)) }
            }
        ) {
            TimePicker(state = timePickerState, modifier = Modifier.padding(16.dp))
        }
    }

    if (showSourceZonePicker) {
        TimeZoneSelectionDialog(
            title = stringResource(R.string.first_time_zone),
            zones = uiState.timeZones,
            onDismiss = { showSourceZonePicker = false },
            onZoneSelected = { zone ->
                viewModel.setSourceTimeZone(zone)
                showSourceZonePicker = false
            }
        )
    }

    if (showTargetZonePicker) {
        TimeZoneSelectionDialog(
            title = stringResource(R.string.second_time_zone),
            zones = uiState.timeZones,
            onDismiss = { showTargetZonePicker = false},
            onZoneSelected = { zone ->
                viewModel.setTargetTimeZone(zone)
                showTargetZonePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeZoneSelectionDialog(
    title: String,
    zones: List<String>,
    onDismiss: () -> Unit,
    onZoneSelected: (String) -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(24.dp)
                )
                HorizontalDivider()
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(zones) { zone ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onZoneSelected(zone) }
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = zone,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            }
        }
    }
}