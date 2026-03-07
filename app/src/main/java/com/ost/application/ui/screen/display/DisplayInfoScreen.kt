package com.ost.application.ui.screen.display

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
import androidx.compose.material3.CircularProgressIndicator
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
import com.ost.application.util.WavyDivider

private data class DisplayInfoRow(
    val titleRes: Int,
    val summary: String? = null,
    val onClick: (() -> Unit)? = null
)

@Composable
fun DisplayInfoScreen(
    modifier: Modifier = Modifier,
    viewModel: DisplayInfoViewModel = viewModel()
) {
    val bottomSpacing = LocalBottomSpacing.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.startUpdates(context.applicationContext)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val displayInfoRows = remember(uiState) {
        listOf(
            DisplayInfoRow(R.string.screen_diagonal, uiState.diagonal),
            DisplayInfoRow(R.string.refresh_rate, uiState.refreshRate),
            DisplayInfoRow(R.string.dpi_dots_per_inch, uiState.dpi),
            DisplayInfoRow(R.string.orientation, uiState.orientation),
            DisplayInfoRow(R.string.stylus_support, uiState.stylusSupport)
        )
    }

    val actionRows = remember(context) {
        listOf(
            DisplayInfoRow(R.string.check_for_dead_pixels, onClick = { viewModel.onCheckPixelsClicked(context) }),
            DisplayInfoRow(R.string.fix_dead_pixels, onClick = { viewModel.onFixPixelsClicked(context) })
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
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
                        painter = painterResource(id = R.drawable.ic_screen_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = uiState.resolution,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        itemsIndexed(displayInfoRows) { index, item ->
            val position = when {
                displayInfoRows.size == 1 -> CardPosition.SINGLE
                index == 0 -> CardPosition.TOP
                index == displayInfoRows.lastIndex -> CardPosition.BOTTOM
                else -> CardPosition.MIDDLE
            }

            CustomCardItem(
                title = stringResource(item.titleRes),
                summary = item.summary,
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

        itemsIndexed(actionRows) { index, item ->
            val position = when {
                actionRows.size == 1 -> CardPosition.SINGLE
                index == 0 -> CardPosition.TOP
                index == actionRows.lastIndex -> CardPosition.BOTTOM
                else -> CardPosition.MIDDLE
            }

            CustomCardItem(
                title = stringResource(item.titleRes),
                summary = item.summary,
                position = position,
                onClick = item.onClick
            )
        }
    }
}