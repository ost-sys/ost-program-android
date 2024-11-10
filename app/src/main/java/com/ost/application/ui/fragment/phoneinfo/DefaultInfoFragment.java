package com.ost.application.ui.fragment.phoneinfo;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.jaredrummler.android.device.DeviceName;
import com.ost.application.R;
import com.ost.application.databinding.FragmentDefaultInfoBinding;
import com.ost.application.ui.core.base.BaseFragment;


public class DefaultInfoFragment extends BaseFragment {

    private FragmentDefaultInfoBinding binding;

    @SuppressLint("UseCompatLoadingForDrawables")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDefaultInfoBinding.inflate(inflater, container, false);

        assert getActivity() != null;
        ActivityManager actManager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        actManager.getMemoryInfo(memInfo);
        double availMemory = memInfo.availMem / (1024.0 * 1024 * 1024);
        double totalMemory = memInfo.totalMem / (1024.0 * 1024 * 1024);

        StatFs internalStatFs = new StatFs(Environment.getRootDirectory().getAbsolutePath() );
        double internalTotal;
        double internalFree;

        StatFs externalStatFs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath() );
        double externalTotal;
        double externalFree;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            internalTotal = ((internalStatFs.getBlockCountLong() * internalStatFs.getBlockSizeLong()) / (1024.0 * 1024 * 1024));
            internalFree = (internalStatFs.getAvailableBlocksLong() * internalStatFs.getBlockSizeLong()) / (1024.0 * 1024 * 1024);
            externalTotal = (externalStatFs.getBlockCountLong() * externalStatFs.getBlockSizeLong()) / (1024.0 * 1024 * 1024);
            externalFree = (externalStatFs.getAvailableBlocksLong() * externalStatFs.getBlockSizeLong()) / (1024.0 * 1024 * 1024);
        }
        else {
            internalTotal = ((long)internalStatFs.getBlockCount() * (long)internalStatFs.getBlockSize()) / (1024.0 * 1024 * 1024);
            internalFree = ((long)internalStatFs.getAvailableBlocks() * (long)internalStatFs.getBlockSize()) / (1024.0 * 1024 * 1024);
            externalTotal = ((long)externalStatFs.getBlockCount() * (long)externalStatFs.getBlockSize()) / (1024.0 * 1024 * 1024);
            externalFree = ((long)externalStatFs.getAvailableBlocks() * (long)externalStatFs.getBlockSize()) / (1024.0 * 1024 * 1024);
        }

        double total = internalTotal + externalTotal;
        double free = internalFree + externalFree;
        double used = total - free;

        String availMemoryString = String.format("%.1f", availMemory);
        String totalMemoryString = String.format("%.1f", totalMemory);
        String totalString = String.format("%.1f", total);
        String freeString = String.format("%.1f", free);
        String usedString = String.format("%.1f", used);

        DeviceName.init(getActivity());
        DeviceName.with(getActivity()).request((info, error) -> {
            String name = info.marketName;
            String model = info.model;
            String codename = info.codename;

            binding.aboutPhoneName.setText(name);
            binding.aboutPhoneAndroid.setSummaryText(Build.VERSION.RELEASE);
            binding.aboutPhoneBrand.setSummaryText(Build.BRAND);
            binding.aboutPhoneBoard.setSummaryText(Build.BOARD);
            binding.aboutPhoneModel.setSummaryText(model);
            binding.aboutPhoneCodename.setSummaryText(codename);
            binding.aboutPhoneBuildNumber.setSummaryText(getSystemProperty("ro.system.build.id"));
            binding.aboutPhoneSdk.setSummaryText(Build.VERSION.SDK);
            if (getSystemProperty("ro.build.characteristics").equals("phone")) {
                binding.aboutPhoneDevice.setSummaryText(getString(R.string.phone));
            } else if (getSystemProperty("ro.build.characteristics").equals("tablet")) {
                binding.aboutPhoneDevice.setSummaryText(getString(R.string.tablet));
            } else {
                binding.aboutPhoneDevice.setSummaryText(getString(R.string.device) + " (" + getSystemProperty("ro.build.characteristics") + ")");
            }
            binding.aboutPhoneRam.setSummaryText(getString(R.string.available) + ": " + availMemoryString + " " + getString(R.string.gb) + "\n" + getString(R.string.total) + ": " + totalMemoryString + " " + getString(R.string.gb));
            binding.aboutPhoneRom.setSummaryText(getString(R.string.total) + ": " + totalString + " " + getString(R.string.gb) + "\n" + getString(R.string.available) + ": " + freeString + " " + getString(R.string.gb) + "\n" + getString(R.string.used) + ": " + usedString + " " + getString(R.string.gb));
            binding.aboutPhoneFingerprint.setSummaryText(Build.FINGERPRINT);
        });

        if (getSystemProperty("ro.build.characteristics").equals("phone")) {
            binding.aboutPhoneImage.setImageDrawable(getResources().getDrawable(dev.oneuiproject.oneui.R.drawable.ic_oui_device_outline));
        } else if (getSystemProperty("ro.build.characteristics").equals("tablet")) {
            binding.aboutPhoneImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_oui_tablet_outline));
        } else {
            binding.aboutPhoneImage.setImageDrawable(getResources().getDrawable(dev.oneuiproject.oneui.R.drawable.ic_oui_page_settings));
        }

        FingerprintManager fingerprintManager = (FingerprintManager) getActivity().getSystemService(Context.FINGERPRINT_SERVICE);
        if (fingerprintManager.isHardwareDetected()) {
            binding.aboutPhoneFingerprintScanner.setSummaryText(getString(R.string.supported));
        } else {
            binding.aboutPhoneFingerprintScanner.setSummaryText(getString(R.string.unsupported));
        }

        return binding.getRoot();
    }
    @SuppressLint("PrivateApi")
    public String getSystemProperty(String key) {
        String value = null;

        try {
            value = (String) Class.forName("android.os.SystemProperties")
                    .getMethod("get", String.class).invoke(null, key);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return value;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_default_info;
    }

    @Override
    public int getIconResId() {
        if (getSystemProperty("ro.build.characteristics").equals("phone")) {
            return dev.oneuiproject.oneui.R.drawable.ic_oui_device_outline;
        } else if (getSystemProperty("ro.build.characteristics").equals("tablet")) {
            return R.drawable.ic_oui_tablet_outline;
        }
        return dev.oneuiproject.oneui.R.drawable.ic_oui_page_settings;
    }

    @Override
    public CharSequence getTitle() {
        if (getSystemProperty("ro.build.characteristics").equals("phone")) {
            return getString(R.string.about_phone);
        } else if (getSystemProperty("ro.build.characteristics").equals("tablet")) {
            return getString(R.string.about_tablet);
        } else {
            return getString(R.string.about_device);
        }
    }
}