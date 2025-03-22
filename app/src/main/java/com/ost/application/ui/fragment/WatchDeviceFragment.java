package com.ost.application.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.ost.application.R;
import com.ost.application.databinding.FragmentWatchDeviceBinding;
import com.ost.application.ui.core.base.BaseFragment;

public class WatchDeviceFragment extends BaseFragment {
    private FragmentWatchDeviceBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWatchDeviceBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_watch_device;
    }

    @Override
    public int getIconResId() {
        return dev.oneuiproject.oneui.R.drawable.ic_oui_galaxy_watch;
    }

    @Override
    public CharSequence getTitle() {
        return "WearOS";
    }
}