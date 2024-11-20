package com.ost.application.ui.fragment;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.menu.SeslMenuItem;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;

import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.ost.application.R;
import com.ost.application.databinding.FragmentPowerMenuBinding;
import com.ost.application.ui.core.widget.CardView;
import com.topjohnwu.superuser.Shell;

import com.ost.application.ui.core.base.BaseFragment;

import java.io.IOException;

import dev.oneuiproject.oneui.widget.Separator;
import dev.oneuiproject.oneui.widget.TipPopup;
import dev.oneuiproject.oneui.widget.Toast;

public class PowerMenuFragment extends BaseFragment implements View.OnClickListener {

    private FragmentPowerMenuBinding binding;
    CardView powerOff, reboot, recovery, download_mode, fastboot, fastbootd;
    private long mLastClickTime;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPowerMenuBinding.inflate(inflater, container, false);

        shellResult();

        initContent();
        return binding.getRoot();

    }

    private void shellResult() {
        powerOff = binding.powerOff;
        reboot = binding.reboot;
        recovery = binding.recovery;
        download_mode = binding.downloadMode;
        fastboot = binding.fastboot;
        fastbootd = binding.fastbootd;

        Shell.Result result = Shell.cmd("su").exec();
        if (result.isSuccess()) {
            powerOff.setEnabled(true);
            reboot.setEnabled(true);
            recovery.setEnabled(true);
            download_mode.setEnabled(getSystemProperty("ro.product.system.brand").equals("samsung"));
            fastboot.setEnabled(true);
            fastbootd.setEnabled(true);

            binding.powerMenuText.setText(R.string.access_granted);
            binding.powerMenuText.setTextColor(getResources().getColor(R.color.green));
        } else {
            powerOff.setEnabled(false);
            reboot.setEnabled(false);
            recovery.setEnabled(false);
            download_mode.setEnabled(false);
            fastboot.setEnabled(false);
            fastbootd.setEnabled(false);

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
        powerOff.setOnClickListener(this);
        reboot.setOnClickListener(this);
        recovery.setOnClickListener(this);
        download_mode.setOnClickListener(this);
        fastboot.setOnClickListener(this);
        fastbootd.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        long uptimeMillis = SystemClock.uptimeMillis();
        if (uptimeMillis - mLastClickTime > 600L) {
            String message = null;
            String cmd = null;
            if (v.getId() == powerOff.getId()) {
                message = getString(R.string.turn_off_q);
//                cmd = "reboot -p";
            } else if (v.getId() == reboot.getId()) {
                message = getString(R.string.reboot_system_q);
                cmd = "reboot";
            } else if (v.getId() == recovery.getId()) {
                message = getString(R.string.reboot_recovery_q);
                cmd = "reboot recovery";
            } else if (v.getId() == download_mode.getId()) {
                message = getString(R.string.reboot_download_q);
                cmd = "reboot download";
            } else if (v.getId() == fastboot.getId()) {
                message = getString(R.string.reboot_fastboot_q);
                cmd = "reboot bootloader";
            } else if (v.getId() == fastbootd.getId()) {
                message = getString(R.string.reboot_fastbootd_q);
                cmd = "reboot fastboot";
            }

            if (message != null && cmd != null) {
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

    private MenuProvider menuProvider = new MenuProvider() {
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