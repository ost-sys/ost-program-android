package com.ost.application.ui.friends;

import android.content.ActivityNotFoundException;
import android.content.Context;
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

import com.ost.application.databinding.FragmentFriendsBinding;
import com.ost.application.databinding.FragmentHomeBinding;

import dev.oneuiproject.oneui.widget.Toast;

public class FriendsFragment extends Fragment implements View.OnClickListener {

    private FragmentFriendsBinding binding;
    private long mLastClickTime;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFriendsBinding.inflate(inflater, container, false);

        initContent();
        return binding.getRoot();
    }

    private void initContent() {
        binding.friendsDsys1100.setOnClickListener(this);
        binding.friendsNerok.setOnClickListener(this);
        binding.friendsDimalq.setOnClickListener(this);
        binding.friendsBohdan.setOnClickListener(this);
        binding.friendsKernel64.setOnClickListener(this);
        binding.friendsDanielm.setOnClickListener(this);
        binding.friendsTinelix.setOnClickListener(this);
        binding.friendsHackuser.setOnClickListener(this);
        binding.friendsErtor.setOnClickListener(this);
        binding.friendsHappywin8.setOnClickListener(this);
        binding.friendsRivixal.setOnClickListener(this);
        binding.friendsClyt.setOnClickListener(this);
        binding.friendsMilosoft.setOnClickListener(this);
        binding.friendsAnton.setOnClickListener(this);
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
            } else if (v.getId() == binding.friendsDanielm.getId()) {
                url = "https://www.youtube.com/@DanielM";
            } else if (v.getId() == binding.friendsTinelix.getId()) {
                url = "https://tinelix.ru/";
            } else if (v.getId() == binding.friendsHackuser.getId()) {
                url = "https://hackintoshuser137.github.io";
            } else if (v.getId() == binding.friendsErtor.getId()) {
                url = "https://ertorworld.com/";
            } else if (v.getId() == binding.friendsHappywin8.getId()) {
                url = "https://www.youtube.com/channel/UCEUf6E02RlsxKlAxIbNzBfA";
            } else if (v.getId() == binding.friendsRivixal.getId()) {
                url = "https://rivixal.github.io/";
            } else if (v.getId() == binding.friendsClyt.getId()) {
                url = "https://p.kirillka.ru/n.html";
            } else if (v.getId() == binding.friendsMilosoft.getId()) {
                url = "https://www.youtube.com/@Mazurobi";
            } else if (v.getId() == binding.friendsAnton.getId()) {
                url = "https://github.com/Anton-Aboba1234";
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