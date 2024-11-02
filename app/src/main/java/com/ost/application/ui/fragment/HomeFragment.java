package com.ost.application.ui.fragment;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.ost.application.R;
import com.ost.application.databinding.FragmentHomeBinding;
import com.ost.application.ui.core.widget.CardView;
import com.topjohnwu.superuser.Shell;

import com.ost.application.ui.core.base.BaseFragment;

import dev.oneuiproject.oneui.widget.Separator;
import dev.oneuiproject.oneui.widget.TipPopup;

public class HomeFragment extends BaseFragment implements View.OnClickListener {

    private FragmentHomeBinding binding;
    CardView powerOff, reboot, recovery, download_mode, fastboot, fastbootd;
    private long mLastClickTime;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        Separator separatorPowerMenu = binding.separatorPowerMenu;
        separatorPowerMenu.post(() -> {
            if (!(Shell.cmd("su").exec()).isSuccess()) {
                TipPopup tipPopup = new TipPopup(separatorPowerMenu, TipPopup.MODE_NORMAL);
                tipPopup.setMessage(getString(R.string.feature_unavailable_root));
                tipPopup.setExpanded(true);
                tipPopup.show(TipPopup.DIRECTION_BOTTOM_RIGHT);
            }
        });

        powerOff = binding.powerOff;
        reboot = binding.reboot;
        recovery = binding.recovery;
        download_mode = binding.downloadMode;
        fastboot = binding.fastboot;
        fastbootd = binding.fastbootd;

        Shell.Result result = Shell.cmd("su").exec();
        if (result.isSuccess()) {
            powerOff.setEnabled(true);
            powerOff.setEnabled(true);
            reboot.setEnabled(true);
            recovery.setEnabled(true);
            download_mode.setEnabled(getSystemProperty("ro.product.system.brand").equals("samsung"));
            fastboot.setEnabled(true);
            fastbootd.setEnabled(true);
        } else {
            powerOff.setEnabled(false);
            reboot.setEnabled(false);
            recovery.setEnabled(false);
            download_mode.setEnabled(false);
            fastboot.setEnabled(false);
            fastbootd.setEnabled(false);
        }

        initContent();
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

    private void initContent() {
        powerOff.setOnClickListener(this);
        reboot.setOnClickListener(this);
        recovery.setOnClickListener(this);
        download_mode.setOnClickListener(this);
        fastboot.setOnClickListener(this);
        fastbootd.setOnClickListener(this);
        binding.homeBottomRelativeOstYoutube.setOnClickListener(this);
        binding.homeBottomRelativeOstTelegram.setOnClickListener(this);
        binding.homeBottomRelativeOstGithub.setOnClickListener(this);
        binding.homeBottomRelativeOstOpenvk.setOnClickListener(this);
        binding.homeBottomRelativeOstDa.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        long uptimeMillis = SystemClock.uptimeMillis();
        if (uptimeMillis - mLastClickTime > 600L) {
            String url = null;
            String message = null;
            String cmd = null;
            if (v.getId() == binding.homeBottomRelativeOstYoutube.getId()) {
                url = "https://www.youtube.com/channel/UC6wNi6iQFVSnd-eJivuG3_Q";
            } else if (v.getId() == binding.homeBottomRelativeOstTelegram.getId()) {
                url = "https://t.me/ost_news5566";
            } else if (v.getId() == binding.homeBottomRelativeOstGithub.getId()) {
                url = "https://github.com/ost-sys/";
            } else if (v.getId() == binding.homeBottomRelativeOstOpenvk.getId()) {
                url = "https://ovk.to/id11578";
            } else if (v.getId() == binding.homeBottomRelativeOstDa.getId()) {
                url = "https://www.donationalerts.com/r/ost_5566";
            } else if (v.getId() == powerOff.getId()) {
                message = getString(R.string.turn_off_q);
                cmd = "reboot -p";
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
            if (url != null) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.d("Link error", "Activity is not detected");
                }
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
        return R.layout.fragment_home;
    }

    @Override
    public int getIconResId() {
        return dev.oneuiproject.oneui.R.drawable.ic_oui_home_outline;
    }

    @Override
    public CharSequence getTitle() {
        return getString(R.string.home);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem menuItem = menu.findItem(R.id.menu_check_root);
        menuItem.setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_check_root) {
            Shell.Result result = Shell.cmd("su").exec();
            if (result.isSuccess()) {
                powerOff.setEnabled(true);
                powerOff.setEnabled(true);
                reboot.setEnabled(true);
                recovery.setEnabled(true);
                download_mode.setEnabled(getSystemProperty("ro.product.system.brand").equals("samsung"));
                fastboot.setEnabled(true);
                fastbootd.setEnabled(true);
            } else {
                AlertDialog.Builder alert = new AlertDialog.Builder(binding.getRoot().getContext());
                alert.setTitle(getString(R.string.power_menu))
                        .setMessage(getString(R.string.feature_unavailable_root))
                        .setNegativeButton("OK", null)
                        .create()
                        .show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}