package com.ost.application.ui.core.base;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.transition.MaterialSharedAxis;

public abstract class BaseFragment extends Fragment
        implements FragmentInfo {
    protected Context mContext;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        setupFragmentTransitions();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(getLayoutResId(), container, false);
    }

    private void setupFragmentTransitions() {
        PathInterpolator interpolator = new PathInterpolator(0.1f, 0.1f, 0f, 1f);
        MaterialSharedAxis enterTransition = new MaterialSharedAxis(MaterialSharedAxis.Y, true);
        enterTransition.setInterpolator(interpolator);

        MaterialSharedAxis reenterTransition = new MaterialSharedAxis(MaterialSharedAxis.Y, false);
        reenterTransition.setInterpolator(interpolator);

        MaterialSharedAxis returnTransition = new MaterialSharedAxis(MaterialSharedAxis.Y, false);
        returnTransition.setInterpolator(interpolator);

        MaterialSharedAxis exitTransition = new MaterialSharedAxis(MaterialSharedAxis.Y, true);
        exitTransition.setInterpolator(interpolator);
    }

    public abstract int getLayoutResId();

    public abstract int getIconResId();

    public abstract CharSequence getTitle();

    @Override
    public boolean isAppBarEnabled() {
        return true;
    }
}
