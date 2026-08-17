package com.ost.application.ui.screen.converters.timecalc
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimeCalculatorPage(
    viewModel: TimeCalculatorViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        HistoryAndActiveTerm(
            history = state.history,
            current = state.current,
            focusedUnit = state.focusedUnit,
            errorMessage = state.errorMessage,
            onUnitTap = viewModel::onUnitFocused,
            modifier = Modifier.weight(1f),
        )
        HorizontalDivider()
        Keypad(
            onDigit = viewModel::onDigit,
            onOperator = viewModel::onOperator,
            onEquals = viewModel::onEquals,
            onBackspace = viewModel::onBackspace,
            onAllClear = viewModel::onAllClear,
        )
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryAndActiveTerm(
    history: List<ChainTerm>,
    current: TimeDuration,
    focusedUnit: DurationUnit,
    errorMessage: String?,
    onUnitTap: (DurationUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val isScrolledAway = listState.firstVisibleItemIndex > 0 ||
            listState.firstVisibleItemScrollOffset > with(density) { 8.dp.toPx() }
    val collapseFraction by animateFloatAsState(targetValue = if (isScrolledAway) 1f else 0f, label = "activeTermCollapse")
    val activeScale = 1f - 0.22f * collapseFraction
    Box(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            stickyHeader(key = "active") {
                ActiveTermRow(
                    duration = current,
                    focusedUnit = focusedUnit,
                    onUnitTap = onUnitTap,
                    scale = activeScale,
                )
            }
            items(history.reversed(), key = { it.id }) { term ->
                Column {
                    term.trailingOperator?.let { op -> WavyOperatorDivider(operator = op) }
                    HistoryTermRow(duration = term.duration)
                }
            }
        }
        errorMessage?.let { message ->
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
@Composable
private fun ActiveTermRow(
    duration: TimeDuration,
    focusedUnit: DurationUnit,
    onUnitTap: (DurationUnit) -> Unit,
    scale: Float,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        Box(modifier = Modifier.scale(scale)) {
            DurationFieldsRow(
                duration = duration,
                focusedUnit = focusedUnit,
                onUnitTap = onUnitTap,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}
@Composable
private fun HistoryTermRow(duration: TimeDuration) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        DurationFieldsRow(
            duration = duration,
            focusedUnit = null,
            onUnitTap = null,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}
@Composable
private fun DurationFieldsRow(
    duration: TimeDuration,
    focusedUnit: DurationUnit?,
    onUnitTap: ((DurationUnit) -> Unit)?,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        for (unit in DurationUnit.ORDERED) {
            val isFocused = unit == focusedUnit
            Column(
                modifier = Modifier
                    .let { base -> if (onUnitTap != null) base.clickable { onUnitTap(unit) } else base },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = duration.get(unit)?.toString() ?: "\u2013",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
                    color = contentColor,
                )
                Text(
                    text = unit.shortLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f),
                )
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .height(2.dp)
                        .width(if (isFocused) 20.dp else 0.dp)
                        .background(contentColor, RoundedCornerShape(1.dp)),
                )
            }
        }
    }
}
@Composable
private fun WavyOperatorDivider(operator: Operator, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val badgeContainer = MaterialTheme.colorScheme.tertiaryContainer
    val badgeContent = MaterialTheme.colorScheme.onTertiaryContainer
    val rotation = OperatorBadgeShapes.rotationDegreesFor(operator)
    val bgShape = OperatorBadgeShapes.shapeFor(operator)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val badgeGap = 40.dp.toPx()
            val amplitude = 3.5.dp.toPx()
            val wavelength = 22.dp.toPx()
            val midY = size.height / 2f
            val leftEnd = size.width / 2f - badgeGap / 2f
            val rightStart = size.width / 2f + badgeGap / 2f
            drawWavySegment(0f, leftEnd, midY, amplitude, wavelength, lineColor)
            drawWavySegment(rightStart, size.width, midY, amplitude, wavelength, lineColor)
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .rotate(rotation)
                .background(badgeContainer, bgShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = operator.symbol,
                color = badgeContent,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.rotate(-rotation),
            )
        }
    }
}
private fun DrawScope.drawWavySegment(
    startX: Float,
    endX: Float,
    midY: Float,
    amplitude: Float,
    wavelength: Float,
    color: Color,
) {
    if (endX <= startX) return
    val path = Path().apply {
        moveTo(startX, midY)
        var x = startX
        var up = true
        while (x < endX) {
            val nextX = (x + wavelength / 2f).coerceAtMost(endX)
            val controlX = (x + nextX) / 2f
            val controlY = midY + if (up) -amplitude else amplitude
            quadraticBezierTo(controlX, controlY, nextX, midY)
            x = nextX
            up = !up
        }
    }
    drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
}
private object KeypadRadii {
    val resting = RoundedCornerShape(20.dp)
    val pressed = RoundedCornerShape(12.dp)
    val opResting = RoundedCornerShape(20.dp)
    val opPressed = RoundedCornerShape(28.dp)
}
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Keypad(
    onDigit: (Int) -> Unit,
    onOperator: (Operator) -> Unit,
    onEquals: () -> Unit,
    onBackspace: () -> Unit,
    onAllClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ButtonGroup(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), overflowIndicator = { _ -> }) {
            DigitButton(7, onDigit)
            DigitButton(8, onDigit)
            DigitButton(9, onDigit)
            OperatorKey(Operator.DIVIDE, onOperator)
        }
        ButtonGroup(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), overflowIndicator = { _ -> }) {
            DigitButton(4, onDigit)
            DigitButton(5, onDigit)
            DigitButton(6, onDigit)
            OperatorKey(Operator.MULTIPLY, onOperator)
        }
        ButtonGroup(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), overflowIndicator = { _ -> }) {
            DigitButton(1, onDigit)
            DigitButton(2, onDigit)
            DigitButton(3, onDigit)
            OperatorKey(Operator.SUBTRACT, onOperator)
        }
        ButtonGroup(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), overflowIndicator = { _ -> }) {
            ClearKey(onAllClear)
            DigitButton(0, onDigit)
            BackspaceKey(onBackspace)
            OperatorKey(Operator.ADD, onOperator)
        }
        val eqShape = ButtonDefaults.shapes(shape = KeypadRadii.opResting, pressedShape = KeypadRadii.pressed)
        val eqColors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
        Button(
            onClick = onEquals,
            shapes = eqShape,
            colors = eqColors,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text("=", style = MaterialTheme.typography.headlineSmall)
        }
    }
}
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun ButtonGroupScope.DigitButton(digit: Int, onDigit: (Int) -> Unit) {
    customItem(
        buttonGroupContent = {
            val interactionSource = remember { MutableInteractionSource() }
            Button(
                onClick = { onDigit(digit) },
                shapes = ButtonDefaults.shapes(shape = KeypadRadii.resting, pressedShape = KeypadRadii.pressed),
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier
                    .weight(1f)
                    .animateWidth(interactionSource)
                    .height(56.dp),
            ) {
                Text(digit.toString(), style = MaterialTheme.typography.headlineSmall)
            }
        },
        menuContent = { _ -> }
    )
}
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun ButtonGroupScope.OperatorKey(operator: Operator, onOperator: (Operator) -> Unit) {
    customItem(
        buttonGroupContent = {
            val interactionSource = remember { MutableInteractionSource() }
            Button(
                onClick = { onOperator(operator) },
                shapes = ButtonDefaults.shapes(shape = KeypadRadii.opResting, pressedShape = KeypadRadii.opPressed),
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                modifier = Modifier
                    .weight(1f)
                    .animateWidth(interactionSource)
                    .height(56.dp),
            ) {
                Text(operator.symbol, style = MaterialTheme.typography.headlineSmall)
            }
        },
        menuContent = { _ -> }
    )
}
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun ButtonGroupScope.BackspaceKey(onBackspace: () -> Unit) {
    customItem(
        buttonGroupContent = {
            val interactionSource = remember { MutableInteractionSource() }
            Button(
                onClick = onBackspace,
                shapes = ButtonDefaults.shapes(shape = KeypadRadii.resting, pressedShape = KeypadRadii.pressed),
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier
                    .weight(1f)
                    .animateWidth(interactionSource)
                    .height(56.dp),
            ) {
                Icon(Icons.Filled.Backspace, contentDescription = "Backspace")
            }
        },
        menuContent = { _ -> }
    )
}
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun ButtonGroupScope.ClearKey(onAllClear: () -> Unit) {
    customItem(
        buttonGroupContent = {
            val interactionSource = remember { MutableInteractionSource() }
            Button(
                onClick = onAllClear,
                shapes = ButtonDefaults.shapes(shape = KeypadRadii.resting, pressedShape = KeypadRadii.pressed),
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier
                    .weight(1f)
                    .animateWidth(interactionSource)
                    .height(56.dp),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Clear all")
            }
        },
        menuContent = { _ -> }
    )
}
