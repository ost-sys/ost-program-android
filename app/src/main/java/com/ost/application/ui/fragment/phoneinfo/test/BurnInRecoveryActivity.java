package com.ost.application.ui.fragment.phoneinfo.test;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
<<<<<<< Updated upstream
import androidx.appcompat.widget.TooltipCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;
=======
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import com.ost.application.MainActivity;
>>>>>>> Stashed changes
import com.ost.application.R;
import com.ost.application.activity.settings.SettingsActivity;

import java.util.Objects;
import java.util.Random;

import dev.oneuiproject.oneui.widget.TipPopup;

public class BurnInRecoveryActivity extends AppCompatActivity {
    private static final String CHANNEL_ID = "burn_in_recovery_channel";
    private static final int NOTIFICATION_ID = 1;
    private final Random random = new Random();
    private boolean isRunning = true;
    private Handler handler;
    private NoiseView noiseView;

    private static final int REQUEST_WRITE_SETTINGS = 100;

    private int currentMode = 0;
    private static final int MODE_NOISE = 0;
    private static final int MODE_HORIZONTAL_LINES = 1;
    private static final int MODE_VERTICAL_LINES = 2;
    private static final int MODE_BLACK_WHITE_NOISE = 3;

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

        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences == null) {
            Log.e("BurnInRecovery", "SharedPreferences is null");
        } else {
            int totalDuration = sharedPreferences.getInt("total_duration", 30);
            int noiseDuration = sharedPreferences.getInt("noise_duration", 1);
            int horizontalLinesDuration = sharedPreferences.getInt("horizontal_lines_duration", 1);
            int verticalLinesDuration = sharedPreferences.getInt("vertical_lines_duration", 1);
            int blackWhiteNoiseDuration = sharedPreferences.getInt("black_white_noise_duration", 1);

            handler = new Handler(Looper.getMainLooper());
            startModeCycle(totalDuration, noiseDuration, horizontalLinesDuration, verticalLinesDuration, blackWhiteNoiseDuration);
        }

        noiseView.setOnLongClickListener(v -> {
            isRunning = false;
            Toast.makeText(this, getString(R.string.exiting), Toast.LENGTH_SHORT).show();
            finish();
            return true;
        });

        noiseView.setOnClickListener(v -> Toast.makeText(this, R.string.hold_to_exit, Toast.LENGTH_SHORT).show());

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

    private void startModeCycle(int totalDuration, int noiseDuration, int horizontalLinesDuration, int verticalLinesDuration, int blackWhiteNoiseDuration) {
        int[] modeDurations = {
                noiseDuration * 60 * 1000,
                horizontalLinesDuration * 60 * 1000,
                verticalLinesDuration * 60 * 1000,
                blackWhiteNoiseDuration * 60 * 1000
        };

        Runnable modeSwitcher = new Runnable() {
            long startTime = System.currentTimeMillis();
            int modeIndex = 0;

            @Override
            public void run() {
                if (!isRunning) return;

                long elapsedTime = System.currentTimeMillis() - startTime;
                if (elapsedTime >= totalDuration * 60 * 1000) {
                    isRunning = false;
                    Toast.makeText(BurnInRecoveryActivity.this, getString(R.string.exiting), Toast.LENGTH_SHORT).show();
                    showCompletionNotification();
                    finish();
                    return;
                }

                currentMode = modeIndex;
                noiseView.setMode(currentMode);

                modeIndex = (modeIndex + 1) % modeDurations.length;

                Log.d("BurnInRecovery", "Switching to mode " + modeIndex + ", Duration: " + modeDurations[modeIndex]);

                handler.postDelayed(this, modeDurations[modeIndex]);
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

            @SuppressLint("DrawAllocation") int[] pixels = new int[width * height];

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

                case MODE_BLACK_WHITE_NOISE:
                    for (int i = 0; i < pixels.length; i++) {
                        int gray = random.nextInt(256);
                        pixels[i] = Color.rgb(gray, gray, gray);
                    }
                    break;
            }

            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            canvas.drawBitmap(bitmap, 0, 0, paint);

            invalidate();
        }
    }

    private void showCompletionNotification() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(getString(R.string.success));
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(dev.oneuiproject.oneui.R.drawable.ic_oui_screen_resolution)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Fixing dead/burnt pixels is complete")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(soundUri)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }
}

