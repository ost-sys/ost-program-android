@file:OptIn(ExperimentalMaterial3Api::class)

package com.ost.application.ui.screen.deviceinfo

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.CordItem
import com.ost.application.LocalBottomSpacing
import com.ost.application.R
import com.ost.application.ui.component.ExpressiveShapeBackground
import com.ost.application.util.CardPosition
import com.ost.application.util.CustomCardItem
import com.ost.application.util.ToolsManager
import com.ost.application.util.toast
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.Executor

private data class DeviceInfoRow(
    val icon: Int,
    val titleRes: Int,
    val summary: String?,
    val onClick: (() -> Unit)? = null
)

@ExperimentalMaterial3ExpressiveApi
@Composable
fun DeviceInfoScreen(
    modifier: Modifier = Modifier,
    viewModel: DeviceInfoViewModel = viewModel()
) {
    val bottomSpacing = LocalBottomSpacing.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    var rotationState by remember { mutableFloatStateOf(0f) }

    var showAccelerometerSheet by remember { mutableStateOf(false) }

    val rotationDegrees by animateFloatAsState(
        targetValue = rotationState,
        animationSpec = tween(durationMillis = 500),
        label = "iconRotation"
    )

    val deviceInfoRows = remember(uiState) {
        listOf(
            DeviceInfoRow(R.drawable.ic_android_24dp, R.string.android_version, "${uiState.androidVersion} (${viewModel.getLatestCodename()})", viewModel::onAndroidVersionClicked),
            DeviceInfoRow(R.drawable.ic_sell_24dp, R.string.brand, uiState.brand),
            DeviceInfoRow(R.drawable.ic_developer_board_24dp, R.string.board, uiState.board),
            DeviceInfoRow(R.drawable.ic_phone_android_24dp, R.string.model, uiState.model),
            DeviceInfoRow(R.drawable.ic_mobile_code_24dp, R.string.codename, uiState.codename),
            DeviceInfoRow(R.drawable.ic_build_24dp, R.string.build_number, uiState.buildNumber),
            DeviceInfoRow(R.drawable.ic_adb_24dp, R.string.software_development_kit, uiState.sdkVersion),
            DeviceInfoRow(uiState.deviceTypeIconRes, R.string.device_type, uiState.deviceType),
            DeviceInfoRow(R.drawable.ic_memory_alt_24dp, R.string.ram, uiState.ramInfo),
            DeviceInfoRow(R.drawable.ic_storage_24dp, R.string.rom, uiState.romInfo),
            DeviceInfoRow(R.drawable.ic_3d_rotation_24dp, R.string.accelerometer, null, { showAccelerometerSheet = true}),
            DeviceInfoRow(R.drawable.ic_manufacturing_24dp, R.string.build_fingerprint, uiState.buildFingerprint),
            DeviceInfoRow(R.drawable.ic_fingerprint_24dp, R.string.biometrics_support, uiState.fingerprintStatus)
        )
    }

    if (showAccelerometerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAccelerometerSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            val sensorData by ToolsManager.getAccelerometerData(context).collectAsState(initial = floatArrayOf(0f, 0f, 0f))
            val x = sensorData[0]
            val y = sensorData[1]
            val z = sensorData[2]

            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.accelerometer),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 32.dp))
                Card(
                    modifier = Modifier
                        .size(160.dp)
                        .graphicsLayer {
                            rotationX = y * 6f
                            rotationY = x * 6f
                        },
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {}
                Spacer(Modifier.height(48.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CordItem("X", x)
                    CordItem("Y", y)
                    CordItem("Z", z)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.startPeriodicUpdates()
                Lifecycle.Event.ON_PAUSE -> viewModel.stopPeriodicUpdates()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopPeriodicUpdates()
        }
    }

    LaunchedEffect(key1 = viewModel.action) {
        viewModel.action.onEach { action ->
            when (action) {
                is DefaultInfoAction.ShowToastMsg -> context.toast(action.message)
                is DefaultInfoAction.LaunchEasterEgg -> {
                    try {
                        context.startActivity(action.intent)
                    } catch (e: Exception) {
                        Log.e("DeviceInfoScreen", "Failed to launch easter egg", e)
                        context.toast("Could not launch Easter Egg.")
                    }
                }
                is DefaultInfoAction.ShowBiometricPrompt -> {
                    showBiometricPrompt(context) { success, message ->
                        viewModel.handleBiometricAuthResult(success, message)
                    }
                }
            }
        }.launchIn(this)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp + bottomSpacing),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(0.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier.graphicsLayer(rotationZ = rotationDegrees)
                    ) {
                        ExpressiveShapeBackground(
                            iconSize = 120.dp,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    }
                    Image(
                        painter = painterResource(id = uiState.deviceTypeIconRes),
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isLoadingName) {
                    CircularWavyProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = uiState.deviceName,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        itemsIndexed(deviceInfoRows) { index, item ->
            val position = when {
                deviceInfoRows.size == 1 -> CardPosition.SINGLE
                index == 0 -> CardPosition.TOP
                index == deviceInfoRows.lastIndex -> CardPosition.BOTTOM
                else -> CardPosition.MIDDLE
            }

            CustomCardItem(
                title = stringResource(item.titleRes),
                summary = item.summary,
                icon = item.icon,
                position = position,
                onClick = item.onClick
            )
        }
    }
}

private fun showBiometricPrompt(
    context: Context,
    onResult: (success: Boolean, message: CharSequence?) -> Unit
) {
    val activity = context as? FragmentActivity ?: run {
        Log.e("Biometric", "Context is not a FragmentActivity")
        onResult(false, "Internal error: Invalid context")
        return
    }
    val executor: Executor = ContextCompat.getMainExecutor(context)
    val biometricPrompt = BiometricPrompt(activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(true, null)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON && errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                    onResult(false, errString)
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onResult(false, context.getString(R.string.fail))
            }
        })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(context.getString(R.string.fingerprint))
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        .build()

    try {
        biometricPrompt.authenticate(promptInfo)
    } catch (e: Exception) {
        Log.e("Biometric", "Error showing biometric prompt", e)
        onResult(false, "Error launching fingerprint test: ${e.message}")
    }
}