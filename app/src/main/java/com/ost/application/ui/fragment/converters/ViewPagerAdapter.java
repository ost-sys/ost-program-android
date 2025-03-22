package com.ost.application.ui.fragment.converters;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {
    public ViewPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) return new TimeZoneConverterFragment();
        if (position == 1) return new TimeCalculatorFragment();
        return new CurrencyConverterFragment();
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
