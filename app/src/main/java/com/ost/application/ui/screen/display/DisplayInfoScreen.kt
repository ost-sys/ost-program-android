@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.ost.application.ui.screen.display

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.R
import com.ost.application.ui.theme.OSTToolsTheme
import com.ost.application.utils.CustomCardItem

@Composable
fun DisplayInfoScreen(
    modifier: Modifier = Modifier,
    viewModel: DisplayInfoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_screen_24dp),
                contentDescription = stringResource(R.string.display),
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text(
                    text = uiState.resolution,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
            CustomCardItem(
                icon = null,
                title = stringResource(R.string.screen_diagonal),
                summary = uiState.diagonal,
                status = !uiState.isLoading,
                onClick = null,
                iconPainter = null
            )
            CustomCardItem(
                icon = null,
                title = stringResource(R.string.refresh_rate),
                summary = uiState.refreshRate,
                status = !uiState.isLoading,
                onClick = null,
                iconPainter = null
            )
            CustomCardItem(
                icon = null,
                title = stringResource(R.string.dpi_dots_per_inch),
                summary = uiState.dpi,
                status = !uiState.isLoading,
                onClick = null,
                iconPainter = null
            )
            CustomCardItem(
                icon = null,
                title = stringResource(R.string.orientation),
                summary = uiState.orientation,
                status = !uiState.isLoading,
                onClick = null,
                iconPainter = null
            )
            CustomCardItem(
                icon = null,
                title = stringResource(R.string.stylus_support),
                summary = uiState.stylusSupport,
                status = !uiState.isLoading,
                onClick = null,
                iconPainter = null
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
            CustomCardItem(
                icon = null,
                title = stringResource(R.string.check_for_dead_pixels),
                summary = null,
                status = true,
                onClick = { viewModel.onCheckPixelsClicked(context) },
                iconPainter = null
            )
            CustomCardItem(
                icon = null,
                title = stringResource(R.string.fix_dead_pixels),
                summary = stringResource(R.string.fix_dead_pixels_w),
                status = true,
                onClick = { viewModel.onFixPixelsClicked(context) },
                iconPainter = null
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DisplayInfoScreenPreview() {
    OSTToolsTheme {
        val previewState = DisplayInfoUiState(isLoading = false, resolution = "2400 x 1080", refreshRate = "120 Hz", dpi = "480 dpi", diagonal = "6.7 inches", orientation = "Portrait", stylusSupport = "Unsupported")
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_screen_24dp), contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = previewState.resolution, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                CustomCardItem(icon = null, title = "Screen Diagonal", summary = previewState.diagonal, status = true, onClick = null, iconPainter = null)
                CustomCardItem(icon = null, title = "Refresh Rate", summary = previewState.refreshRate, status = true, onClick = null, iconPainter = null)
                CustomCardItem(icon = null, title = "DPI", summary = previewState.dpi, status = true, onClick = null, iconPainter = null)
                CustomCardItem(icon = null, title = "Orientation", summary = previewState.orientation, status = true, onClick = null, iconPainter = null)
                CustomCardItem(icon = null, title = "Stylus Support", summary = previewState.stylusSupport, status = true, onClick = null, iconPainter = null)
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                CustomCardItem(icon = null, title = "Check for dead pixels", summary = null, status = true, iconPainter = null, onClick = {})
                CustomCardItem(icon = null, title = "Fix dead pixels", summary = "This may take some time. During the process, the screen will display various colors and noises.", status = true, onClick = {}, iconPainter = null)
            }
        }
    }
}