@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.ost.application.ui.screen.cpuinfo

import android.os.Build
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.R
import com.ost.application.ui.theme.OSTToolsTheme
import com.ost.application.utils.CustomCardItem
import com.ost.application.utils.Tip

@Composable
fun CpuInfoScreen(
    modifier: Modifier = Modifier,
    viewModel: CpuInfoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_cpu_24dp),
                contentDescription = stringResource(R.string.cpu),
                modifier = Modifier.size(80.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = uiState.cpuName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && uiState.manufacturer != null) {
            CustomCardItem(
                icon = null, iconPainter = null,
                title = stringResource(R.string.soc_manufacturer),
                summary = uiState.manufacturer,
                status = true,
                onClick = null
            )
        }
        CustomCardItem(
            icon = null, iconPainter = null,
            title = stringResource(R.string.application_binary_interface),
            summary = uiState.abiString,
            status = true,
            onClick = null
        )
        CustomCardItem(
            icon = null, iconPainter = null,
            title = stringResource(R.string.cores),
            summary = uiState.coresString,
            status = true,
            onClick = null
        )
        CustomCardItem(
            icon = null, iconPainter = null,
            title = stringResource(R.string.frequency_min_max),
            summary = uiState.clockSpeedString,
            status = true,
            onClick = null
        )

        Tip(stringResource(R.string.attention), stringResource(R.string.still_on_beta))
    }
}

@Preview(showBackground = true)
@Composable
private fun CpuInfoScreenPreview() {
    OSTToolsTheme {
        val previewState = CpuInfoUiState(
            cpuName = "Snapdragon 8 Gen 2 for Galaxy",
            manufacturer = "QUALCOMM",
            abiString = "arm64-v8a, armeabi-v7a, armeabi",
            coresString = "8",
            clockSpeedString = "2000 MHz - 3.36 GHz",
            isLoading = false
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(painterResource(id = R.drawable.ic_cpu_24dp), "", Modifier.size(100.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(previewState.cpuName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            item { CustomCardItem(icon=null, iconPainter=null, title="SoC Manufacturer", summary=previewState.manufacturer, status=true, onClick=null) }
            item { CustomCardItem(icon=null, iconPainter=null, title="ABI", summary=previewState.abiString, status=true, onClick=null) }
            item { CustomCardItem(icon=null, iconPainter=null, title="Cores", summary=previewState.coresString, status=true, onClick=null) }
            item { CustomCardItem(icon=null, iconPainter=null, title="Frequency", summary=previewState.clockSpeedString, status=true, onClick=null) }
            item {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(stringResource(R.string.attention), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.still_on_beta), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}