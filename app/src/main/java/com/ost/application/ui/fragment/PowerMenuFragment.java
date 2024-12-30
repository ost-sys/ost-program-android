package com.ost.application.ui.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.MenuProvider;

import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.ost.application.R;
import com.ost.application.databinding.FragmentPowerMenuBinding;
import com.topjohnwu.superuser.Shell;

import com.ost.application.ui.core.base.BaseFragment;

public class PowerMenuFragment extends BaseFragment implements View.OnClickListener {

    private FragmentPowerMenuBinding binding;
    private long mLastClickTime;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPowerMenuBinding.inflate(inflater, container, false);

        shellResult();

        initContent();
        return binding.getRoot();

    }

    private void shellResult() {

        Shell.Result result = Shell.cmd("su").exec();
        if (result.isSuccess()) {
            binding.powerOff.setEnabled(true);
            binding.reboot.setEnabled(true);
            binding.recovery.setEnabled(true);
            binding.downloadMode.setEnabled(getSystemProperty("ro.product.system.brand").equals("samsung"));
            binding.fastboot.setEnabled(true);
            binding.fastbootd.setEnabled(true);

            binding.powerMenuText.setText(R.string.access_granted);
            binding.powerMenuText.setTextColor(getResources().getColor(R.color.green));
        } else {
            binding.powerOff.setEnabled(false);
            binding.reboot.setEnabled(false);
            binding.recovery.setEnabled(false);
            binding.downloadMode.setEnabled(false);
            binding.fastboot.setEnabled(false);
            binding.fastbootd.setEnabled(false);

            binding.powerMenuText.setText(R.string.access_denied);
            binding.powerMenuText.setTextColor(getResources().getColor(R.color.red));
        }
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

    private void initContent() {
        binding.powerOff.setOnClickListener(this);
        binding.reboot.setOnClickListener(this);
        binding.recovery.setOnClickListener(this);
        binding.downloadMode.setOnClickListener(this);
        binding.fastboot.setOnClickListener(this);
        binding.fastbootd.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        long uptimeMillis = SystemClock.uptimeMillis();
        if (uptimeMillis - mLastClickTime > 600L) {
            String message = null;
            String cmd = null;
            if (v.getId() == binding.powerOff.getId()) {
                message = getString(R.string.turn_off_q);
                cmd = "reboot -p";
            } else if (v.getId() == binding.reboot.getId()) {
                message = getString(R.string.reboot_system_q);
                cmd = "reboot";
            } else if (v.getId() == binding.recovery.getId()) {
                message = getString(R.string.reboot_recovery_q);
                cmd = "reboot recovery";
            } else if (v.getId() == binding.downloadMode.getId()) {
                message = getString(R.string.reboot_download_q);
                cmd = "reboot download";
            } else if (v.getId() == binding.fastboot.getId()) {
                message = getString(R.string.reboot_fastboot_q);
                cmd = "reboot bootloader";
            } else if (v.getId() == binding.fastbootd.getId()) {
                message = getString(R.string.reboot_fastbootd_q);
                cmd = "reboot fastboot";
            }

            if (message != null) {
                String finalCmd = cmd;
                AlertDialog.Builder alert = new AlertDialog.Builder(v.getContext());
                alert.setTitle(getString(R.string.attention))
                        .setMessage(message)
                        .setPositiveButton(R.string.yes, ((dialog, which) -> Shell.cmd(finalCmd).exec()))
                        .setNegativeButton(R.string.no, null)
                        .create()
                        .show();
            }
        }
        mLastClickTime = uptimeMillis;
    }

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_power_menu;
    }

    @Override
    public int getIconResId() {
        return dev.oneuiproject.oneui.R.drawable.ic_oui_power;
    }

    @Override
    public CharSequence getTitle() {
        return getString(R.string.power_menu);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            requireActivity().addMenuProvider(menuProvider);
        } else {
            requireActivity().removeMenuProvider(menuProvider);
        }
    }

    private final MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            MenuItem menuItem = menu.findItem(R.id.menu_check_root);
            menuItem.setVisible(true);
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem item) {
            if (item.getItemId() == R.id.menu_check_root) {
                shellResult();
            }
            return false;
        }
    };
}