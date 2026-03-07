package com.ost.application.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ost.application.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier.width(320.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(24.dp))
                content()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp, bottom = 8.dp),
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
@ExperimentalTextApi
@Composable
fun MorphingConvertButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    var isSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            delay(600)
            isSuccess = false
        }
    }
    val targetWeight = when {
        isSuccess -> 1000f
        isPressed -> 100f
        else -> 700f
    }
    val animatedWeight by animateFloatAsState(
        targetValue = targetWeight,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "fontWeight"
    )
    val targetWidth = if (isSuccess) 150f else 100f
    val animatedWidth by animateFloatAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "fontWidth"
    )
    val targetCorner = if (isPressed) 16.dp else 32.dp
    val animatedCorner by animateDpAsState(
        targetValue = targetCorner,
        animationSpec = tween(durationMillis = 200),
        label = "corner"
    )

    val flexFontFamily = remember(animatedWeight, animatedWidth) {
        FontFamily(
            Font(
                resId = R.font.google_sans_flex,
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(animatedWeight.toInt().coerceIn(1, 1000)),
                    FontVariation.width(animatedWidth)
                )
            )
        )
    }

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            isSuccess = true
            onClick()
        },
        interactionSource = interactionSource,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(animatedCorner),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(
            text = stringResource(R.string.convert).uppercase(),
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = flexFontFamily,
                fontSize = 24.sp
            )
        )
    }
}