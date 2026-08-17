package com.ost.application.presentation
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.google.android.gms.wearable.Wearable
import com.ost.application.BuildConfig
import com.ost.application.R
import com.ost.application.theme.OSTToolsTheme
private const val REMOTE_OPEN_PATH = "/open_url"
private const val ABOUT_PHONE_PATH = "/open_about"
class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSTToolsTheme {
                AboutScreen()
            }
        }
    }
}
@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()
    val iconBitmap = remember {
        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(context.packageName, 0)
            val drawable = pm.getApplicationIcon(appInfo)
            val bmp = createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1)
            )
            val canvas = android.graphics.Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
    AppScaffold(timeText = { TimeText() }) {
        ScreenScaffold(scrollState = listState) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(30.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                Modifier.size(0.dp)
                            )
                    )
                }
                ScalingLazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    state = listState,
                    anchorType = ScalingLazyListAnchorType.ItemCenter,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        if (iconBitmap != null) {
                            Image(
                                bitmap = iconBitmap,
                                contentDescription = "App icon",
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .size(52.dp)
                            )
                        }
                    }
                    item {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Text(
                            text = BuildConfig.VERSION_NAME,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Text(
                            text = BuildConfig.APPLICATION_ID,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Text(
                            text = stringResource(R.string.ost),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_settings_24dp),
                                    contentDescription = "App settings"
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            FilledIconButton(
                                onClick = { openAboutOnPhone(context) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_open_in_new_24dp),
                                    contentDescription = "Continue on phone"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
fun openAboutOnPhone(context: Context) {
    val nodeClient = Wearable.getNodeClient(context)
    val messageClient = Wearable.getMessageClient(context)
    nodeClient.connectedNodes.addOnSuccessListener { nodes ->
        nodes.forEach { node ->
            messageClient.sendMessage(node.id, ABOUT_PHONE_PATH, null)
        }
    }
}
