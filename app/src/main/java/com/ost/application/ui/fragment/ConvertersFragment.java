package com.ost.application.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.ost.application.R;
import com.ost.application.databinding.FragmentConvertersBinding;
import com.ost.application.ui.core.base.BaseFragment;
import com.ost.application.ui.fragment.converters.ViewPagerAdapter;

public class ConvertersFragment extends BaseFragment {

    FragmentConvertersBinding binding;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentConvertersBinding.inflate(inflater, container, false);

        ViewPager2 viewPager = binding.viewPager;
        TabLayout tabLayout = binding.tabLayout;

        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        viewPager.setOffscreenPageLimit(3);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText(getString(R.string.time_zone));
            } else if (position == 1) {
                tab.setText(getString(R.string.time));
            } else if (position == 2) {
                tab.setText(getString(R.string.currency));
            }
        }).attach();

        return binding.getRoot();
    }

        @Override
    public int getLayoutResId() {
        return R.layout.fragment_converters;
    }

    @Override
    public int getIconResId() {
        return dev.oneuiproject.oneui.R.drawable.ic_oui_tool_outline;
    }

    @Override
    public CharSequence getTitle() {
        return getString(R.string.tools);
    }
}
