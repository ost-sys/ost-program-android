package com.ost.application.ui.screen.network

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.LocalBottomSpacing
import com.ost.application.R
import com.ost.application.ui.component.ExpressiveShapeBackground
import com.ost.application.util.CardPosition
import com.ost.application.util.CustomCardItem
import com.ost.application.util.WarningTip
import com.ost.application.util.WavyDivider

private data class NetworkInfoRow(
    val titleRes: Int,
    val summary: String,
    val onClick: (() -> Unit)? = null,
    val isLoading: Boolean = false
)

@Composable
fun NetworkInfoScreen(
    modifier: Modifier = Modifier,
    viewModel: NetworkInfoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val bottomSpacing = LocalBottomSpacing.current

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

    val networkInfoRows = remember(uiState.permissionGranted, uiState.isLoadingIp, uiState) {
        listOf(
            NetworkInfoRow(R.string.operator_country, uiState.countryCode),
            NetworkInfoRow(
                titleRes = R.string.phone_type,
                summary = uiState.networkTypeString,
                onClick = if (!uiState.permissionGranted) viewModel::requestPermission else null
            ),
            NetworkInfoRow(R.string.network_connectivity_status, uiState.connectivityStatusString),
            NetworkInfoRow(
                titleRes = R.string.ip_address,
                summary = uiState.ipAddressDisplay,
                isLoading = uiState.isLoadingIp,
                onClick = if (!uiState.isLoadingIp) viewModel::toggleIpMasking else null
            )
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp + bottomSpacing),
        verticalArrangement = Arrangement.spacedBy(0.dp)
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
                    ExpressiveShapeBackground(
                        iconSize = 120.dp,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )

                    Image(
                        painter = painterResource(id = uiState.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = uiState.carrierName,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        itemsIndexed(networkInfoRows) { index, item ->
            val position = when {
                networkInfoRows.size == 1 -> CardPosition.SINGLE
                index == 0 -> CardPosition.TOP
                index == networkInfoRows.lastIndex -> CardPosition.BOTTOM
                else -> CardPosition.MIDDLE
            }

            CustomCardItem(
                title = stringResource(item.titleRes),
                summary = if (item.isLoading) stringResource(R.string.loading) else item.summary,
                position = position,
                onClick = item.onClick
            )
        }

        item {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                WavyDivider()
            }
        }
        item {
            WarningTip(
                title = stringResource(R.string.dont_worry),
                summary = stringResource(R.string.privacy_text)
            )
        }
    }
}