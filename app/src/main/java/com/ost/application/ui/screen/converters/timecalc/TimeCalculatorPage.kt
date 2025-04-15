package com.ost.application.ui.screen.converters.timecalc

import android.icu.util.Calendar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.R
import com.ost.application.utils.SectionTitle
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

    val calendar1 = remember { Calendar.getInstance() }.apply { timeInMillis = uiState.firstDateTimeMillis }
    val calendar2 = remember { Calendar.getInstance() }.apply { timeInMillis = uiState.secondDateTimeMillis }
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

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_schedule_24dp),
                    contentDescription = stringResource(R.string.time),
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.resultText ?: stringResource(R.string.result),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary,
                    minLines = 2
                )
            }

            SectionTitle(stringResource(R.string.minuend))
            DateTimePickerRow(
                label = stringResource(R.string.date),
                value = dateFormatter.format(Date(uiState.firstDateTimeMillis)),
                onClick = { showDatePicker1 = true }
            )
            DateTimePickerRow(
                label = stringResource(R.string.time),
                value = timeFormatter.format(Date(uiState.firstDateTimeMillis)),
                onClick = { showTimePicker1 = true }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            SectionTitle(stringResource(R.string.subtrahend))
            DateTimePickerRow(
                label = stringResource(R.string.date),
                value = dateFormatter.format(Date(uiState.secondDateTimeMillis)),
                onClick = { showDatePicker2 = true }
            )
            DateTimePickerRow(
                label = stringResource(R.string.time),
                value = timeFormatter.format(Date(uiState.secondDateTimeMillis)),
                onClick = { showTimePicker2 = true }
            )
        }

        FloatingActionButton(
            onClick = viewModel::calculateTimeDifference,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(painterResource(id = R.drawable.ic_calculate_24dp), contentDescription = stringResource(R.string.calculate))
        }
    }

    if (showDatePicker1) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker1 = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker1 = false
                    datePickerState1.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance().apply { timeInMillis = millis }
                        viewModel.updateFirstDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                    }
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker1 = false }) { Text(stringResource(android.R.string.cancel)) }
            }
        ) {
            DatePicker(state = datePickerState1)
        }
    }

    if (showTimePicker1) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker1 = false },
            confirmButton = {
                TextButton(onClick = {
                    showTimePicker1 = false
                    viewModel.updateFirstTime(timePickerState1.hour, timePickerState1.minute)
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker1 = false }) { Text(stringResource(android.R.string.cancel)) }
            }
        ) {
            TimePicker(state = timePickerState1, modifier = Modifier.padding(16.dp))
        }
    }

    if (showDatePicker2) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker2 = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker2 = false
                    datePickerState2.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance().apply { timeInMillis = millis }
                        viewModel.updateSecondDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                    }
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker2 = false }) { Text(stringResource(android.R.string.cancel)) }
            }
        ) {
            DatePicker(state = datePickerState2)
        }
    }

    if (showTimePicker2) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker2 = false },
            confirmButton = {
                TextButton(onClick = {
                    showTimePicker2 = false
                    viewModel.updateSecondTime(timePickerState2.hour, timePickerState2.minute)
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker2 = false }) { Text(stringResource(android.R.string.cancel)) }
            }
        ) {
            TimePicker(state = timePickerState2, modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
fun DateTimePickerRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
    ) {
        Surface(
            shape = MaterialTheme.shapes.large
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(24.dp))
                content()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    dismissButton()
                    Spacer(modifier = Modifier.width(8.dp))
                    confirmButton()
                }
            }
        }
    }
}