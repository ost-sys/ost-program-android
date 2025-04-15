@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.ost.application.ui.screen.batteryinfo

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

@Composable
fun BatteryInfoScreen(
    modifier: Modifier = Modifier,
    viewModel: BatteryInfoViewModel = viewModel()
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
                painter = painterResource(id = uiState.iconResId),
                contentDescription = stringResource(R.string.battery),
                modifier = Modifier.size(80.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = uiState.levelText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
        }

        CustomCardItem(
            icon = null, iconPainter = null,
            title = stringResource(R.string.health),
            summary = uiState.health,
            status = true, onClick = null
        )
        CustomCardItem(
            icon = null, iconPainter = null,
            title = stringResource(R.string.status),
            summary = uiState.status,
            status = true, onClick = null
        )
        CustomCardItem(
            icon = null, iconPainter = null,
            title = stringResource(R.string.temperature),
            summary = uiState.temperature,
            status = true, onClick = null
        )
        CustomCardItem(
            icon = null, iconPainter = null,
            title = stringResource(R.string.voltage),
            summary = uiState.voltage,
            status = true, onClick = null
        )
        CustomCardItem(
            icon = null, iconPainter = null,
            title = stringResource(R.string.technology),
            summary = uiState.technology,
            status = true, onClick = null
        )
        CustomCardItem(
            icon = null, iconPainter = null,
            title = stringResource(R.string.capacity),
            summary = if (uiState.isLoadingCapacity) stringResource(R.string.loading) else uiState.capacity,
            status = true, onClick = null
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BatteryInfoScreenPreview() {
    OSTToolsTheme {
        val previewState = BatteryInfoUiState(
            levelText = "75%",
            iconResId = R.drawable.ic_battery_unknown_24dp,
            health = "Good",
            status = "Discharging",
            temperature = "25.0°C",
            voltage = "3.85V",
            technology = "Li-ion",
            capacity = "4500 mAh",
            isLoadingCapacity = false
        )
        LazyColumn(modifier = Modifier.fillMaxSize().padding(bottom = 16.dp)) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(previewState.iconResId), "", Modifier.size(100.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(previewState.levelText, fontSize=20.sp, fontWeight=FontWeight.Bold)
                }
            }
            item { CustomCardItem(icon=null, iconPainter=null, title="Health", summary=previewState.health, status=true, onClick=null) }
            item { CustomCardItem(icon=null, iconPainter=null, title="Status", summary=previewState.status, status=true, onClick=null) }
            item { CustomCardItem(icon=null, iconPainter=null, title="Temperature", summary=previewState.temperature, status=true, onClick=null) }
            item { CustomCardItem(icon=null, iconPainter=null, title="Voltage", summary=previewState.voltage, status=true, onClick=null) }
            item { CustomCardItem(icon=null, iconPainter=null, title="Technology", summary=previewState.technology, status=true, onClick=null) }
            item { CustomCardItem(icon=null, iconPainter=null, title="Capacity", summary=previewState.capacity, status=true, onClick=null) }
        }
    }
}