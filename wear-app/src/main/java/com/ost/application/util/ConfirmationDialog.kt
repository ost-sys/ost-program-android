package com.ost.application.util

import androidx.compose.foundation.layout.size// Убедитесь, что импорты ведут на material3
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ConfirmationDialog
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text

@Composable
fun ConfirmationDialog(
    showDialog: Boolean,
    message: String,
    iconResId: Int,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        visible = showDialog,
        onDismissRequest = onDismiss,
        durationMillis = 3000,
        content = {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = message,
                modifier = Modifier.size(36.dp)
            )
        },
        text = {
            Text(
                text = message,
                textAlign = TextAlign.Center,
            )
        },
    )
}