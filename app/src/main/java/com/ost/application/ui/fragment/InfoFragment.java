package com.ost.application.ui.fragment;

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
import androidx.appcompat.app.AlertDialog;

import com.ost.application.R;
import com.ost.application.databinding.FragmentInfoBinding;

import com.ost.application.ui.core.base.BaseFragment;

public class InfoFragment extends BaseFragment implements View.OnClickListener {
    FragmentInfoBinding binding;
    private long mLastClickTime;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentInfoBinding.inflate(inflater, container, false);

        initContent();

        return binding.getRoot();
    }

    public void initContent() {
        binding.aboutMe.setOnClickListener(this);
        binding.aboutToolsPreview.setOnClickListener(this);
        binding.aboutToolsRecorder.setOnClickListener(this);
        binding.aboutToolsVideoEditor.setOnClickListener(this);
        binding.aboutToolsVm.setOnClickListener(this);
        binding.aboutDevicesPhone.setOnClickListener(this);
        binding.aboutDevicesPc.setOnClickListener(this);
        binding.aboutDevicesHeadphone.setOnClickListener(this);
        binding.aboutDevicesSecondPhone.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        long uptimeMillis = SystemClock.uptimeMillis();
        if (uptimeMillis - mLastClickTime > 600L) {
            String url = null;
            if (view.getId() == binding.aboutMe.getId()) {
                url = "https://t.me/ost_info";
            } else if (view.getId() == binding.aboutToolsPreview.getId()) {
                url = "https://rsload.net/repack/kpojiuk/23242-adobe-photoshop-repack-kpojiuk-crack.html";
            } else if (view.getId() == binding.aboutToolsRecorder.getId()) {
                url = "https://obsproject.com/";
            } else if (view.getId() == binding.aboutToolsVm.getId()) {
                url = "https://rsload.net/repack/kpojiuk/25218-vmware-workstation-pro-repack-kpojiuk.html";
            } else if (view.getId() == binding.aboutToolsVideoEditor.getId()) {
                url = "https://rsload.net/soft/editor/10312-sony-vegas-pro.html";
            } else if (view.getId() == binding.aboutDevicesPhone.getId()) {
                url = "https://www.samsung.com/en/smartphones/galaxy-s21-5g/specs/";
            } else if (view.getId() == binding.aboutDevicesPc.getId()) {
                AlertDialog.Builder pc = new AlertDialog.Builder(binding.getRoot().getContext());
                pc.setTitle(getString(R.string.computer))
                        .setView(getLayoutInflater().inflate(R.layout.container_info_pc, null))
                        .setPositiveButton("OK", null)
                        .create()
                        .show();
            } else if (view.getId() == binding.aboutDevicesHeadphone.getId()) {
                url = "https://www.samsung.com/us/app/mobile-audio/galaxy-buds2/";
            } else if (view.getId() == binding.aboutDevicesSecondPhone.getId()) {
                url = "https://support.apple.com/en-us/111976";
            } else if (view.getId() == binding.homeBottomRelativeOstYoutube.getId()) {
                url = "https://www.youtube.com/channel/UC6wNi6iQFVSnd-eJivuG3_Q";
            } else if (view.getId() == binding.homeBottomRelativeOstTelegram.getId()) {
                url = "https://t.me/ost_news5566";
            } else if (view.getId() == binding.homeBottomRelativeOstGithub.getId()) {
                url = "https://github.com/ost-sys/";
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

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_info;
    }

    @Override
    public int getIconResId() {
        return dev.oneuiproject.oneui.R.drawable.ic_oui_info_outline;
    }

    @Override
    public CharSequence getTitle() {
        return getString(R.string.information);
    }

}