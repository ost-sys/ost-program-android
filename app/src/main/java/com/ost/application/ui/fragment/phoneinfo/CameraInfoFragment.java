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
                binding.cameraBackResolution.setSummary(getResolution(cameraManager, rearCameraId));
                binding.cameraBackFocalLenght.setSummary(getFocalLength(cameraManager, rearCameraId));
                binding.cameraBackStabilization.setSummary(getStabilizationSupport(cameraManager, rearCameraId));
                binding.cameraBackFlash.setSummary(getFlashSupport(cameraManager, rearCameraId));
            }
            String secondRearCameraId = getSecondRearCameraId(cameraManager);
            if (secondRearCameraId != null) {
                binding.cameraBackSecondResolution.setSummary(getResolution(cameraManager, secondRearCameraId));
                binding.cameraBackSecondFocalLenght.setSummary(getFocalLength(cameraManager, secondRearCameraId));
                binding.cameraBackSecondStabilization.setSummary(getStabilizationSupport(cameraManager, secondRearCameraId));
                binding.cameraBackSecondFlash.setSummary(getFlashSupport(cameraManager, secondRearCameraId));
            } else {
                binding.second.setVisibility(View.GONE);
                binding.cameraBackSecondResolution.setVisibility(View.GONE);
                binding.cameraBackSecondFocalLenght.setVisibility(View.GONE);
                binding.cameraBackSecondStabilization.setVisibility(View.GONE);
                binding.cameraBackSecondFlash.setVisibility(View.GONE);
            }
            String thirdRearCameraId = getThirdRearCameraId(cameraManager);
            if (thirdRearCameraId != null) {
                binding.cameraBackThirdResolution.setSummary(getResolution(cameraManager, thirdRearCameraId));
                binding.cameraBackThirdFocalLenght.setSummary(getFocalLength(cameraManager, thirdRearCameraId));
                binding.cameraBackThirdStabilization.setSummary(getStabilizationSupport(cameraManager, thirdRearCameraId));
                binding.cameraBackThirdFlash.setSummary(getFlashSupport(cameraManager, thirdRearCameraId));
            } else {
                binding.third.setVisibility(View.GONE);
                binding.cameraBackThirdResolution.setVisibility(View.GONE);
                binding.cameraBackThirdFocalLenght.setVisibility(View.GONE);
                binding.cameraBackThirdStabilization.setVisibility(View.GONE);
                binding.cameraBackThirdFlash.setVisibility(View.GONE);
            }
            String frontCameraId = findCameraId(cameraManager, CameraCharacteristics.LENS_FACING_FRONT);
            if (frontCameraId != null) {
                binding.cameraFrontResolution.setSummary(getResolution(cameraManager, frontCameraId));
                binding.cameraFrontFocalLenght.setSummary(getFocalLength(cameraManager, frontCameraId));
                binding.cameraFrontStabilization.setSummary(getStabilizationSupport(cameraManager, frontCameraId));
                binding.cameraFrontFlash.setSummary(getFlashSupport(cameraManager, frontCameraId));
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
        return (stabilizationModes != null && stabilizationModes.length > 0) ? getString(R.string.support) : getString(R.string.unsupport);
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
        return (flashAvailable != null && flashAvailable) ? getString(R.string.support) : getString(R.string.unsupport);
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