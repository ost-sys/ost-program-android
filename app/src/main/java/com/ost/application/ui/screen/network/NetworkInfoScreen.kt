@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.ost.application.ui.screen.network

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.R
import com.ost.application.ui.theme.OSTToolsTheme
import com.ost.application.utils.CustomCardItem
import com.ost.application.utils.Tip
import com.ost.application.utils.toast
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
fun NetworkInfoScreen(
    modifier: Modifier = Modifier,
    viewModel: NetworkInfoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.loadInitialDataAndStartUpdates()
        } else {
            context.toast(context.getString(R.string.grant_permission_to_continue))
            viewModel.loadInitialDataAndStartUpdates()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.loadInitialDataAndStartUpdates()
                Lifecycle.Event.ON_PAUSE -> viewModel.stopUpdates()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopUpdates()
        }
    }

    LaunchedEffect(key1 = viewModel.action) {
        viewModel.action.onEach { action ->
            when (action) {
                is NetworkInfoAction.RequestPermission -> {
                    val activity = context as Activity
                    if (ActivityCompat.shouldShowRequestPermissionRationale(activity, action.permission)) {
                        context.toast("Phone state permission is needed for detailed network info.")
                    } else {
                        permissionLauncher.launch(action.permission)
                    }
                }
                is NetworkInfoAction.ShowToast -> {
                    context.toast(context.getString(action.messageResId))
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
                    painter = painterResource(id = uiState.iconRes),
                    contentDescription = uiState.connectivityStatusString,
                    modifier = Modifier.size(100.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.carrierName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        item {
            CustomCardItem(
                icon = null, iconPainter = null,
                title = stringResource(R.string.operator_country),
                summary = uiState.countryCode,
                status = true,
                onClick = null
            )
        }
        item {
            if (!uiState.permissionGranted) {
                CustomCardItem(
                    icon = null, iconPainter = null,
                    title = stringResource(R.string.phone_type),
                    summary = uiState.networkTypeString,
                    status = true,
                    onClick = viewModel::requestPermission
                )
            } else {
                CustomCardItem(
                    icon = null, iconPainter = null,
                    title = stringResource(R.string.phone_type),
                    summary = uiState.networkTypeString,
                    status = true,
                    onClick = null
                )
            }
        }
        item {
            CustomCardItem(
                icon = null, iconPainter = null,
                title = stringResource(R.string.network_connectivity_status),
                summary = uiState.connectivityStatusString,
                status = true,
                onClick = null
            )
        }
        item {
            CustomCardItem(
                icon = null, iconPainter = null,
                title = stringResource(R.string.ip_address),
                summary = if (uiState.isLoadingIp) "Loading" else uiState.ipAddressDisplay,
                status = !uiState.isLoadingIp,
                onClick = if (!uiState.isLoadingIp) viewModel::toggleIpMasking else null
            )
        }

        item {
            Tip(stringResource(R.string.dont_worry), stringResource(R.string.privacy_text))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NetworkInfoScreenPreview() {
    OSTToolsTheme {
        val previewState = NetworkInfoUiState(
            carrierName = "MegaFon",
            countryCode = "RU",
            networkTypeString = "LTE",
            connectivityStatusString = "Mobile data enabled",
            ipAddressDisplay = "10.0.2.15",
            iconRes = R.drawable.ic_signal_cellular_24dp,
            isIpMasked = false,
            isLoadingIp = false,
            permissionGranted = true
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(painterResource(id = previewState.iconRes), "", Modifier.size(100.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(previewState.carrierName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            item { CustomCardItem(icon=null, iconPainter=null, title="Operator Country", summary=previewState.countryCode, status=true, onClick=null) }
            item { CustomCardItem(icon=null, iconPainter=null, title="Phone Type", summary=previewState.networkTypeString, status=true, onClick=null) }
            item { CustomCardItem(icon=null, iconPainter=null, title="Connectivity Status", summary=previewState.connectivityStatusString, status=true, onClick=null) }
            item { CustomCardItem(icon=null, iconPainter=null, title="IP Address", summary=previewState.ipAddressDisplay, status=!previewState.isLoadingIp, onClick={}) }
            item {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Don't worry", style=MaterialTheme.typography.titleSmall)
                    Text("Your IP address is safe...", style=MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}