package com.ost.application.ui.screen.converters.timecalc

import android.icu.util.Calendar
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeCalculatorPage(
    modifier: Modifier = Modifier,
    viewModel: TimeCalculatorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bottomSpacing = LocalBottomSpacing.current
    val haptic = LocalHapticFeedback.current

    var showDatePicker1 by remember { mutableStateOf(false) }
    var showTimePicker1 by remember { mutableStateOf(false) }
    var showDatePicker2 by remember { mutableStateOf(false) }
    var showTimePicker2 by remember { mutableStateOf(false) }

    val datePickerState1 = rememberDatePickerState(
        initialSelectedDateMillis = uiState.firstDateTimeMillis,
        yearRange = 1900..Calendar.getInstance().get(Calendar.YEAR) + 100
    )
    val datePickerState2 = rememberDatePickerState(
        initialSelectedDateMillis = uiState.secondDateTimeMillis,
        yearRange = 1900..Calendar.getInstance().get(Calendar.YEAR) + 100
    )

    val calendar1 =
        remember { Calendar.getInstance() }.apply { timeInMillis = uiState.firstDateTimeMillis }
    val calendar2 =
        remember { Calendar.getInstance() }.apply { timeInMillis = uiState.secondDateTimeMillis }

    val timePickerState1 = rememberTimePickerState(
        initialHour = calendar1.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar1.get(Calendar.MINUTE),
        is24Hour = true
    )
    val timePickerState2 = rememberTimePickerState(
        initialHour = calendar2.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar2.get(Calendar.MINUTE),
        is24Hour = true
    )

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp, bottom = bottomSpacing + 88.dp),
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
                        forcedShape = ExpressiveShapeType.ARCH,
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                    )

                    Image(
                        painter = painterResource(id = R.drawable.ic_schedule_24dp),
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

            SectionTitle(stringResource(R.string.minuend))

            CustomCardItem(
                title = stringResource(R.string.date),
                summary = dateFormatter.format(Date(uiState.firstDateTimeMillis)),
                icon = R.drawable.ic_calendar_today_24dp,
                position = CardPosition.TOP,
                onClick = { showDatePicker1 = true }
            )

            CustomCardItem(
                title = stringResource(R.string.time),
                summary = timeFormatter.format(Date(uiState.firstDateTimeMillis)),
                icon = R.drawable.ic_schedule_24dp,
                position = CardPosition.BOTTOM,
                onClick = { showTimePicker1 = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SectionTitle(stringResource(R.string.subtrahend))

            CustomCardItem(
                title = stringResource(R.string.date),
                summary = dateFormatter.format(Date(uiState.secondDateTimeMillis)),
                icon = R.drawable.ic_calendar_today_24dp,
                position = CardPosition.TOP,
                onClick = { showDatePicker2 = true }
            )

            CustomCardItem(
                title = stringResource(R.string.time),
                summary = timeFormatter.format(Date(uiState.secondDateTimeMillis)),
                icon = R.drawable.ic_schedule_24dp,
                position = CardPosition.BOTTOM,
                onClick = { showTimePicker2 = true }
            )
        }
    }

    if (showDatePicker1) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker1 = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState1.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance().apply { timeInMillis = millis }
                        viewModel.updateFirstDate(
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        )
                    }
                    showDatePicker1 = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker1 = false }) {
                    Text(
                        stringResource(android.R.string.cancel)
                    )
                }
            }
        ) { DatePicker(state = datePickerState1) }
    }

    if (showTimePicker1) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker1 = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateFirstTime(timePickerState1.hour, timePickerState1.minute)
                    showTimePicker1 = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker1 = false }) {
                    Text(
                        stringResource(android.R.string.cancel)
                    )
                }
            }
        ) { TimePicker(state = timePickerState1, modifier = Modifier.padding(16.dp)) }
    }

    if (showDatePicker2) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker2 = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState2.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance().apply { timeInMillis = millis }
                        viewModel.updateSecondDate(
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        )
                    }
                    showDatePicker2 = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker2 = false }) {
                    Text(
                        stringResource(android.R.string.cancel)
                    )
                }
            }
        ) { DatePicker(state = datePickerState2) }
    }

    if (showTimePicker2) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker2 = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateSecondTime(timePickerState2.hour, timePickerState2.minute)
                    showTimePicker2 = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker2 = false }) {
                    Text(
                        stringResource(android.R.string.cancel)
                    )
                }
            }
        ) { TimePicker(state = timePickerState2, modifier = Modifier.padding(16.dp)) }
    }
}