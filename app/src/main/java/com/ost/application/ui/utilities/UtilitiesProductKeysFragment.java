package com.ost.application.ui.utilities;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ost.application.R;
import com.ost.application.databinding.FragmentUtilitiesProductKeysBinding;


public class UtilitiesProductKeysFragment extends Fragment {
    private FragmentUtilitiesProductKeysBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUtilitiesProductKeysBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }
}