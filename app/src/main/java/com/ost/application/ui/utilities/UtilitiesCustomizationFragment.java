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

import com.ost.application.R;
import com.ost.application.databinding.FragmentUtilitiesBinding;
import com.ost.application.databinding.FragmentUtilitiesCustomizationBinding;


public class UtilitiesCustomizationFragment extends Fragment implements View.OnClickListener {
    public FragmentUtilitiesCustomizationBinding binding;
    private long mLastClickTime;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentUtilitiesCustomizationBinding.inflate(inflater, container, false);

        initContent();
        return binding.getRoot();
    }



    private void initContent() {
        binding.utilitiesCustomizationWallpapers.setOnClickListener(this);
        binding.utilitiesCustomizationMusic.setOnClickListener(this);
        binding.utilitiesCustomizationSounds.setOnClickListener(this);
        binding.utilitiesCustomizationUxPatcher.setOnClickListener(this);
        binding.utilitiesCustomizationSharex.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        long uptimeMillis = SystemClock.uptimeMillis();
        if (uptimeMillis - mLastClickTime > 600L) {
            String url = null;
            if (v.getId() == binding.utilitiesCustomizationWallpapers.getId()) {
                url = "https://drive.google.com/drive/folders/1KigH_0VDp91qw-5pVMRBHvPVxbdkRAPN?usp=drive_link";
            } else if (v.getId() == binding.utilitiesCustomizationMusic.getId()) {
                url = "https://drive.google.com/drive/folders/1WVhCd8hU6o3MteJwYnVfeEgLdKMgkdJ-";
            } else if (v.getId() == binding.utilitiesCustomizationSounds.getId()) {
                url = "https://drive.google.com/drive/folders/1dEKviyBUgNqEs9qutlL9k1Jt0-kYi3dX";
            } else if (v.getId() == binding.utilitiesCustomizationUxPatcher.getId()) {
                url = "https://mhoefs.eu/software_uxtheme.php?ref=syssel&lang=en";
            } else if (v.getId() == binding.utilitiesCustomizationSharex.getId()) {
                url = "https://getsharex.com/";
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