package com.ost.application.presentation

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import com.ost.application.R
import com.ost.application.util.InfoListScreenContent
import com.ost.application.util.ListItem

class CPUActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CPUScreen()
            }
        }
    }
}

@Composable
fun CPUScreen() {
    val listState = rememberScalingLazyListState()
    val context = LocalContext.current

    val items = remember {
        listOf(
            ListItem(context.getString(R.string.soc_manufacturer), Build.SOC_MANUFACTURER ?: "N/A", null, true, null),
            ListItem("ABI", Build.SUPPORTED_ABIS?.contentToString() ?: "N/A", null, true, null),
            ListItem(context.getString(R.string.cores), Runtime.getRuntime().availableProcessors().toString(), null, true, null)
        )
    }

    val screenTitle = remember { Build.SOC_MODEL }

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
    ) {
        InfoListScreenContent(
            listState = listState,
            screenTitle = screenTitle,
            items = items
        )
    }
}