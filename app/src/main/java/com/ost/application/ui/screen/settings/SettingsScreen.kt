package com.ost.application.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ost.application.R
import com.ost.application.ui.component.ExpressiveShapeBackground
import com.ost.application.ui.component.LanguagePickerDialog
import com.ost.application.util.CardPosition
import com.ost.application.util.SectionTitle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun getShapeForPosition(position: CardPosition): Shape {
    val large = 24.dp
    val small = 4.dp

    return when (position) {
        CardPosition.TOP -> RoundedCornerShape(topStart = large, topEnd = large, bottomStart = small, bottomEnd = small)
        CardPosition.MIDDLE -> RoundedCornerShape(small)
        CardPosition.BOTTOM -> RoundedCornerShape(topStart = small, topEnd = small, bottomStart = large, bottomEnd = large)
        CardPosition.SINGLE -> RoundedCornerShape(large)
    }
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onTotalDurationChange: (Float) -> Unit,
    onNoiseDurationChange: (Float) -> Unit,
    onBWNoiseDurationChange: (Float) -> Unit,
    onHorizontalDurationChange: (Float) -> Unit,
    onVerticalDurationChange: (Float) -> Unit,
    onGithubTokenChange: (String) -> Unit,
    onSaveGithubToken: () -> Unit,
    onClearGithubToken: () -> Unit,
    onLanguagePreferenceClick: () -> Unit,
    onLanguageSelected: (Locale?) -> Unit,
    onLanguageConfirm: () -> Unit,
    onLanguageDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLanguageDialogVisible) {
        LanguagePickerDialog(
            supportedLocales = state.supportedLocales,
            selectedLocale = state.selectedLanguageInDialog,
            onLanguageSelected = onLanguageSelected,
            onConfirm = onLanguageConfirm,
            onDismiss = onLanguageDismiss
        )
    }

    SettingsListContent(
        state = state,
        onTotalDurationChange = onTotalDurationChange,
        onNoiseDurationChange = onNoiseDurationChange,
        onBWNoiseDurationChange = onBWNoiseDurationChange,
        onHorizontalDurationChange = onHorizontalDurationChange,
        onVerticalDurationChange = onVerticalDurationChange,
        onGithubTokenChange = onGithubTokenChange,
        onSaveGithubToken = onSaveGithubToken,
        onClearGithubToken = onClearGithubToken,
        onLanguagePreferenceClick = onLanguagePreferenceClick,
        modifier = modifier
    )
}

@Composable
fun SettingsListContent(
    state: SettingsUiState,
    onTotalDurationChange: (Float) -> Unit,
    onNoiseDurationChange: (Float) -> Unit,
    onBWNoiseDurationChange: (Float) -> Unit,
    onHorizontalDurationChange: (Float) -> Unit,
    onVerticalDurationChange: (Float) -> Unit,
    onGithubTokenChange: (String) -> Unit,
    onSaveGithubToken: () -> Unit,
    onClearGithubToken: () -> Unit,
    onLanguagePreferenceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(bottom = 8.dp)
    ) {
        item {
            SectionTitle(
                title = stringResource(R.string.category_general),
            )
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SettingsItem(
                    title = stringResource(R.string.language),
                    summary = state.currentAppliedLocale.getDisplayName(state.currentAppliedLocale)
                        .replaceFirstChar { it.titlecase(state.currentAppliedLocale) },
                    icon = R.drawable.ic_public_24dp,
                    onClick = onLanguagePreferenceClick,
                    position = CardPosition.SINGLE
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle(
                title = stringResource(R.string.category_timings),
            )
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SliderSettingsItem(
                    title = stringResource(R.string.total_recovery_time),
                    summary = stringResource(R.string.recovery_time),
                    value = state.totalDuration,
                    range = 1f..120f,
                    steps = 58,
                    onValueChange = onTotalDurationChange,
                    position = CardPosition.TOP
                )
                SliderSettingsItem(
                    title = stringResource(R.string.noise),
                    summary = stringResource(R.string.noise_display_time),
                    value = state.noiseDuration,
                    range = 1f..10f,
                    steps = 8,
                    onValueChange = onNoiseDurationChange,
                    position = CardPosition.MIDDLE
                )
                SliderSettingsItem(
                    title = stringResource(R.string.black_white_noise),
                    summary = stringResource(R.string.black_white_noise_display_time),
                    value = state.blackWhiteNoiseDuration,
                    range = 1f..10f,
                    steps = 8,
                    onValueChange = onBWNoiseDurationChange,
                    position = CardPosition.MIDDLE
                )
                SliderSettingsItem(
                    title = stringResource(R.string.horizontal_lines),
                    summary = stringResource(R.string.horizontal_lines_display_time),
                    value = state.horizontalDuration,
                    range = 1f..10f,
                    steps = 8,
                    onValueChange = onHorizontalDurationChange,
                    position = CardPosition.MIDDLE
                )
                SliderSettingsItem(
                    title = stringResource(R.string.vertical_lines),
                    summary = stringResource(R.string.vertical_lines_display_time),
                    value = state.verticalDuration,
                    range = 1f..10f,
                    steps = 8,
                    onValueChange = onVerticalDurationChange,
                    position = CardPosition.BOTTOM
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            SectionTitle(
                title = stringResource(R.string.github_integration),
            )
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    shape = RoundedCornerShape(
                        topStart = 24.dp, topEnd = 24.dp,
                        bottomStart = 4.dp, bottomEnd = 4.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = state.githubToken,
                            onValueChange = onGithubTokenChange,
                            label = { Text(stringResource(R.string.personal_access_token)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = onSaveGithubToken,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(
                            topStart = 4.dp, topEnd = 4.dp,
                            bottomStart = 24.dp, bottomEnd = 4.dp
                        )
                    ) {
                        Text(stringResource(R.string.save_refresh))
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    FilledTonalButton(
                        onClick = onClearGithubToken,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(
                            topStart = 4.dp, topEnd = 4.dp,
                            bottomStart = 4.dp, bottomEnd = 24.dp
                        )
                    ) {
                        Text(stringResource(R.string.exit))
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsCardWrapper(
    onClick: (() -> Unit)? = null,
    position: CardPosition,
    content: @Composable () -> Unit
) {
    val shape = getShapeForPosition(position)

    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        content()
    }
}

@Composable
fun SettingsItem(
    title: String,
    summary: String? = null,
    icon: Int? = null,
    onClick: () -> Unit,
    position: CardPosition = CardPosition.SINGLE
) {
    SettingsCardWrapper(onClick = onClick, position = position) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (icon != null) {
                    ExpressiveShapeBackground(
                        iconSize = 48.dp,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.size(16.dp))
                }
            }

            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                if (summary != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SliderSettingsItem(
    title: String,
    summary: String? = null,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    position: CardPosition = CardPosition.SINGLE
) {
    val haptic = LocalHapticFeedback.current

    SettingsCardWrapper(position = position) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (summary != null) {
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = value.toFloat(),
                onValueChange = { newValue ->
                    val intNewValue = newValue.roundToInt()
                    if (intNewValue != value) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onValueChange(newValue)
                },
                valueRange = range,
                steps = steps,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}