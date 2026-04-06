@file:OptIn(ExperimentalMaterial3Api::class)

package com.ost.application.ui.screen.converters.currency

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.LocalBottomSpacing
import com.ost.application.R
import com.ost.application.ui.component.ExpressiveShapeBackground
import com.ost.application.ui.component.ExpressiveShapeType
import com.ost.application.ui.component.MorphingConvertButton
import com.ost.application.util.CardPosition
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalTextApi::class)
@Composable
fun CurrencyConverterPage(
    modifier: Modifier = Modifier,
    viewModel: CurrencyConverterViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bottomSpacing = LocalBottomSpacing.current

    if (uiState.showBaseCurrencyWarning) {
        AlertDialog(
            onDismissRequest = viewModel::dismissWarning,
            title = { Text(stringResource(R.string.attention)) },
            text = { Text(stringResource(R.string.base_currency_cannot_be_in_targets)) },
            confirmButton = { TextButton(onClick = viewModel::dismissWarning) { Text("OK") } }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = bottomSpacing + 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatusHeader(
            status = uiState.networkStatus,
            onRefresh = viewModel::refreshData
        )
        Spacer(modifier = Modifier.height(8.dp))

        BigInputCard(
            amount = uiState.amountInput,
            onAmountChange = viewModel::setAmount,
            baseCurrency = uiState.baseCurrency,
            onBaseCurrencyChange = viewModel::setBaseCurrency,
            currencyCodes = uiState.allCurrencyCodes
        )

        Spacer(modifier = Modifier.height(16.dp))

        MorphingConvertButton(
            onClick = viewModel::convertCurrency,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        uiState.targetCurrencies.forEachIndexed { index, item ->
            val cardPosition = when {
                uiState.targetCurrencies.size == 1 -> CardPosition.SINGLE
                index == 0 -> CardPosition.TOP
                index == uiState.targetCurrencies.size - 1 -> CardPosition.BOTTOM
                else -> CardPosition.MIDDLE
            }

            val isEditing = uiState.editingCurrencyCode == item.code

            AnimatedContent(
                targetState = isEditing,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "edit_transition_${item.code}"
            ) { isEditingTarget ->
                if (isEditingTarget) {
                    val availableCodesForEdit = uiState.allCurrencyCodes.filter {
                        it != uiState.baseCurrency && (it == item.code || uiState.targetCurrencies.none { target -> target.code == it })
                    }
                    EditCurrencyCard(
                        modifier = Modifier,
                        position = cardPosition,
                        item = item,
                        availableCodes = availableCodesForEdit,
                        onSave = { newCode -> viewModel.onSaveEditCurrency(item.code, newCode) },
                        onCancel = viewModel::onCancelEditCurrency,
                        onDelete = { viewModel.onDeleteCurrency(item.code) }
                    )
                } else {
                    CustomCardItem(
                        modifier = Modifier,
                        icon = R.drawable.ic_currency_exchange_24dp,
                        title = "${item.result ?: "..."} ${item.code}",
                        summary = "${uiState.amountInput} ${uiState.baseCurrency}",
                        status = true,
                        iconPainter = null,
                        position = cardPosition,
                        onClick = { viewModel.onEditCurrencyClicked(item.code) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = uiState.isAddingCurrency,
            transitionSpec = {
                if (targetState) {
                    (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = 0.6f)))
                        .togetherWith(fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f))
                        .using(SizeTransform(clip = false))
                } else {
                    (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f))
                        .togetherWith(fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.8f))
                        .using(SizeTransform(clip = false))
                }
            },
            label = "AddCurrencyTransition"
        ) { isAdding ->
            if (isAdding) {
                AddCurrencyCard(
                    modifier = Modifier,
                    availableCodes = uiState.availableTargetCodes,
                    onAdd = viewModel::onConfirmAddCurrency,
                    onCancel = viewModel::onCancelAddCurrency
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    LargeFloatingActionButton(
                        onClick = viewModel::onAddCurrencyClicked,
                        shape = RoundedCornerShape(24.dp),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_currency), modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun BigInputCard(
    amount: String,
    onAmountChange: (String) -> Unit,
    baseCurrency: String,
    onBaseCurrencyChange: (String) -> Unit,
    currencyCodes: List<String>
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.amount).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            BasicTextField(
                value = amount,
                onValueChange = onAmountChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            CurrencyChipSelector(
                selectedCurrency = baseCurrency,
                currencyCodes = currencyCodes,
                onCurrencySelected = onBaseCurrencyChange
            )
        }
    }
}

@Composable
fun CurrencyChipSelector(
    selectedCurrency: String,
    currencyCodes: List<String>,
    onCurrencySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedCurrency,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.height(300.dp)
        ) {
            currencyCodes.forEach { code ->
                DropdownMenuItem(
                    text = { Text(code, fontWeight = if(code == selectedCurrency) FontWeight.Bold else FontWeight.Normal) },
                    onClick = {
                        onCurrencySelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun StatusHeader(status: NetworkStatus, onRefresh: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val tooltipState = rememberTooltipState(isPersistent = true)

    val rotation = remember { Animatable(0f) }

    val (starColor, iconColor, statusText) = when (status) {
        NetworkStatus.LOADING -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, stringResource(R.string.loading))
        NetworkStatus.CONNECTED -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, stringResource(R.string.connected))
        NetworkStatus.OFFLINE -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, stringResource(R.string.offline_data))
        NetworkStatus.ERROR -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, stringResource(R.string.not_connected))
    }

    LaunchedEffect(status) {
        when (status) {
            NetworkStatus.LOADING -> {
                if (!rotation.isRunning) {
                    rotation.animateTo(
                        targetValue = rotation.value + 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        )
                    )
                }
            }
            NetworkStatus.CONNECTED, NetworkStatus.OFFLINE -> {
                rotation.stop()
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                scope.launch {
                    tooltipState.show()
                    delay(3000)
                    tooltipState.dismiss()
                }
            }
            NetworkStatus.ERROR -> {
                rotation.stop()
                haptic.performHapticFeedback(HapticFeedbackType.Reject)
                scope.launch {
                    tooltipState.show()
                    delay(3000)
                    tooltipState.dismiss()
                }
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp)
    ) {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { PlainTooltip { Text(statusText) } },
            state = tooltipState
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clickable(
                        onClick = onRefresh,
                        enabled = status != NetworkStatus.LOADING,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    )
            ) {
                val currentRotation = if(status == NetworkStatus.LOADING) rotation.value else 0f
                Box(modifier = Modifier.graphicsLayer { rotationZ = currentRotation }) {
                    ExpressiveShapeBackground(
                        iconSize = 120.dp,
                        color = starColor,
                        forcedShape = ExpressiveShapeType.COOKIE_9
                    )
                }
                Image(
                    painter = painterResource(id = R.drawable.ic_currency_exchange_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    colorFilter = ColorFilter.tint(iconColor)
                )
            }
        }
    }
}

@Composable
fun AddCurrencyCard(
    modifier: Modifier = Modifier,
    availableCodes: List<String>,
    onAdd: (String) -> Unit,
    onCancel: () -> Unit
) {
    var selectedCode by remember { mutableStateOf(availableCodes.firstOrNull() ?: "") }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.add_currency),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            CurrencyChipSelector(
                selectedCurrency = selectedCode,
                currencyCodes = availableCodes,
                onCurrencySelected = { selectedCode = it }
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FilledTonalIconButton(onClick = onCancel, modifier = Modifier.size(48.dp)) {
                    Icon(painterResource(R.drawable.ic_close_24dp), contentDescription = stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(24.dp))
                Button(
                    onClick = { onAdd(selectedCode) },
                    enabled = selectedCode.isNotEmpty(),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(R.string.add))
                }
            }
        }
    }
}

