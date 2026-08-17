package com.ost.application.appmanager
import android.annotation.SuppressLint
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import coil.compose.rememberAsyncImagePainter
import com.ost.application.R
@SuppressLint("WearRecents")
@Composable
fun AppDetailScreen(
    packageName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val appInfo = remember(packageName) {
        try { pm.getApplicationInfo(packageName, 0) } catch (e: Exception) { null }
    }
    val appName = remember(appInfo) {
        appInfo?.loadLabel(pm)?.toString() ?: packageName
    }
    val appIcon = remember(appInfo) {
        appInfo?.loadIcon(pm)
    }
    val isSystemApp = remember(appInfo) {
        appInfo?.let {
            (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (it.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        } ?: false
    }
    val apkSizeBytes = remember(appInfo) {
        try { appInfo?.sourceDir?.let { java.io.File(it).length() } ?: 0L }
        catch (e: Exception) { 0L }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        if (appIcon != null) {
            Image(
                painter = rememberAsyncImagePainter(model = appIcon),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(28.dp),
                contentScale = ContentScale.Crop
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 28.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (appIcon != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = appIcon),
                    contentDescription = appName,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = appName,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = packageName,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatAppSize(apkSizeBytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSystemApp)
                        stringResource(R.string.system_app)
                    else
                        stringResource(R.string.user_app),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSystemApp)
                        Color.Yellow.copy(alpha = 0.85f)
                    else
                        Color.White.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back_24dp),
                        contentDescription = "Back"
                    )
                }
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = "package:$packageName".toUri()
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_info_24dp),
                        contentDescription = stringResource(R.string.app_info)
                    )
                }
            }
        }
    }
}
private fun formatAppSize(bytes: Long): String {
    if (bytes <= 0) return "— MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb < 1.0) "< 1 MB" else "%.1f MB".format(mb)
}
