package com.ost.application.ui.fragment.phoneinfo;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.jaredrummler.android.device.DeviceName;
import com.ost.application.R;
import com.ost.application.databinding.FragmentDefaultInfoBinding;
import com.ost.application.ui.core.base.BaseFragment;

public class DefaultInfoFragment extends BaseFragment implements View.OnClickListener{

    private FragmentDefaultInfoBinding binding;
    private Handler handler;
    private Runnable updateRunnable;
    private long mLastClickTime;
    private int clickCount = 0;


    @SuppressLint({"UseCompatLoadingForDrawables", "ObsoleteSdkInt"})
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDefaultInfoBinding.inflate(inflater, container, false);
        handler = new Handler(Looper.getMainLooper());
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateInfo();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateRunnable);
        return binding.getRoot();
    }

    private void updateInfo() {
        assert getActivity() != null;
        ActivityManager actManager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        actManager.getMemoryInfo(memInfo);
        double availMemory = memInfo.availMem / (1024.0 * 1024 * 1024);
        double totalMemory = memInfo.totalMem / (1024.0 * 1024 * 1024);

        StatFs internalStatFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
        double internalTotal;
        double internalFree;

        StatFs externalStatFs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
        double externalTotal;
        double externalFree;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            internalTotal = ((internalStatFs.getBlockCountLong() * internalStatFs.getBlockSizeLong()) / (1024.0 * 1024 * 1024));
            internalFree = (internalStatFs.getAvailableBlocksLong() * internalStatFs.getBlockSizeLong()) / (1024.0 * 1024 * 1024);
            externalTotal = (externalStatFs.getBlockCountLong() * externalStatFs.getBlockSizeLong()) / (1024.0 * 1024 * 1024);
            externalFree = (externalStatFs.getAvailableBlocksLong() * externalStatFs.getBlockSizeLong()) / (1024.0 * 1024 * 1024);
        } else {
            internalTotal = ((long) internalStatFs.getBlockCount() * (long) internalStatFs.getBlockSize()) / (1024.0 * 1024 * 1024);
            internalFree = ((long) internalStatFs.getAvailableBlocks() * (long) internalStatFs.getBlockSize()) / (1024.0 * 1024 * 1024);
            externalTotal = ((long) externalStatFs.getBlockCount() * (long) externalStatFs.getBlockSize()) / (1024.0 * 1024 * 1024);
            externalFree = ((long) externalStatFs.getAvailableBlocks() * (long) externalStatFs.getBlockSize()) / (1024.0 * 1024 * 1024);
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
            if (!isAdded()) return;

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            FingerprintManager fingerprintManager = getActivity().getSystemService(FingerprintManager.class);

            if (fingerprintManager != null && fingerprintManager.isHardwareDetected()) {
                if (fingerprintManager.hasEnrolledFingerprints()) {
                    binding.aboutPhoneFingerprintScanner.setSummaryText(getString(R.string.supported) + "\n" + getString(R.string.fingers_registered));
                } else {
                    binding.aboutPhoneFingerprintScanner.setSummaryText(getString(R.string.supported) + "\n" + getString(R.string.fingers_not_registered));
                }
            } else {
                binding.aboutPhoneFingerprintScanner.setSummaryText(getString(R.string.unsupported));
            }
        }
        binding.aboutPhoneAndroid.setOnClickListener(this);
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
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(updateRunnable);
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

    @Override
    public void onClick(View v) {
        clickCount++;
        handler.removeCallbacks(resetClickCountRunnable);

        if (clickCount == 3) {
            performAction();
            clickCount = 0;
        } else {
            int maxClickTime = 1000;
            handler.postDelayed(resetClickCountRunnable, maxClickTime);
        }
    }

    private void performAction() {
        long uptimeMillis = SystemClock.uptimeMillis();
        if (uptimeMillis - mLastClickTime > 600L) {
            String activity = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                activity = "com.android.egg.landroid.MainActivity";
            } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Toast.makeText(getActivity(), "Easter Egg is not founded in System UI or in Android Easter Egg application", Toast.LENGTH_SHORT).show();
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                activity = "com.android.egg.quares.QuaresActivity";
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
                activity = "com.android.egg.paint.PaintActivity";
            }
            if (activity != null) {
                startActivity(Intent.makeMainActivity(new ComponentName("com.android.egg", activity)));
            }
        }
    }

    private final Runnable resetClickCountRunnable = () -> clickCount = 0;
}
