package com.ost.application.ui.fragment.phoneinfo;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Bundle;

import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.RequiresApi;

import com.ost.application.R;
import com.ost.application.databinding.FragmentCameraInfoBinding;
import com.ost.application.ui.core.base.BaseFragment;

import java.util.Arrays;

public class CameraInfoFragment extends BaseFragment {

    private FragmentCameraInfoBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCameraInfoBinding.inflate(inflater, container, false);

        displayCameraInfo();

        return binding.getRoot();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void displayCameraInfo() {
        CameraManager cameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);

        try {
            String rearCameraId = findCameraId(cameraManager, CameraCharacteristics.LENS_FACING_BACK);
            if (rearCameraId != null) {
                binding.cameraBackResolution.setSummaryText(getResolution(cameraManager, rearCameraId));
                binding.cameraBackFocalLenght.setSummaryText(getFocalLength(cameraManager, rearCameraId));
                binding.cameraBackStabilization.setSummaryText(getStabilizationSupport(cameraManager, rearCameraId));
                binding.cameraBackFlash.setSummaryText(getFlashSupport(cameraManager, rearCameraId));
            }
            String secondRearCameraId = getSecondRearCameraId(cameraManager);
            if (secondRearCameraId != null) {
                binding.cameraBackSecondResolution.setSummaryText(getResolution(cameraManager, secondRearCameraId));
                binding.cameraBackSecondFocalLenght.setSummaryText(getFocalLength(cameraManager, secondRearCameraId));
                binding.cameraBackSecondStabilization.setSummaryText(getStabilizationSupport(cameraManager, secondRearCameraId));
                binding.cameraBackSecondFlash.setSummaryText(getFlashSupport(cameraManager, secondRearCameraId));
            } else {
                binding.second.setVisibility(View.GONE);
                binding.cameraBackSecondResolution.setVisibility(View.GONE);
                binding.cameraBackSecondFocalLenght.setVisibility(View.GONE);
                binding.cameraBackSecondStabilization.setVisibility(View.GONE);
                binding.cameraBackSecondFlash.setVisibility(View.GONE);
            }
            String thirdRearCameraId = getThirdRearCameraId(cameraManager);
            if (thirdRearCameraId != null) {
                binding.cameraBackThirdResolution.setSummaryText(getResolution(cameraManager, thirdRearCameraId));
                binding.cameraBackThirdFocalLenght.setSummaryText(getFocalLength(cameraManager, thirdRearCameraId));
                binding.cameraBackThirdStabilization.setSummaryText(getStabilizationSupport(cameraManager, thirdRearCameraId));
                binding.cameraBackThirdFlash.setSummaryText(getFlashSupport(cameraManager, thirdRearCameraId));
            } else {
                binding.third.setVisibility(View.GONE);
                binding.cameraBackThirdResolution.setVisibility(View.GONE);
                binding.cameraBackThirdFocalLenght.setVisibility(View.GONE);
                binding.cameraBackThirdStabilization.setVisibility(View.GONE);
                binding.cameraBackThirdFlash.setVisibility(View.GONE);
            }
            String frontCameraId = findCameraId(cameraManager, CameraCharacteristics.LENS_FACING_FRONT);
            if (frontCameraId != null) {
                binding.cameraFrontResolution.setSummaryText(getResolution(cameraManager, frontCameraId));
                binding.cameraFrontFocalLenght.setSummaryText(getFocalLength(cameraManager, frontCameraId));
                binding.cameraFrontStabilization.setSummaryText(getStabilizationSupport(cameraManager, frontCameraId));
                binding.cameraFrontFlash.setSummaryText(getFlashSupport(cameraManager, frontCameraId));
            }
            boolean hasMultipleRearCameras = hasMultipleRearCameras(cameraManager);
            binding.cameraModules.setText(hasMultipleRearCameras ? getString(R.string.multiple_rear_cameras_detected) : getString(R.string.single_rear_camera));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private String findCameraId(CameraManager cameraManager, int lensFacing) throws Exception {
        for (String cameraId : cameraManager.getCameraIdList()) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == lensFacing) {
                return cameraId;
            }
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private String getSecondRearCameraId(CameraManager cameraManager) throws Exception {
        String[] cameraIds = cameraManager.getCameraIdList();
        int rearCamerasCount = 0;

        for (String cameraId : cameraIds) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                rearCamerasCount++;
                if (rearCamerasCount == 2) {
                    return cameraId;
                }
            }
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private String getThirdRearCameraId(CameraManager cameraManager) throws Exception {
        String[] cameraIds = cameraManager.getCameraIdList();
        int rearCamerasCount = 0;

        for (String cameraId : cameraIds) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                rearCamerasCount++;
                if (rearCamerasCount == 3) {
                    return cameraId;
                }
            }
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private String getResolution(CameraManager cameraManager, String cameraId) throws Exception {
        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map != null) {
            Size[] sizes = map.getOutputSizes(android.graphics.ImageFormat.JPEG);
            if (sizes != null && sizes.length > 0) {
                Size maxSize = sizes[0];
                for (Size size : sizes) {
                    if (size.getWidth() * size.getHeight() > maxSize.getWidth() * maxSize.getHeight()) {
                        maxSize = size;
                    }
                }
                return maxSize.getWidth() + " * " + maxSize.getHeight();
            }
        }
        return getString(R.string.unknown);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private String getFocalLength(CameraManager cameraManager, String cameraId) throws Exception {
        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
        float[] focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
        return (focalLengths != null && focalLengths.length > 0) ? Arrays.toString(focalLengths) : getString(R.string.unknown);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private String getStabilizationSupport(CameraManager cameraManager, String cameraId) throws Exception {
        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
        int[] stabilizationModes = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES);
        return (stabilizationModes != null && stabilizationModes.length > 0) ? getString(R.string.supported) : getString(R.string.unsupported);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private boolean hasMultipleRearCameras(CameraManager cameraManager) throws Exception {
        String[] cameraIds = cameraManager.getCameraIdList();
        int rearCamerasCount = 0;

        for (String cameraId : cameraIds) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                rearCamerasCount++;
            }
        }
        return rearCamerasCount > 1;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private String getFlashSupport(CameraManager cameraManager, String cameraId) throws Exception {
        CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
        Boolean flashAvailable = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        return (flashAvailable != null && flashAvailable) ? getString(R.string.supported) : getString(R.string.unsupported);
    }

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_camera_info;
    }

    @Override
    public int getIconResId() {
        return dev.oneuiproject.oneui.R.drawable.ic_oui_camera_outline;
    }

    @Override
    public CharSequence getTitle() {
        return getString(R.string.camera);
    }

}