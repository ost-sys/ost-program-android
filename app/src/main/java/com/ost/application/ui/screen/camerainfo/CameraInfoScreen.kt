@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.ost.application.ui.screen.camerainfo

import android.Manifest
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.ost.application.utils.SectionTitle
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
fun CameraInfoScreen(
    modifier: Modifier = Modifier,
    viewModel: CameraInfoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.checkPermissionAndLoadInfo()
        } else {
            // Обновляем UI, чтобы показать, что пермишен не выдан
            viewModel.checkPermissionAndLoadInfo()
        }
    }

    LaunchedEffect(uiState.hasCameraPermission) {
        if (!uiState.hasCameraPermission) {
            // Можно добавить логику автоматического запроса или оставить только кнопку
            // viewModel.requestCameraPermission()
        }
    }

    LaunchedEffect(viewModel.action) {
        viewModel.action.filterNotNull().onEach { action ->
            when (action) {
                is CameraInfoAction.RequestCameraPermission -> {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
                is CameraInfoAction.ShowErrorToast -> {
                    // Показываем тост
                }
            }
            viewModel.clearAction()
        }.launchIn(this)
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // Делаем колонку скроллящейся
            .padding(bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_camera_24dp),
                contentDescription = stringResource(R.string.camera),
                modifier = Modifier.size(80.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = uiState.moduleCountText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (!uiState.isLoading && !uiState.hasCameraPermission) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text(stringResource(R.string.grant_camera_permission))
                }
            }
            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
        uiState.cameraModules.forEach { cameraInfo ->
            key(cameraInfo.id) {
                CameraModuleSection(cameraInfo = cameraInfo)
            }
        }
    }
}

@Composable
fun CameraModuleSection(cameraInfo: CameraModuleInfo) {
    Column {
        SectionTitle(cameraInfo.type)
        CustomCardItem(
            icon = null, iconPainter = null,
            title = stringResource(R.string.camera_resolution),
            summary = cameraInfo.resolution,
            status = true, onClick = null
        )
        CustomCardItem(
            icon = null, iconPainter = null,
            title = stringResource(R.string.focal_lenght),
            summary = cameraInfo.focalLengthInfo,
            status = true, onClick = null
        )
        CustomCardItem(
            icon = null, iconPainter = null,
            title = stringResource(R.string.field_of_view),
            summary = cameraInfo.fovInfo,
            status = true, onClick = null
        )
        CustomCardItem(
            icon = null, iconPainter = null,
            title = stringResource(R.string.stabilization_support),
            summary = cameraInfo.stabilization,
            status = true, onClick = null
        )
        CustomCardItem(
            icon = null, iconPainter = null,
            title = stringResource(R.string.flash),
            summary = cameraInfo.flashSupport,
            status = true, onClick = null
        )
        CustomCardItem(
            icon = null, iconPainter = null,
            title = stringResource(R.string.raw_support),
            summary = cameraInfo.rawSupport,
            status = true, onClick = null
        )
        if (cameraInfo.logicalMultiCam && !cameraInfo.physicalIds.isNullOrEmpty()) {
            CustomCardItem(
                icon = null, iconPainter = null,
                title = stringResource(R.string.logical_camera),
                summary = stringResource(R.string.physical_ids, cameraInfo.physicalIds.joinToString()),
                status = true, onClick = null
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}


@Preview(showBackground = true)
@Composable
private fun CameraInfoScreenPreview() {
    OSTToolsTheme {
        val previewState = CameraInfoUiState(
            cameraModules = listOf(
                CameraModuleInfo(id = "0", type = "Rear Camera (Main)", resolution = "4032x3024 (12.2 MP)", focalLengthInfo = "4.3mm", fovInfo = "78° (H) / 62° (V)", stabilization = "OIS + EIS", flashSupport = "Supported", rawSupport = "Supported"),
                CameraModuleInfo(id = "2", type = "Rear Camera 2 (Ultrawide)", resolution = "4000x3000 (12.0 MP)", focalLengthInfo = "1.8mm", fovInfo = "120° (H) / 90° (V)", stabilization = "EIS", flashSupport = "Unsupported", rawSupport = "Supported"),
                CameraModuleInfo(id = "1", type = "Front Camera", resolution = "3840x2160 (8.3 MP)", focalLengthInfo = "2.8mm", fovInfo = "85° (H) / 68° (V)", stabilization = "Unsupported", flashSupport = "Unsupported", rawSupport = "Unsupported")
            ),
            moduleCountText = "2 Rear, 1 Front cameras detected", // Обновлено
            hasCameraPermission = true,
            isLoading = false,
            error = null
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical=24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.ic_camera_24dp), "", Modifier.size(100.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(previewState.moduleCountText, fontSize=20.sp, fontWeight=FontWeight.Bold)
                }
            }
            items(previewState.cameraModules) { module ->
                CameraModuleSection(cameraInfo = module)
            }
        }
    }
}