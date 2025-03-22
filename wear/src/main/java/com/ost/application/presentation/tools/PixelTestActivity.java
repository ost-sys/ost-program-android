package com.ost.application.presentation.tools;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ost.application.R;

public class PixelTestActivity extends ComponentActivity {
    private final int[] colors = {
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.WHITE,
            Color.BLACK,
            Color.YELLOW,
            Color.GRAY,
            Color.DKGRAY,
            Color.MAGENTA,
            Color.CYAN,
            Color.LTGRAY
    };
    private int currentColorIndex = 0;
    private static final int REQUEST_WRITE_SETTINGS = 100;
    private int originalBrightness = -1;
    private int originalBrightnessMode = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
            finish();
            return true;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        restoreOriginalBrightnessSettings();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
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
            Toast.makeText(this, "Permission request", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Permission error", Toast.LENGTH_SHORT).show();
            }
        }
    }
}