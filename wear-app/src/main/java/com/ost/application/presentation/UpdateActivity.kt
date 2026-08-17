package com.ost.application.presentation
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.google.android.gms.wearable.Wearable
import com.ost.application.BuildConfig
import com.ost.application.R
import com.ost.application.UpdateCheckResult
import com.ost.application.core.update.UpdateChecker
import com.ost.application.theme.OSTToolsTheme
import com.ost.application.util.FailDialog
import com.ost.application.util.SuccessDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds
private const val PREFS_NAME = "update_prefs"
private const val KEY_INCLUDE_BETA = "include_beta"
private const val TAG = "UpdateActivity"
sealed class UpdateDialogState {
    data class UpdateWithPhone(val version: String) : UpdateDialogState()
    data class UpdateNoPhone(val version: String) : UpdateDialogState()
    data class Failure(val message: String, val iconResId: Int) : UpdateDialogState()
}
class UpdateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSTToolsTheme {
                UpdateScreen()
            }
        }
    }
}
@Composable
fun UpdateScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var includeBeta by remember {
        mutableStateOf(prefs.getBoolean(KEY_INCLUDE_BETA, UpdateChecker.isPrereleaseVersion(BuildConfig.VERSION_NAME)))
    }
    var isChecking by remember { mutableStateOf(false) }
    var dialogState by remember { mutableStateOf<UpdateDialogState?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()
    AppScaffold(timeText = { TimeText() }) {
        ScreenScaffold(
            scrollState = listState,
            edgeButton = {
                EdgeButton(
                    onClick = {
                        val currentIncludeBeta = includeBeta
                        coroutineScope.launch {
                            isChecking = true
                            val result = checkForUpdatesWithChannel(context, currentIncludeBeta)
                            val phoneConnected = isPhoneConnectedUpdate(context)
                            dialogState = when (result) {
                                is UpdateCheckResult.UpdateAvailable ->
                                    if (phoneConnected) UpdateDialogState.UpdateWithPhone(result.latestVersion)
                                    else UpdateDialogState.UpdateNoPhone(result.latestVersion)
                                is UpdateCheckResult.LatestVersion -> UpdateDialogState.Failure(
                                    context.getString(R.string.you_are_updated),
                                    R.drawable.ic_update_good_24dp
                                )
                                is UpdateCheckResult.Error -> UpdateDialogState.Failure(
                                    result.message,
                                    R.drawable.ic_error_24dp
                                )
                            }
                            isChecking = false
                        }
                    },
                    enabled = !isChecking
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_update_24dp),
                            contentDescription = "Check updates"
                        )
                    }
                }
            }
        ) {
            ScalingLazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                state = listState,
                anchorType = ScalingLazyListAnchorType.ItemCenter,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text(
                        text = "Updates",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text(
                        text = "Current: ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item { Spacer(Modifier.height(4.dp)) }
                item {
                    SwitchButton(
                        checked = includeBeta,
                        onCheckedChange = { checked ->
                            includeBeta = checked
                            prefs.edit().putBoolean(KEY_INCLUDE_BETA, checked).apply()
                        },
                        label = { Text("Beta builds") },
                        secondaryLabel = { Text(if (includeBeta) "Enabled" else "Disabled") },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_science_24dp),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    dialogState?.let { state ->
        when (state) {
            is UpdateDialogState.UpdateWithPhone -> SuccessDialog(
                actionIconResId = R.drawable.ic_update_good_24dp,
                onDismiss = { dialogState = null },
                showDialog = true
            )
            is UpdateDialogState.UpdateNoPhone -> FailDialog(
                showDialog = true,
                message = context.getString(R.string.update_available_check_phone),
                iconResId = R.drawable.ic_update_good_24dp,
                onDismiss = { dialogState = null }
            )
            is UpdateDialogState.Failure -> FailDialog(
                showDialog = true,
                message = state.message,
                iconResId = state.iconResId,
                onDismiss = { dialogState = null }
            )
        }
    }
}
suspend fun isPhoneConnectedUpdate(context: Context): Boolean {
    return try {
        val nodes = withTimeoutOrNull(3000L.milliseconds) {
            Wearable.getNodeClient(context).connectedNodes.await()
        }
        Log.d(TAG, "Connected nodes: ${nodes?.map { "${it.displayName} (${it.id})" }}")
        nodes?.isNotEmpty() == true
    } catch (e: ClassCastException) {
        Log.w(TAG, "Wearable API ClassCastException — treating as disconnected", e)
        false
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get connected nodes", e)
        false
    }
}
suspend fun checkForUpdatesWithChannel(
    context: Context,
    includeBeta: Boolean
): UpdateCheckResult {
    return withContext(Dispatchers.IO) {
        try {
            val release = UpdateChecker.fetchLatestRelease(includePrereleases = includeBeta)
                ?: return@withContext UpdateCheckResult.Error(context.getString(R.string.no_releases_found))
            val latestWearVersion = release.wearVersion
                ?: return@withContext UpdateCheckResult.Error(context.getString(R.string.update_check_error))
            if (UpdateChecker.isNewerVersion(latestWearVersion, BuildConfig.VERSION_NAME)) {
                UpdateCheckResult.UpdateAvailable(latestWearVersion)
            } else {
                UpdateCheckResult.LatestVersion
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error", e)
            UpdateCheckResult.Error(context.getString(R.string.network_error))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error", e)
            UpdateCheckResult.Error("${context.getString(R.string.error)}: ${e.message ?: e.javaClass.simpleName}")
        }
    }
}
