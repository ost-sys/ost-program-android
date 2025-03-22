package com.ost.application.ui.fragment.phoneinfo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;

import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.preference.PreferenceManager;

import com.ost.application.R;
import com.ost.application.databinding.FragmentDisplayInfoBinding;
import com.ost.application.ui.core.base.BaseFragment;
import com.ost.application.ui.fragment.phoneinfo.test.BurnInRecoveryActivity;
import com.ost.application.ui.fragment.phoneinfo.test.PixelTestActivity;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

public class DisplayInfoFragment extends BaseFragment implements View.OnClickListener {
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

        binding.checkDeadPixels.setOnClickListener(this);
        binding.fixDeadPixels.setOnClickListener(this);

        return binding.getRoot();
    }

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_display_info;
    }

    @Override
    public int getIconResId() {
        return dev.oneuiproject.oneui.R.drawable.ic_oui_screen_resolution;
    }

    @Override
    public CharSequence getTitle() {
        return getString(R.string.display);
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
        binding.displayRefreshRate.setSummary((int) refreshRating + " " + getString(R.string.hz));
        binding.displayDpi.setSummary(densityDpi + " " + getString(R.string.dpi));
        binding.displayScreenDiagonal.setSummary(getDisplaySize(getActivity()) + " " + getString(R.string.inches));

        if (getActivity().getResources().getConfiguration().orientation == 1) {
            binding.displayScreenOrientation.setSummary(getString(R.string.portrait));
        } else if (getActivity().getResources().getConfiguration().orientation == 2) {
            binding.displayScreenOrientation.setSummary(getString(R.string.landscape));
        }

        if (hasStylusSupport()) {
            binding.displayStylusSupport.setSummary(getString(R.string.support));
        } else {
            binding.displayStylusSupport.setSummary(getString(R.string.unsupport));
        }
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

    private void launchPixelTestActivity() {
        Intent intent = new Intent(requireContext(), PixelTestActivity.class);
        startActivity(intent);
    }

    private void launchPixelFixActivity() {
        Context context = requireContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        int totalDuration = preferences.getInt("total_duration", 30);
        int noiseDuration = preferences.getInt("noise_duration", 1);
        int horizontalDuration = preferences.getInt("horizontal_duration", 1);
        int verticalDuration = preferences.getInt("vertical_duration", 1);
        int blackWhiteNoiseDuration = preferences.getInt("black_white_noise_duration", 1);

        Intent intent = new Intent(requireContext(), BurnInRecoveryActivity.class);
        intent.putExtra("totalDuration", totalDuration);
        intent.putExtra("noiseDuration", noiseDuration);
        intent.putExtra("horizontalDuration", horizontalDuration);
        intent.putExtra("verticalDuration", verticalDuration);
        intent.putExtra("blackWhiteNoiseDuration", blackWhiteNoiseDuration);
        startActivity(intent);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == binding.checkDeadPixels.getId()) {
            launchPixelTestActivity();
        } else if (view.getId() == binding.fixDeadPixels.getId()) {
            launchPixelFixActivity();
        }
    }

    private boolean hasStylusSupport() {
        int[] deviceIds = InputDevice.getDeviceIds();
        for (int id : deviceIds) {
            InputDevice device = InputDevice.getDevice(id);
            if (device != null && device.supportsSource(InputDevice.SOURCE_STYLUS)) {
                return true;
            }
        }
        return false;
    }
}
