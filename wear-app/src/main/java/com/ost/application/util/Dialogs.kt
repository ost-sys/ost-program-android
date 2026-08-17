package com.ost.application.util
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ConfirmationDialog
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.SuccessConfirmationDialog
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.curvedText
import com.ost.application.R
@Composable
fun FailDialog(
    showDialog: Boolean,
    message: String,
    iconResId: Int,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        visible = showDialog,
        onDismissRequest = onDismiss,
        durationMillis = 4000,
        content = {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
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
@Composable
fun SuccessDialog(
    showDialog: Boolean,
    actionIconResId: Int,
    onDismiss: () -> Unit
) {
    val successText = stringResource(R.string.done)
    SuccessConfirmationDialog(
        visible = showDialog,
        onDismissRequest = onDismiss,
        durationMillis = 2000,
        content = {
            Icon(
                painter = painterResource(id = actionIconResId),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        },
        curvedText = {
            curvedText(text = successText)
        },
    )
}
