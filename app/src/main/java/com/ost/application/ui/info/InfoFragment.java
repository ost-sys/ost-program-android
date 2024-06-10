package com.ost.application.ui.info;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.ost.application.databinding.ContainerInfoBasicBinding;
import com.ost.application.databinding.ContainerInfoPcBinding;
import com.ost.application.databinding.ContainerInfoPhoneBinding;
import com.ost.application.databinding.ContainerInfoProgramsBinding;
import com.ost.application.databinding.FragmentInfoBinding;

import dev.oneuiproject.oneui.widget.Separator;

public class InfoFragment extends Fragment {

    SwitchCompat switchBasicInfo;
    SwitchCompat switchPrograms;
    SwitchCompat switchPc;
    SwitchCompat switchSmartphone;
    Separator separatorBasicInfo;
    Separator separatorPrograms;
    Separator separatorPc;
    Separator separatorSmartphone;
    ContainerInfoBasicBinding containerInfoBasicBinding;
    ContainerInfoProgramsBinding containerInfoProgramsBinding;
    ContainerInfoPcBinding containerInfoPcBinding;
    ContainerInfoPhoneBinding containerInfoPhoneBinding;
    FragmentInfoBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentInfoBinding.inflate(inflater, container, false);

        switchBasicInfo = binding.switchBasicInfo;
        switchPrograms = binding.switchPrograms;
        switchPc = binding.switchPc;
        switchSmartphone = binding.switchSmartphone;

        separatorBasicInfo = binding.separatorBasicInfo;
        separatorPrograms = binding.separatorPrograms;
        separatorPc = binding.separatorPc;
        separatorSmartphone = binding.separatorSmartphone;

        containerInfoBasicBinding = binding.containerBasicInfo;
        containerInfoProgramsBinding = binding.containerPrograms;
        containerInfoPcBinding = binding.containerPc;
        containerInfoPhoneBinding = binding.containerSmartphone;

        switchBasicInfo.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if (buttonView.isChecked()) {
                separatorBasicInfo.setVisibility(View.VISIBLE);
                containerInfoBasicBinding.getRoot().setVisibility(View.VISIBLE);
            } else {
                separatorBasicInfo.setVisibility(View.GONE);
                containerInfoBasicBinding.getRoot().setVisibility(View.GONE);
            }
        }));

        switchPrograms.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if (buttonView.isChecked()) {
                separatorPrograms.setVisibility(View.VISIBLE);
                containerInfoProgramsBinding.getRoot().setVisibility(View.VISIBLE);
            } else {
                separatorPrograms.setVisibility(View.GONE);
                containerInfoProgramsBinding.getRoot().setVisibility(View.GONE);
            }
        }));

        switchPc.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if (buttonView.isChecked()) {
                separatorPc.setVisibility(View.VISIBLE);
                containerInfoPcBinding.getRoot().setVisibility(View.VISIBLE);
            } else {
                separatorPc.setVisibility(View.GONE);
                containerInfoPcBinding.getRoot().setVisibility(View.GONE);
            }
        }));

        switchSmartphone.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if (buttonView.isChecked()) {
                separatorSmartphone.setVisibility(View.VISIBLE);
                containerInfoPhoneBinding.getRoot().setVisibility(View.VISIBLE);
            } else {
                separatorSmartphone.setVisibility(View.GONE);
                containerInfoPhoneBinding.getRoot().setVisibility(View.GONE);
            }
        }));

        return binding.getRoot();
    }

    public void onPause() {
        super.onPause();
    }
    public void onResume() {
        super.onResume();
    }
}