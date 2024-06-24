package com.ost.application.ui.utilities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ost.application.GeminiActivity;
import com.ost.application.R;
import com.ost.application.databinding.FragmentUtilitiesBinding;
import com.ost.application.databinding.FragmentUtilitiesMagiskBinding;

public class UtilitiesMagiskFragment extends Fragment implements View.OnClickListener {
    public FragmentUtilitiesMagiskBinding binding;
    private long mLastClickTime;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentUtilitiesMagiskBinding.inflate(inflater, container, false);

        initContent();
        return binding.getRoot();
    }

    private void initContent() {
        binding.utilitiesMagiskManager.setOnClickListener(this);
        binding.utilitiesMagiskPixelify.setOnClickListener(this);
        binding.utilitiesMagiskAdBlocker.setOnClickListener(this);
        binding.utilitiesMagiskSafetynetFix.setOnClickListener(this);
        binding.utilitiesMagiskKnoxPatch.setOnClickListener(this);
        binding.geminiMagiskInfo.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), GeminiActivity.class);
            String textToInsert = getString(R.string.what_is_magisk_ask);
            intent.putExtra("textToInsert", textToInsert);
            startActivity(intent);
        });
    }

    @Override
    public void onClick(View v) {
        long uptimeMillis = SystemClock.uptimeMillis();
        if (uptimeMillis - mLastClickTime > 600L) {
            String url = null;
            if (v.getId() == binding.utilitiesMagiskManager.getId()) {
                url = "https://github.com/topjohnwu/Magisk";
            } else if (v.getId() == binding.utilitiesMagiskPixelify.getId()) {
                url = "https://github.com/Kingsman44/Pixelify";
            } else if (v.getId() == binding.utilitiesMagiskAdBlocker.getId()) {
                url = "https://github.com/pantsufan/Magisk-Ad-Blocking-Module";
            } else if (v.getId() == binding.utilitiesMagiskSafetynetFix.getId()) {
                url = "https://github.com/kdrag0n/safetynet-fix";
            } else if (v.getId() == binding.utilitiesMagiskKnoxPatch.getId()) {
                url = "https://github.com/XDABlackMesa123/KnoxPatch";
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