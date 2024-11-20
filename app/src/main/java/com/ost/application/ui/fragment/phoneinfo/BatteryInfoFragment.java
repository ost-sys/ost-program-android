package com.ost.application.ui.fragment.phoneinfo;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ost.application.R;
import com.ost.application.databinding.FragmentBatteryInfoBinding;
import com.ost.application.ui.core.base.BaseFragment;


public class BatteryInfoFragment extends BaseFragment {

    private FragmentBatteryInfoBinding binding;
    private BroadcastReceiver batteryReceiver;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentBatteryInfoBinding.inflate(inflater, container, false);

        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                    updateBatteryInfo(intent);
                    getBatteryCapacity(context);
                }
            }
        };

        return binding.getRoot();
    }

    @SuppressLint({"UseCompatLoadingForDrawables", "SetTextI18n"})
    private void updateBatteryInfo(Intent intent) {
        double level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        float temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        int health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN);
        float voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        final Bundle extras = intent.getExtras();
        String batteryTech = (extras != null) ? extras.getString(BatteryManager.EXTRA_TECHNOLOGY) : null;

        String healthStatus;
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD:
                healthStatus = getString(R.string.good);
                break;
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                healthStatus = getString(R.string.overheat);
                break;
            case BatteryManager.BATTERY_HEALTH_DEAD:
                healthStatus = getString(R.string.dead);
                break;
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                healthStatus = getString(R.string.over_voltage);
                break;
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                healthStatus = getString(R.string.fail);
                break;
            case BatteryManager.BATTERY_HEALTH_COLD:
                healthStatus = getString(R.string.cold);
                break;
            default:
                healthStatus = getString(R.string.undefined);
                break;
        }

        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        String chargingStatus;
        if (plugged == BatteryManager.BATTERY_PLUGGED_AC) {
            chargingStatus = getString(R.string.charging);
            binding.batteryIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_oui_lightning));
            binding.batteryLevel.setTextColor(getResources().getColor(R.color.green));
            binding.batteryLevel.setText(chargingStatus + ": " + (int) level + "%");
        } else if (plugged == BatteryManager.BATTERY_PLUGGED_USB) {
            chargingStatus = getString(R.string.charging_via_usb);
            binding.batteryIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_oui_usb));
            binding.batteryLevel.setTextColor(getResources().getColor(R.color.blue));
            binding.batteryLevel.setText(chargingStatus + ": " + (int) level + "%");
        } else if (plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
            chargingStatus = getString(R.string.wireless_charging);
            binding.batteryIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_oui_lightning_outline));
            binding.batteryLevel.setTextColor(getResources().getColor(R.color.yellow));
            binding.batteryLevel.setText(chargingStatus + ": " + (int) level + "%");
        } else {
            chargingStatus = getString(R.string.discharging);
            if (level != -1) {
                int batteryPct = (int) level;
                if (batteryPct >= 85) {
                    binding.batteryIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_oui_battery_filled));
                    binding.batteryLevel.setTextColor(getResources().getColor(R.color.green));
                } else if (batteryPct > 15) {
                    binding.batteryIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_oui_batterylevel));
                    binding.batteryLevel.setTextColor(getResources().getColor(R.color.oui_primary_text_color));
                } else {
                    binding.batteryIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_oui_battery));
                    binding.batteryLevel.setTextColor(getResources().getColor(R.color.red));
                }
                binding.batteryLevel.setText(batteryPct + "%");
            }
        }

        binding.batteryStatus.setSummaryText(chargingStatus);
        binding.batteryHealth.setSummaryText(healthStatus);
        binding.batteryTemp.setSummaryText(temp / 10 + "°C");
        binding.batteryVoltage.setSummaryText(voltage / 1000 + "V");
        binding.batteryTechnology.setSummaryText(batteryTech);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireContext().registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public void onPause() {
        super.onPause();
        if (batteryReceiver != null) {
            requireContext().unregisterReceiver(batteryReceiver);
        }
    }

    @SuppressLint("PrivateApi")
    public double getBatteryCapacity(Context context) {
        Object mPowerProfile;
        double batteryCapacity = 0;
        final String POWER_PROFILE_CLASS = "com.android.internal.os.PowerProfile";

        try {
            mPowerProfile = Class.forName(POWER_PROFILE_CLASS)
                    .getConstructor(Context.class)
                    .newInstance(context);

            batteryCapacity = (double) Class
                    .forName(POWER_PROFILE_CLASS)
                    .getMethod("getBatteryCapacity")
                    .invoke(mPowerProfile);

        } catch (Exception e) {
            e.printStackTrace();
        }

        binding.batteryCapacity.setSummaryText((int) batteryCapacity + " " + getString(R.string.mah));
        return batteryCapacity;

    }

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_battery_info;
    }

    @Override
    public int getIconResId() {
        return dev.oneuiproject.oneui.R.drawable.ic_oui_batterylevel;
    }

    @Override
    public CharSequence getTitle() {
        return getString(R.string.battery);
    }
}