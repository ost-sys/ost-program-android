package com.ost.application.ui.screen.camerainfo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ost.application.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.atan
import kotlin.math.roundToInt

@Stable
data class CameraModuleInfo(
    val id: String,
    val type: String,
    val resolution: String = "N/A",
    val focalLengthInfo: String = "N/A",
    val fovInfo: String = "N/A",
    val stabilization: String = "N/A",
    val flashSupport: String = "N/A",
    val rawSupport: String = "N/A",
    val logicalMultiCam: Boolean = false,
    val physicalIds: List<String>? = null
)

@Stable
data class CameraInfoUiState(
    val cameraModules: List<CameraModuleInfo> = emptyList(),
    val moduleCountText: String = "Loading cameras...",
    val hasCameraPermission: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed class CameraInfoAction {
    object RequestCameraPermission : CameraInfoAction()
    data class ShowErrorToast(val message: String) : CameraInfoAction()
}

class CameraInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CameraInfoUiState())
    val uiState: StateFlow<CameraInfoUiState> = _uiState.asStateFlow()

    private val _action = MutableStateFlow<CameraInfoAction?>(null)
    val action: StateFlow<CameraInfoAction?> = _action

    init {
        checkPermissionAndLoadInfo()
    }

    fun checkPermissionAndLoadInfo() {
        val context = getApplication<Application>()
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        _uiState.update { it.copy(hasCameraPermission = hasPermission) }

        if (hasPermission) {
            loadCameraInfo()
        } else {
            _uiState.update { it.copy(isLoading = false, error = context.getString(R.string.camera_permission_required)) }
        }
    }

    fun requestCameraPermission() {
        _action.value = CameraInfoAction.RequestCameraPermission
    }

    fun clearAction() {
        _action.value = null
    }

    @SuppressLint("NewApi")
    private fun loadCameraInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, cameraModules = emptyList()) }
            val context = getApplication<Application>()
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

            if (cameraManager == null) {
                _uiState.update { it.copy(isLoading = false, error = context.getString(R.string.camera_manager_not_available)) }
                return@launch
            }

            val result: Pair<List<CameraModuleInfo>, String?> = withContext(Dispatchers.IO) {
                try {
                    val allCameraIds = cameraManager.cameraIdList.toList()
                    val allCharacteristics = allCameraIds.associateWith { id ->
                        try { cameraManager.getCameraCharacteristics(id) }
                        catch (e: Exception) { Log.w("CamVM", "Failed get characteristics for $id", e); null }
                    }.filterValues { it != null }.mapValues { it.value!! }

                    val modules = mutableListOf<CameraModuleInfo>()
                    val reportedPhysicalIds = mutableSetOf<String>()

                    val logicalCameraIds = allCharacteristics.filter { (_, chars) ->
                        chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                            ?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA) == true
                    }.keys

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        logicalCameraIds.forEach { logId ->
                            allCharacteristics[logId]?.physicalCameraIds?.forEach { physId ->
                                reportedPhysicalIds.add(physId)
                            }
                        }
                    }

                    val cameraIdsToShow = logicalCameraIds.toMutableSet()
                    allCharacteristics.keys.forEach { id ->
                        if (id !in logicalCameraIds && id !in reportedPhysicalIds) {
                            cameraIdsToShow.add(id)
                        }
                    }

                    val rearCamerasToShow = mutableListOf<Pair<String, CameraCharacteristics>>()
                    val frontCamerasToShow = mutableListOf<Pair<String, CameraCharacteristics>>()

                    cameraIdsToShow.forEach { id ->
                        val characteristics = allCharacteristics[id]
                        if (characteristics != null) {
                            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                            if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                                rearCamerasToShow.add(id to characteristics)
                            } else if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                                frontCamerasToShow.add(id to characteristics)
                            }
                        }
                    }

                    rearCamerasToShow.sortBy { (_, chars) -> getMinFocalLength(chars) ?: Float.MAX_VALUE }

                    rearCamerasToShow.forEachIndexed { index, (id, characteristics) ->
                        val type = when(index) {
                            0 -> context.getString(R.string.rear_facing_camera)
                            else -> "${context.getString(R.string.rear_facing_camera)} ${index + 1}"
                        }
                        modules.add(extractCameraInfo(id, type, characteristics, context))
                    }
                    frontCamerasToShow.forEachIndexed { index, (id, characteristics) ->
                        val type = if (frontCamerasToShow.size > 1) "${context.getString(R.string.front_camera)} ${index + 1}"
                        else context.getString(R.string.front_camera)
                        modules.add(extractCameraInfo(id, type, characteristics, context))
                    }

                    val moduleCountText = getModuleCountText(rearCamerasToShow.size, frontCamerasToShow.size, context)

                    modules to moduleCountText

                } catch (e: CameraAccessException) {
                    Log.e("CameraInfoVM", "Cannot access camera service.", e)
                    emptyList<CameraModuleInfo>() to context.getString(R.string.cannot_access_camera_service)
                } catch (e: Exception) {
                    Log.e("CameraInfoVM", "Error loading camera info.", e)
                    emptyList<CameraModuleInfo>() to context.getString(R.string.error_loading_camera_info)
                }
            }

            _uiState.update {
                it.copy(
                    cameraModules = result.first,
                    moduleCountText = result.second ?: context.getString(R.string.error),
                    isLoading = false,
                    error = if (result.first.isEmpty() && result.second != null) result.second else null
                )
            }
        }
    }

    private fun getModuleCountText(rearCount: Int, frontCount: Int, context: Context): String {
        return when {
            rearCount == 0 && frontCount == 0 -> context.getString(R.string.no_cameras_detected)
            rearCount > 0 && frontCount > 0 -> context.getString(R.string.multiple_cameras_detected, rearCount, frontCount)
            rearCount > 0 -> context.getString(R.string.rear_cameras_only_detected, rearCount)
            frontCount > 0 -> context.getString(R.string.front_cameras_only_detected, frontCount)
            else -> context.getString(R.string.error_counting_cameras)
        }
    }

    private fun getMinFocalLength(characteristics: CameraCharacteristics): Float? {
        return characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.minOrNull()
    }

    @SuppressLint("NewApi")
    private fun extractCameraInfo(
        cameraId: String,
        cameraType: String,
        characteristics: CameraCharacteristics,
        context: Context
    ): CameraModuleInfo {

        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val resolution = map?.getOutputSizes(android.graphics.ImageFormat.JPEG)
            ?.maxByOrNull { it.width * it.height }
            ?.let { "${it.width}x${it.height} (${String.format("%.1f", (it.width * it.height / 1_000_000.0))} MP)" }
            ?: context.getString(R.string.unknown)

        val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        val focalLengthInfo = focalLengths
            ?.joinToString { String.format("%.1f", it) + "mm" }
            ?: context.getString(R.string.unknown)

        val fovInfo = calculateFov(characteristics, context)

        val hasOis = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
            ?.any { it == CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON } ?: false
        val hasVideoStab = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
            ?.any { it != CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_OFF } ?: false
        val stabilization = when {
            hasOis && hasVideoStab -> "OIS + EIS"
            hasOis -> "OIS"
            hasVideoStab -> "EIS"
            else -> context.getString(R.string.unsupport)
        }

        val flashSupport = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)?.let {
            if (it) context.getString(R.string.support) else context.getString(R.string.unsupport)
        } ?: context.getString(R.string.unknown)

        val rawSupport = map?.outputFormats?.contains(android.graphics.ImageFormat.RAW_SENSOR)?.let {
            if (it) context.getString(R.string.support) else context.getString(R.string.unsupport)
        } ?: context.getString(R.string.unknown)

        val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        val isLogical = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA) ?: false
        val physicalIds = if (isLogical && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            characteristics.physicalCameraIds.toList()
        } else null

        return CameraModuleInfo(
            id = cameraId,
            type = cameraType,
            resolution = resolution,
            focalLengthInfo = focalLengthInfo,
            fovInfo = fovInfo,
            stabilization = stabilization,
            flashSupport = flashSupport,
            rawSupport = rawSupport,
            logicalMultiCam = isLogical,
            physicalIds = physicalIds
        )
    }

    @SuppressLint("NewApi")
    private fun calculateFov(characteristics: CameraCharacteristics, context: Context): String {
        val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        val focalLength = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull()

        if (sensorSize != null && focalLength != null && focalLength > 0) {
            val sensorWidth = sensorSize.width
            val sensorHeight = sensorSize.height
            val fovHorizontal = 2.0 * atan((sensorWidth / (2.0 * focalLength)).toDouble()) * (180.0 / Math.PI)
            val fovVertical = 2.0 * atan((sensorHeight / (2.0 * focalLength)).toDouble()) * (180.0 / Math.PI)
            return "${fovHorizontal.roundToInt()}° (H) / ${fovVertical.roundToInt()}° (V)"
        }
        return context.getString(R.string.unknown)
    }
}