package com.ost.application.ui.fragment.phoneinfo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;

import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.ost.application.R;
import com.ost.application.databinding.FragmentDisplayInfoBinding;
import com.ost.application.ui.core.base.BaseFragment;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

public class DisplayInfoFragment extends BaseFragment {
    private FragmentDisplayInfoBinding binding;
    private Handler handler = new Handler();
    private Runnable updateRunnable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentDisplayInfoBinding.inflate(inflater, container, false);

        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateDisplayInfo();
                handler.postDelayed(this, 500);
            }
        };

        handler.post(updateRunnable);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(updateRunnable);
    }

    private void updateDisplayInfo() {
        getActivity().getWindowManager().getDefaultDisplay();
        Display display = ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        Point size = new Point();

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        try {
            Display.class.getMethod("getRealSize", Point.class).invoke(display, size);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        int width = size.x;
        int height = size.y;

        float refreshRating = display.getRefreshRate();
        int densityDpi = (int)(metrics.density * 160f);

        binding.displayScreenResolution.setText(height + " * " + width);
        binding.displayRefreshRate.setSummaryText((int) refreshRating + " " + getString(R.string.hz));
        binding.displayDpi.setSummaryText(densityDpi + " " + getString(R.string.dpi));
        binding.displayScreenDiagonal.setSummaryText(getDisplaySize(getActivity()) + " " + getString(R.string.inches));

        if (getActivity().getResources().getConfiguration().orientation == 1) {
            binding.displayScreenOrientation.setSummaryText(getString(R.string.portrait));
        } else if (getActivity().getResources().getConfiguration().orientation == 2) {
            binding.displayScreenOrientation.setSummaryText(getString(R.string.landscape));
        }
    }

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_display_info;
    }

    @Override
    public int getIconResId() {
        return dev.oneuiproject.oneui.R.drawable.ic_oui_fit_to_screen;
    }

    @Override
    public CharSequence getTitle() {
        return getString(R.string.display);
    }

    static String getDisplaySize(Activity activity) {
        double x = 0, y = 0;
        int mWidthPixels, mHeightPixels;
        try {
            WindowManager windowManager = activity.getWindowManager();
            Display display = windowManager.getDefaultDisplay();
            DisplayMetrics displayMetrics = new DisplayMetrics();
            display.getMetrics(displayMetrics);
            Point realSize = new Point();
            Display.class.getMethod("getRealSize", Point.class).invoke(display, realSize);
            mWidthPixels = realSize.x;
            mHeightPixels = realSize.y;
            DisplayMetrics dm = new DisplayMetrics();
            activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
            x = Math.pow(mWidthPixels / dm.xdpi, 2);
            y = Math.pow(mHeightPixels / dm.ydpi, 2);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return String.format(Locale.US, "%.2f", Math.sqrt(x + y));
    }
}
