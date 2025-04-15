package com.ost.application.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.dialog.Confirmation
import androidx.wear.compose.material.dialog.Dialog

@Composable
fun ConfimationDialog(message: String, iconResId: Int, onDismiss: () -> Unit) {
    var showDialog by remember { mutableStateOf(true) } // Initially show the dialog

    if (showDialog) {
        Dialog(showDialog = true, onDismissRequest = {
            showDialog = false
            onDismiss()
        }) {
            Confirmation(
                onTimeout = {
                    showDialog = false
                    onDismiss()
                },
                icon = {
                    Icon(painter = painterResource(iconResId), contentDescription = message)
                },
                durationMillis = 3000,
            ) {
                Text(text = message, textAlign = TextAlign.Center)
            }
        }
    }
}