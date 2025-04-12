@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.ost.application.ui.screen.deviceinfo

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.R
import com.ost.application.utils.CustomCardItem
import com.ost.application.utils.toast
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.Executor

@Composable
fun DeviceInfoScreen(
    modifier: Modifier = Modifier,
    viewModel: DeviceInfoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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
                is DefaultInfoAction.ShowToast -> context.toast(context.getString(action.messageResId))
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
        modifier = modifier
            .fillMaxSize()
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = uiState.deviceTypeIconRes),
                    contentDescription = uiState.deviceType,
                    modifier = Modifier.size(100.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (uiState.isLoadingName) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = uiState.deviceName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        item {
            CustomCardItem(
                icon = null,
                iconPainter = null,
                title = stringResource(R.string.android_version),
                summary = uiState.androidVersion,
                status = true,
                onClick = viewModel::onAndroidVersionClicked
            )
        }
        item {
            CustomCardItem(
                icon = null,
                iconPainter = null,
                title = stringResource(R.string.brand),
                summary = uiState.brand,
                status = true,
                onClick = null
            )
        }
        item {
            CustomCardItem(
                icon = null,
                iconPainter = null,
                title = stringResource(R.string.board),
                summary = uiState.board,
                status = true,
                onClick = null
            )
        }
        item {
            CustomCardItem(
                icon = null,
                iconPainter = null,
                title = stringResource(R.string.model),
                summary = uiState.model,
                status = true,
                onClick = null
            )
        }
        item {
            CustomCardItem(
                icon = null,
                iconPainter = null,
                title = stringResource(R.string.codename),
                summary = uiState.codename,
                status = true,
                onClick = null
            )
        }
        item {
            CustomCardItem(
                icon = null,
                iconPainter = null,
                title = stringResource(R.string.build_number),
                summary = uiState.buildNumber,
                status = true,
                onClick = null
            )
        }
        item {
            CustomCardItem(
                icon = null,
                iconPainter = null,
                title = stringResource(R.string.software_development_kit),
                summary = uiState.sdkVersion,
                status = true,
                onClick = null
            )
        }
        item {
            CustomCardItem(
                icon = null,
                iconPainter = null,
                title = stringResource(R.string.device_type),
                summary = uiState.deviceType,
                status = true,
                onClick = null
            )
        }
        item {
            CustomCardItem(
                icon = null,
                iconPainter = null,
                title = stringResource(R.string.ram),
                summary = uiState.ramInfo,
                status = true,
                onClick = null
            )
        }
        item {
            CustomCardItem(
                icon = null,
                iconPainter = null,
                title = stringResource(R.string.rom),
                summary = uiState.romInfo,
                status = true,
                onClick = null
            )
        }
        item {
            CustomCardItem(
                icon = null,
                iconPainter = null,
                title = stringResource(R.string.build_fingerprint),
                summary = uiState.buildFingerprint,
                status = true,
                onClick = null
            )
        }
        item {
            CustomCardItem(
                icon = null,
                iconPainter = null,
                title = stringResource(R.string.fingerprint_support),
                summary = uiState.fingerprintStatus,
                status = uiState.isFingerprintTestable,
                onClick = viewModel::onFingerprintTestClicked
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
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