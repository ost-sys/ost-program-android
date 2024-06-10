package com.ost.application.ui.home;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ost.application.R;
import com.ost.application.databinding.FragmentHomeBinding;
import com.ost.application.ui.friends.FriendsFragment;
import com.ost.application.ui.info.InfoFragment;
import com.ost.application.ui.utilities.UtilitiesFragment;

import dev.oneuiproject.oneui.widget.Toast;

public class HomeFragment extends Fragment implements View.OnClickListener {

    private FragmentHomeBinding binding;
    private long mLastClickTime;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        initContent();
        return binding.getRoot();
    }

    private void initContent() {
        binding.homeBottomUtilities.setOnClickListener(this);
        binding.homeBottomFriends.setOnClickListener(this);
        binding.homeBottomInfo.setOnClickListener(this);
        binding.homeBottomRelativeOstYoutube.setOnClickListener(this);
        binding.homeBottomRelativeOstTelegram.setOnClickListener(this);
        binding.homeBottomRelativeOstGithub.setOnClickListener(this);
        binding.homeBottomRelativeOstOpenvk.setOnClickListener(this);
        binding.homeBottomRelativeOstDa.setOnClickListener(this);
    }

    private void setBottomContentEnabled(boolean enabled) {
        binding.homeBottomUtilities.setEnabled(enabled);
        binding.homeBottomFriends.setEnabled(enabled);
        binding.homeBottomInfo.setEnabled(enabled);
        binding.homeBottomRelativeOstYoutube.setEnabled(enabled);
        binding.homeBottomRelativeOstTelegram.setEnabled(enabled);
        binding.homeBottomRelativeOstGithub.setEnabled(enabled);
        binding.homeBottomRelativeOstOpenvk.setEnabled(enabled);
        binding.homeBottomRelativeOstDa.setEnabled(enabled);
    }

    @Override
    public void onClick(View v) {
        long uptimeMillis = SystemClock.uptimeMillis();
        if (uptimeMillis - mLastClickTime > 600L) {
            String url = null;
            if (v.getId() == binding.homeBottomUtilities.getId()) {
                Fragment utilitiesFragment = new UtilitiesFragment();
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment_activity_main, utilitiesFragment, "findThisFragment")
                        .addToBackStack(null)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit();
            } else if (v.getId() == binding.homeBottomFriends.getId()) {
                Fragment friendsFragment = new FriendsFragment();
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment_activity_main, friendsFragment, "findThisFragment")
                        .addToBackStack(null)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit();

            } else if (v.getId() == binding.homeBottomInfo.getId()) {
                Fragment infoFragment = new InfoFragment();
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment_activity_main, infoFragment, "findThisFragment")
                        .addToBackStack(null)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit();

            } else if (v.getId() == binding.homeBottomRelativeOstYoutube.getId()) {
                url = "https://www.youtube.com/channel/UC6wNi6iQFVSnd-eJivuG3_Q";
            } else if (v.getId() == binding.homeBottomRelativeOstTelegram.getId()) {
                url = "https://t.me/ost_news5566";
            } else if (v.getId() == binding.homeBottomRelativeOstGithub.getId()) {
                url = "https://github.com/ost-sys/";
            } else if (v.getId() == binding.homeBottomRelativeOstOpenvk.getId()) {
                url = "https://ovk.to/id11578";
            } else if (v.getId() == binding.homeBottomRelativeOstDa.getId()) {
                url = "https://www.donationalerts.com/r/ost_5566";
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
        mLastClickTime = uptimeMillis;
    }
}