package com.ost.application.ui.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Handler;
import android.os.SystemClock;
import android.service.quicksettings.Tile;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.ost.application.R;
import com.topjohnwu.superuser.Shell;

public class TileService extends android.service.quicksettings.TileService {

    private int currentAction = 0;
    private final int TOTAL_ACTIONS = 6;
    private long lastClickTime = 0;
    private int clickCount = 0;
    private final long CLICK_TIME_DELTA = 500;
    private final long TRIPLE_CLICK_TIME_DELTA = 300;
    private Handler handler = new Handler();
    private Runnable singleClickRunnable;
    private static final String PREF_APP_LAUNCHED = "pref_app_launched";

    @Override
    public void onTileAdded() {
        if (!isAppLaunched()) {
            Tile tile = getQsTile();
            if (tile != null) {
                tile.setState(Tile.STATE_UNAVAILABLE);
                tile.updateTile();
            }
        } else if (!hasRootAccess()) {
            Tile tile = getQsTile();
            if (tile != null) {
                tile.setState(Tile.STATE_UNAVAILABLE);
                tile.updateTile();
            }
        } else {
            updateTile("Моя кнопка", R.drawable.ic_oui_open, Tile.STATE_INACTIVE);
        }
    }

    @Override
    public void onStartListening() {
        if (!hasRootAccess()) {
            Tile tile = getQsTile();
            if (tile != null) {
                tile.setState(Tile.STATE_UNAVAILABLE);
                tile.updateTile();
            }
        } else {
            updateTile("Моя кнопка", R.drawable.ic_oui_open, Tile.STATE_INACTIVE);
        }
    }

    @Override
    public void onClick() {
        long clickTime = SystemClock.elapsedRealtime();

        if (clickTime - lastClickTime < TRIPLE_CLICK_TIME_DELTA) {
            clickCount++;
        } else {
            clickCount = 1;
        }

        lastClickTime = clickTime;

        if (singleClickRunnable != null) {
            handler.removeCallbacks(singleClickRunnable);
        }

        singleClickRunnable = () -> {
            if (clickCount >= 3) {
                performCurrentAction();
            } else {
                currentAction = (currentAction + 1) % TOTAL_ACTIONS;
                updateTileForCurrentAction();
            }
            clickCount = 0;
        };

        handler.postDelayed(singleClickRunnable, CLICK_TIME_DELTA);
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
    }

    private boolean isAppLaunched() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getBoolean(PREF_APP_LAUNCHED, false);
    }

    public static void setAppLaunchedFlag(boolean launched, Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREF_APP_LAUNCHED, launched);
        editor.apply();
    }

    private void updateTileForCurrentAction() {
        String label;
        int iconResId;
        int state;

        String brand = getSystemProperty("ro.product.system.brand");
        boolean isSamsung = brand.equalsIgnoreCase("samsung");

        if (currentAction == 3 && !isSamsung) {
            currentAction = (currentAction + 1) % TOTAL_ACTIONS;
        }

        switch (currentAction) {
            case 0:
                label = getString(R.string.turn_off);
                iconResId = R.drawable.ic_oui_power;
                state = Tile.STATE_INACTIVE;
                break;
            case 1:
                label = getString(R.string.reboot);
                iconResId = R.drawable.ic_oui_restart;
                state = Tile.STATE_INACTIVE;
                break;
            case 2:
                label = getString(R.string.reboot_recovery);
                iconResId = R.drawable.ic_oui_lightning;
                state = Tile.STATE_INACTIVE;
                break;
            case 3:
                label = getString(R.string.reboot_download);
                iconResId = R.drawable.ic_oui_download_circle_filled;
                state = Tile.STATE_INACTIVE;
                break;
            case 4:
                label = getString(R.string.reboot_fastboot);
                iconResId = R.drawable.ic_oui_restart;
                state = Tile.STATE_INACTIVE;
                break;
            case 5:
                label = getString(R.string.reboot_fastbootd);
                iconResId = R.drawable.ic_oui_restart;
                state = Tile.STATE_INACTIVE;
                break;
            default:
                label = getString(R.string.power_menu);
                iconResId = R.drawable.ic_oui_open;
                state = Tile.STATE_INACTIVE;
        }

        updateTile(label, iconResId, state);
    }

    private void performCurrentAction() {
        switch (currentAction) {
            case 0:
                Shell.cmd("reboot -p").exec();
                break;
            case 1:
                Shell.cmd("reboot").exec();
                break;
            case 2:
                Shell.cmd("reboot recovery").exec();
                break;
            case 3:
                Shell.cmd("reboot download").exec();
                break;
            case 4:
                Shell.cmd("reboot bootloader").exec();
                break;
            case 5:
                Shell.cmd("reboot fastbootd").exec();
                break;
            default:
                Toast.makeText(this, "Unknown action", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void updateTile(String label, int iconResId, int state) {
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setLabel(label);
            tile.setIcon(Icon.createWithResource(this, iconResId));
            tile.setState(state);
            tile.updateTile();
        }
    }

    public String getSystemProperty(String key) {
        String value = null;
        try {
            value = (String) Class.forName("android.os.SystemProperties")
                    .getMethod("get", String.class).invoke(null, key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    private boolean hasRootAccess() {
        Shell.Result result = Shell.cmd("su").exec();
        return result.isSuccess();
    }
}
