package com.ost.application.ui.fragment.phoneinfo;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jaredrummler.android.device.DeviceName;
import com.ost.application.R;
import com.ost.application.databinding.FragmentCpuInfoBinding;
import com.ost.application.ui.core.base.BaseFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.regex.Pattern;

import com.ost.application.sysinfo.Cpu;

public class CPUInfoFragment extends BaseFragment {

    private FragmentCpuInfoBinding binding;
    private Cpu cpu;

    @SuppressLint("NewApi")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCpuInfoBinding.inflate(inflater, container, false);
        DeviceName.init(getActivity());

        cpu = new Cpu();

        loadAboutDevice();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.cpuManufacturer.setSummary(Build.SOC_MANUFACTURER);
        } else {
            binding.cpuManufacturer.setVisibility(View.GONE);
        }

        binding.cpuName.setSummary(getCpuName());
        binding.cpuAbi.setSummary(Arrays.toString(Build.SUPPORTED_ABIS));
        binding.cpuCores.setSummary(String.valueOf(getNumberOfCores()));
        binding.cpuCoresClock.setSummary(cpu.getClock_speed());

        return binding.getRoot();
    }

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_cpu_info;
    }

    @Override
    public int getIconResId() {
        return dev.oneuiproject.oneui.R.drawable.ic_oui_enhanced_processing;
    }

    @Override
    public CharSequence getTitle() {
        return getString(R.string.cpu) + " " + "(" + getString(R.string.beta) + ")";
    }

    public String getCpuName() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return Build.SOC_MODEL;
        } else {
            try {
                FileInputStream fis = new FileInputStream("/proc/cpuinfo");
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
                    return reader.lines()
                            .filter(line -> line.startsWith("Hardware"))
                            .findFirst()
                            .map(line -> line.substring(line.indexOf(":") + 1).trim())
                            .orElse(getString(R.string.unknown_cpu));
                }
            } catch (Exception e) {
                return getString(R.string.error_cpu_info);
            }
        }
    }

    private int getNumberOfCores() {
        if (Build.VERSION.SDK_INT >= 17) {
            return Runtime.getRuntime().availableProcessors();
        } else {
            return getNumCoresOldPhones();
        }
    }
    private int getNumCoresOldPhones() {
        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                return Pattern.matches("cpu[0-9]+", pathname.getName());
            }
        }

        try {
            File dir = new File("/sys/devices/system/cpu/");
            File[] files = dir.listFiles(new CpuFilter());
            return files.length;
        } catch (Exception e) {
            return 1;
        }
    }
    private void loadAboutDevice() {
        cpu = new Cpu();
        int cores = Runtime.getRuntime().availableProcessors();
        cpu.setModel(Build.MANUFACTURER);
        cpu.setCores(String.valueOf(cores));
        cpu.setClock_speed(getClockMinSpeed() / 1000 + " " + getString(R.string.mhz) + " - " + getClockMaxSpeed() / 1000000 + " " + getString(R.string.ghz));
        cpu.setGpu_vendor(Build.HARDWARE);
    }

    private float getClockMaxSpeed() {
        try {
            String[] args = {"/system/bin/cat", "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"};
            ProcessBuilder builder = new ProcessBuilder(args);
            if ("/system/bin/" != null)
                builder.directory(new File("/system/bin/"));
            builder.redirectErrorStream(true);
            Process process = null;
            try {
                process = builder.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            InputStream in = process.getInputStream();
            String temp = "";
            byte[] re = new byte[1024];
            try {
                while (in.read(re) != -1) {
                    temp = temp + new String(re) + "\n";
                }
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return Float.parseFloat(temp);
        } catch (NumberFormatException e) {
            Log.e("TAG", "File is not detected");
        }

        return 0;
    }

    private float getClockMinSpeed() {
        try {
            String[] args = {"/system/bin/cat", "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq"};
            ProcessBuilder builder = new ProcessBuilder(args);
            if ("/system/bin/" != null)
                builder.directory(new File("/system/bin/"));
            builder.redirectErrorStream(true);
            Process process = null;
            try {
                process = builder.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            InputStream in = process.getInputStream();
            String temp = "";
            byte[] re = new byte[1024];
            try {
                while (in.read(re) != -1) {
                    temp = temp + new String(re) + "\n";
                }
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return Float.parseFloat(temp);
        } catch (NumberFormatException e) {
            Log.e("TAG", "File is not found");
        }
        return 0;
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}