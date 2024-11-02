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

import com.ost.application.R;
import com.ost.application.databinding.FragmentFriendsBinding;

import com.ost.application.ui.core.base.BaseFragment;

public class FriendsFragment extends BaseFragment implements View.OnClickListener {

    private FragmentFriendsBinding binding;
    private long mLastClickTime;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFriendsBinding.inflate(inflater, container, false);

        initContent();
        return binding.getRoot();
    }

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_friends;
    }

    @Override
    public int getIconResId() {
        return dev.oneuiproject.oneui.R.drawable.ic_oui_group_outline;
    }

    @Override
    public CharSequence getTitle() {
        return getString(R.string.friends);
    }

    private void initContent() {
        binding.friendsDsys1100.setOnClickListener(this);
        binding.friendsNerok.setOnClickListener(this);
        binding.friendsDimalq.setOnClickListener(this);
        binding.friendsBohdan.setOnClickListener(this);
        binding.friendsKernel64.setOnClickListener(this);
        binding.friendsHackuser.setOnClickListener(this);
        binding.friendsErtor.setOnClickListener(this);
        binding.friendsHappywin8.setOnClickListener(this);
        binding.friendsRivixal.setOnClickListener(this);
        binding.friendsAnton.setOnClickListener(this);
        binding.friendsTu4wkl.setOnClickListener(this);
        binding.friendsZhh4.setOnClickListener(this);
        binding.friendsShagalin.setOnClickListener(this);

        binding.friendsDanielm.setOnClickListener(this);
        binding.friendsTinelix.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        long uptimeMillis = SystemClock.uptimeMillis();
        if (uptimeMillis - mLastClickTime > 600L) {
            String url = null;
            if (v.getId() == binding.friendsDsys1100.getId()) {
                url = "https://dsys1100.github.io/";
            } else if (v.getId() == binding.friendsNerok.getId()) {
                url = "https://t.me/localhosted";
            } else if (v.getId() == binding.friendsDimalq.getId()) {
                url = "https://www.tiktok.com/@dimalq_real?_t=8mrsrUJf1Aj&_r=1";
            } else if (v.getId() == binding.friendsBohdan.getId()) {
                url = "https://Bohdan157.github.io";
            } else if (v.getId() == binding.friendsKernel64.getId()) {
                url = "https://xerix123456.github.io";
            } else if (v.getId() == binding.friendsHackuser.getId()) {
                url = "https://hackintoshuser137.github.io";
            } else if (v.getId() == binding.friendsErtor.getId()) {
                url = "https://ertorworld.com/";
            } else if (v.getId() == binding.friendsHappywin8.getId()) {
                url = "https://www.youtube.com/channel/UCEUf6E02RlsxKlAxIbNzBfA";
            } else if (v.getId() == binding.friendsRivixal.getId()) {
                url = "https://rivixal.github.io/";
            } else if (v.getId() == binding.friendsAnton.getId()) {
                url = "https://github.com/Anton-Aboba1234";
            } else if (v.getId() == binding.friendsTu4wkl.getId()) {
                url = "https://pachdomenic.github.io";
            } else if (v.getId() == binding.friendsZhh4.getId()) {
                url = "https://t.me/zh4eny";
            } else if (v.getId() == binding.friendsShagalin.getId()) {
                url = "https://www.youtube.com/@DanyaShagalin";
            } else if (v.getId() == binding.friendsDanielm.getId()) {
                url = "https://www.youtube.com/@DanielM";
            } else if (v.getId() == binding.friendsTinelix.getId()) {
                url = "https://tinelix.ru/";
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