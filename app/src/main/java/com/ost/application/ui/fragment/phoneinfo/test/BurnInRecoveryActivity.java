package com.ost.application.ui.fragment.phoneinfo.test;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.ost.application.R;
import com.ost.application.SettingsActivity;

import java.util.Random;

public class BurnInRecoveryActivity extends AppCompatActivity {
    private final Random random = new Random();
    private boolean isRunning = true;
    private Handler handler;
    private NoiseView noiseView;

    private static final int REQUEST_WRITE_SETTINGS = 100;
    private static final long MODE_DURATION = 60 * 1000;

    private int currentMode = 0;
    private static final int MODE_NOISE = 0;
    private static final int MODE_HORIZONTAL_LINES = 1;
    private static final int MODE_VERTICAL_LINES = 2;

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
        noiseView = new NoiseView(this);
        setContentView(noiseView);

        boolean isBrightnessControlEnabled = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean("brightness_control", true);

        if (isBrightnessControlEnabled) {
            if (!Settings.System.canWrite(this)) {
                Toast.makeText(this, getString(R.string.brightness_permission_m), Toast.LENGTH_LONG).show();
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
                return;
            } else {
                saveCurrentBrightnessSettings();
                setMaxBrightness();
            }
        }

        handler = new Handler(Looper.getMainLooper());
        startModeCycle();

        noiseView.setOnLongClickListener(v -> {
            isRunning = false;
            Toast.makeText(this, getString(R.string.exiting), Toast.LENGTH_SHORT).show();
            finish();
            return true;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
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
            Toast.makeText(this, getString(R.string.brightness_fail), Toast.LENGTH_SHORT).show();
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

    private void startModeCycle() {
        Runnable modeSwitcher = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;

                currentMode = (currentMode + 1) % 3;
                noiseView.setMode(currentMode);

                handler.postDelayed(this, MODE_DURATION);
            }
        };
        handler.post(modeSwitcher);
    }

    private static class NoiseView extends View {
        private final Paint paint = new Paint();
        private final Random random = new Random();
        private Bitmap bitmap;

        private int mode = MODE_NOISE;

        public NoiseView(android.content.Context context) {
            super(context);
        }

        public void setMode(int mode) {
            this.mode = mode;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            int width = getWidth();
            int height = getHeight();

            if (bitmap == null || bitmap.getWidth() != width || bitmap.getHeight() != height) {
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            }

            int[] pixels = new int[width * height];

            switch (mode) {
                case MODE_NOISE:
                    for (int i = 0; i < pixels.length; i++) {
                        pixels[i] = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256));
                    }
                    break;

                case MODE_HORIZONTAL_LINES:
                    for (int y = 0; y < height; y++) {
                        int color = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256));
                        for (int x = 0; x < width; x++) {
                            pixels[y * width + x] = color;
                        }
                    }
                    break;

                case MODE_VERTICAL_LINES:
                    for (int x = 0; x < width; x++) {
                        int color = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256));
                        for (int y = 0; y < height; y++) {
                            pixels[y * width + x] = color;
                        }
                    }
                    break;
            }

            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            canvas.drawBitmap(bitmap, 0, 0, paint);

            invalidate();
        }
    }
}

