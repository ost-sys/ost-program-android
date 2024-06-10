package com.ost.application.ui.utilities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.ost.application.databinding.FragmentUtilitiesAndroidBinding;

public class UtilitiesAndroidFragment extends Fragment implements View.OnClickListener {

    private FragmentUtilitiesAndroidBinding binding;
    private long mLastClickTime;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUtilitiesAndroidBinding.inflate(inflater, container, false);

        initContent();
        return binding.getRoot();
    }

    private void initContent() {
        binding.utilitiesAndroidChrome.setOnClickListener(this);
        binding.utilitiesAndroidTermux.setOnClickListener(this);
        binding.utilitiesAndroidTgmonet.setOnClickListener(this);
        binding.utilitiesAndroidAppmanager.setOnClickListener(this);
        binding.utilitiesAndroidTotalcmd.setOnClickListener(this);
        binding.utilitiesAndroidSpacedesk.setOnClickListener(this);
        binding.utilitiesAndroidGmsflags.setOnClickListener(this);
        binding.utilitiesAndroidKdeconnect.setOnClickListener(this);
    }

    private void setBottomContentEnabled(boolean enabled) {
        binding.utilitiesAndroidChrome.setEnabled(enabled);
        binding.utilitiesAndroidTermux.setEnabled(enabled);
        binding.utilitiesAndroidTgmonet.setEnabled(enabled);
        binding.utilitiesAndroidAppmanager.setEnabled(enabled);
        binding.utilitiesAndroidTotalcmd.setEnabled(enabled);
        binding.utilitiesAndroidSpacedesk.setEnabled(enabled);
        binding.utilitiesAndroidGmsflags.setEnabled(enabled);
        binding.utilitiesAndroidKdeconnect.setEnabled(enabled);
    }

    @Override
    public void onClick(View v) {
        long uptimeMillis = SystemClock.uptimeMillis();
        if (uptimeMillis - mLastClickTime > 600L) {
            String url = null;
            if (v.getId() == binding.utilitiesAndroidChrome.getId()) {
                url = "https://www.google.com/intl/ru/chrome/";
            } else if (v.getId() == binding.utilitiesAndroidTermux.getId()) {
                url = "https://github.com/KitsunedFox/termux-monet";
            } else if (v.getId() == binding.utilitiesAndroidTgmonet.getId()) {
                url = "https://play.google.com/store/apps/details?id=com.c3r5b8.telegram_monet&hl=en_US";
            } else if (v.getId() == binding.utilitiesAndroidAppmanager.getId()) {
                url = "https://t.me/AppManagerChannel";
            } else if (v.getId() == binding.utilitiesAndroidTotalcmd.getId()) {
                url = "https://play.google.com/store/apps/details?id=com.ghisler.android.TotalCommander&hl=en_US";
            } else if (v.getId() == binding.utilitiesAndroidSpacedesk.getId()) {
                url = "https://www.spacedesk.net/";
            } else if (v.getId() == binding.utilitiesAndroidGmsflags.getId()) {
                url = "https://github.com/polodarb/GMS-Flags";
            } else if (v.getId() == binding.utilitiesAndroidKdeconnect.getId()) {
                url = "https://kdeconnect.kde.org/";
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
        }
    }
}