@Composable
fun EditCurrencyCard(
    modifier: Modifier = Modifier,
    position: CardPosition,
    item: TargetCurrencyInfo,
    availableCodes: List<String>,
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    var selectedCode by remember { mutableStateOf(item.code) }
    val shape = RoundedCornerShape(24.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.edit_currency),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.weight(1f))
                FilledTonalIconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                CurrencyChipSelector(
                    selectedCurrency = selectedCode,
                    currencyCodes = availableCodes,
                    onCurrencySelected = { selectedCode = it }
                )
                Spacer(Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(onClick = { onSave(selectedCode) }) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

@Composable
fun CustomCardItem(
    modifier: Modifier,
    icon: Int?,
    title: String,
    summary: String?,
    status: Boolean,
    iconPainter: Painter?,
    position: CardPosition = CardPosition.SINGLE,
    onClick: (() -> Unit)?
) {
    val largeCornerRadius = 24.dp
    val smallCornerRadius = 4.dp

    val shape = when (position) {
        CardPosition.TOP -> RoundedCornerShape(topStart = largeCornerRadius, topEnd = largeCornerRadius, bottomStart = smallCornerRadius, bottomEnd = smallCornerRadius)
        CardPosition.MIDDLE -> RoundedCornerShape(smallCornerRadius)
        CardPosition.BOTTOM -> RoundedCornerShape(topStart = smallCornerRadius, topEnd = smallCornerRadius, bottomStart = largeCornerRadius, bottomEnd = largeCornerRadius)
        CardPosition.SINGLE -> RoundedCornerShape(largeCornerRadius)
    }

    Card(
        onClick = onClick ?: {},
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = if (position == CardPosition.MIDDLE || position == CardPosition.BOTTOM) 1.dp else 0.dp,
                bottom = if (position == CardPosition.MIDDLE || position == CardPosition.TOP) 1.dp else 0.dp
            ),
        enabled = status,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else if (iconPainter != null) {
                Image(
                    painter = iconPainter,
                    contentDescription = title,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                summary?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}