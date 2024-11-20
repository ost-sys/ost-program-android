package com.ost.application.ui.fragment.phoneinfo.test;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ost.application.R;

public class PixelTestActivity extends AppCompatActivity {
    private final int[] colors = {
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.WHITE,
            Color.BLACK,
            Color.YELLOW,
            Color.GRAY,
            Color.DKGRAY
    };
    private int currentColorIndex = 0;
    private static final int REQUEST_WRITE_SETTINGS = 100;
    private int originalBrightness = -1;
    private int originalBrightnessMode = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        final View colorView = new View(this);
        colorView.setBackgroundColor(colors[currentColorIndex]);
        setContentView(colorView);

        saveCurrentBrightnessSettings();
        setMaxBrightness();

        colorView.setOnClickListener(v -> {
            currentColorIndex = (currentColorIndex + 1) % colors.length;
            colorView.setBackgroundColor(colors[currentColorIndex]);
        });

        colorView.setOnLongClickListener(v -> {
            Toast.makeText(this, getString(R.string.exiting), Toast.LENGTH_SHORT).show();
            finish();
            return true;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        restoreOriginalBrightnessSettings();
    }

    private void saveCurrentBrightnessSettings() {
        try {
            originalBrightness = Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS
            );
            originalBrightnessMode = Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE
            );
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void setMaxBrightness() {
        try {
            Settings.System.putInt(
                    getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    255
            );
            Settings.System.putInt(
                    getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            );
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.brightness_permission_r), Toast.LENGTH_SHORT).show();
        }
    }

    private void restoreOriginalBrightnessSettings() {
        try {
            if (originalBrightness != -1) {
                Settings.System.putInt(
                        getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS,
                        originalBrightness
                );
            }

            if (originalBrightnessMode != -1) {
                Settings.System.putInt(
                        getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        originalBrightnessMode
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_WRITE_SETTINGS) {
            if (Settings.System.canWrite(this)) {
                saveCurrentBrightnessSettings();
                setMaxBrightness();
            } else {
                Toast.makeText(this, getString(R.string.permission_not_granted), Toast.LENGTH_SHORT).show();
            }
        }
    }
}