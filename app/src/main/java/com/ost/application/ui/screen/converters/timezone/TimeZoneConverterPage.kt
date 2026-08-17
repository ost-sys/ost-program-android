package com.ost.application.ui.screen.converters.timezone
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.LocalBottomSpacing
import com.ost.application.R
import com.ost.application.ui.components.ExpressiveShapeBackground
import com.ost.application.ui.components.ExpressiveShapeType
import com.ost.application.ui.components.SectionTitle
import com.ost.application.ui.components.TimePickerDialog
import com.ost.application.util.CardPosition
import com.ost.application.util.CustomCardItem
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimeZoneConverterPage(
    modifier: Modifier = Modifier,
    viewModel: TimeZoneConverterViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bottomSpacing = LocalBottomSpacing.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var showTimePicker by remember { mutableStateOf(false) }
    val sourceSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val targetSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSourceSheet  by remember { mutableStateOf(false) }
    var showTargetSheet  by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = uiState.selectedTime.hour,
        initialMinute = uiState.selectedTime.minute,
        is24Hour = true
    )
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()) }
    val hasResult = !uiState.resultText.isNullOrEmpty()
    val resultCardColor by animateColorAsState(
        targetValue = if (hasResult) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(400), label = "resultCardColor"
    )
    val resultTextColor by animateColorAsState(
        targetValue = if (hasResult) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(400), label = "resultTextColor"
    )
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp, bottom = 16.dp + bottomSpacing),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
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
                Spacer(Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = resultCardColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AnimatedContent(
                            targetState = uiState.resultText ?: stringResource(R.string.result),
                            transitionSpec = {
                                (slideInVertically { -it } + fadeIn(tween(300)))
                                    .togetherWith(slideOutVertically { it } + fadeOut(tween(200)))
                            },
                            label = "resultText"
                        ) { text ->
                            Text(
                                text = text,
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                                color = resultTextColor
                            )
                        }
                        AnimatedContent(
                            targetState = uiState.offsetDiff,
                            transitionSpec = { fadeIn(tween(300)).togetherWith(fadeOut(tween(200))) },
                            label = "offsetDiff"
                        ) { diff ->
                            if (diff != null) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ) {
                                    Text(
                                        text = diff,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            SectionTitle(stringResource(R.string.first_time_zone))
            CustomCardItem(
                title   = stringResource(R.string.time_zone),
                summary = uiState.sourceTimeZoneId,
                position = CardPosition.TOP,
                icon    = R.drawable.ic_public_24dp,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showSourceSheet = true
                }
            )
            CustomCardItem(
                title   = stringResource(R.string.time),
                summary = uiState.selectedTime.format(timeFormatter),
                position = CardPosition.BOTTOM,
                icon    = R.drawable.ic_schedule_24dp,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showTimePicker = true
                }
            )
            Spacer(Modifier.height(8.dp))
            SectionTitle(stringResource(R.string.second_time_zone))
            CustomCardItem(
                title   = stringResource(R.string.time_zone),
                summary = uiState.targetTimeZoneId,
                position = CardPosition.SINGLE,
                icon    = R.drawable.ic_public_24dp,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showTargetSheet = true
                }
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
        ) { TimePicker(state = timePickerState, modifier = Modifier.padding(16.dp)) }
    }
    if (showSourceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSourceSheet = false },
            sheetState = sourceSheetState
        ) {
            TimeZoneSheetContent(
                title        = stringResource(R.string.first_time_zone),
                groupedZones = uiState.groupedZones,
                searchQuery  = uiState.searchQuery,
                onSearchChange = { viewModel.setSearchQuery(it) },
                onZoneSelected = { zone ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.setSourceTimeZone(zone)
                    scope.launch { sourceSheetState.hide() }.invokeOnCompletion { showSourceSheet = false }
                }
            )
        }
    }
    if (showTargetSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTargetSheet = false },
            sheetState = targetSheetState
        ) {
            TimeZoneSheetContent(
                title        = stringResource(R.string.second_time_zone),
                groupedZones = uiState.groupedZones,
                searchQuery  = uiState.searchQuery,
                onSearchChange = { viewModel.setSearchQuery(it) },
                onZoneSelected = { zone ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.setTargetTimeZone(zone)
                    scope.launch { targetSheetState.hide() }.invokeOnCompletion { showTargetSheet = false }
                }
            )
        }
    }
}
@Composable
private fun TimeZoneSheetContent(
    title: String,
    groupedZones: Map<String, List<String>>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onZoneSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
        )
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text(stringResource(R.string.search)) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(50)
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            groupedZones.forEach { (region, zones) ->
                item(key = "header_$region") {
                    SectionTitle(region)
                }
                itemsIndexed(items = zones, key = { _, zone -> zone }) { index, zone ->
                    val position = when {
                        zones.size == 1 -> CardPosition.SINGLE
                        index == 0 -> CardPosition.TOP
                        index == zones.lastIndex -> CardPosition.BOTTOM
                        else -> CardPosition.MIDDLE
                    }
                    CustomCardItem(
                        title = zone.substringAfter("/").replace("_", " "),
                        summary = zone,
                        position = position,
                        status = true,
                        onClick = { onZoneSelected(zone) }
                    )
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
