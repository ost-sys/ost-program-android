package com.ost.application.components


import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.ost.application.R

class ConfirmationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "No message provided"
        val iconResId = intent.getIntExtra(EXTRA_ICON, R.drawable.ic_watch_24dp) // Default icon

        setContent {
            ConfirmationScreen(message = message, iconResId = iconResId, onClose = { finish() })
        }
    }

    companion object {
        private const val EXTRA_MESSAGE = "message"
        private const val EXTRA_ICON = "icon"

        fun newIntent(context: Context, message: String, iconResId: Int): Intent {
            return Intent(context, ConfirmationActivity::class.java).apply {
                putExtra(EXTRA_MESSAGE, message)
                putExtra(EXTRA_ICON, iconResId)
            }
        }
    }
}

@Composable
fun ConfirmationScreen(message: String, iconResId: Int, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = iconResId),
            contentDescription = "Confirmation Icon",
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onClose,
            modifier = Modifier
                .size(36.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back_24dp),
                contentDescription = "OK",
                modifier = Modifier
                    .size(ButtonDefaults.DefaultIconSize) // Use a default size
                    .wrapContentSize(align = Alignment.Center)

            )
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    ConfirmationScreen("Test", R.drawable.ic_update_good_24dp, TODO())
}