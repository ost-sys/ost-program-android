package com.ost.application.ui.utilities;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.ost.application.R;
import com.ost.application.databinding.FragmentUtilitiesBinding;

public class UtilitiesFragment extends Fragment implements View.OnClickListener {
    public FragmentUtilitiesBinding binding;
    private long mLastClickTime;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentUtilitiesBinding.inflate(inflater, container, false);

        initContent();
        return binding.getRoot();
    }

    private void initContent() {
        binding.utilitiesAndroid.setOnClickListener(this);
        binding.utilitiesMagisk.setOnClickListener(this);
        binding.utilitiesCustomization.setOnClickListener(this);
        binding.utilitiesProductKeys.setOnClickListener(this);
    }

    private void setBottomContentEnabled(boolean enabled) {
        binding.utilitiesAndroid.setEnabled(enabled);
        binding.utilitiesMagisk.setEnabled(enabled);
        binding.utilitiesCustomization.setEnabled(enabled);
        binding.utilitiesProductKeys.setEnabled(enabled);
    }

    @Override
    public void onClick(View v) {
        long uptimeMillis = SystemClock.uptimeMillis();
        if (uptimeMillis - mLastClickTime > 600L) {
            if (v.getId() == binding.utilitiesAndroid.getId()) {
                Fragment utilitesAndroidFragment = new UtilitiesAndroidFragment();
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment_activity_main, utilitesAndroidFragment, "findThisFragment")
                        .addToBackStack(null)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit();

            } else if (v.getId() == binding.utilitiesMagisk.getId()) {
                Fragment utilitiesMagiskFragment = new UtilitiesMagiskFragment();
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment_activity_main, utilitiesMagiskFragment, "findThisFragment")
                        .addToBackStack(null)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit();

            } else if (v.getId() == binding.utilitiesCustomization.getId()) {
                Fragment utilitiesCustomizationFragment = new UtilitiesCustomizationFragment();
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment_activity_main, utilitiesCustomizationFragment, "findThisFragment")
                        .addToBackStack(null)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit();

            } else if (v.getId() == binding.utilitiesProductKeys.getId()) {
                Fragment utilitiesProductKeysFragment = new UtilitiesProductKeysFragment();
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment_activity_main, utilitiesProductKeysFragment, "findThisFragment")
                        .addToBackStack(null)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit();

            }
        }
        mLastClickTime = uptimeMillis;
    }

    private FragmentManager getSupportFragmentManager() {
        return getSupportFragmentManager();
    }
}