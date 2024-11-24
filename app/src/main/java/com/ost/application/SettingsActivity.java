package com.ost.application;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.util.SeslMisc;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;

import com.ost.application.ui.core.base.FragmentInfo;
import dev.oneuiproject.oneui.layout.ToolbarLayout;
import dev.oneuiproject.oneui.preference.HorizontalRadioPreference;
import dev.oneuiproject.oneui.widget.Toast;

import com.ost.application.ui.core.DarkModeUtils;

public class SettingsActivity extends AppCompatActivity {

    ToolbarLayout toolbarLayout;
    private static final int REQUEST_WRITE_SETTINGS = 100;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DarkModeUtils.setDarkMode(this, DarkModeUtils.getDarkMode(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        toolbarLayout = findViewById(R.id.toolbarLayout);
        toolbarLayout.setNavigationButtonOnClickListener(n -> onBackPressed());
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements FragmentInfo, Preference.OnPreferenceClickListener,
            Preference.OnPreferenceChangeListener {

        private Context mContext;
        private static final int REQUEST_WRITE_SETTINGS = 100;

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            mContext = context;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }

        @Override
        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);
            initPreferences();
        }

        @Override
        public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            getView().setBackgroundColor(mContext.getColor(R.color.oui_background_color));
            getListView().seslSetLastRoundedCorner(false);
        }

        @Override
        public void onResume() {
            initPreferences();
            super.onResume();
        }

        public int getLayoutResId() {
            return -1;
        }

        public int getIconResId() {
            return R.drawable.ic_oui_settings_outline;
        }

        public boolean isAppBarEnabled() {
            return true;
        }

        @Override
        public CharSequence getTitle() {
            return null;
        }

        @SuppressLint("RestrictedApi")
        private void initPreferences() {
            SwitchPreferenceCompat brightnessControlPref = findPreference("brightness_control");
            if (brightnessControlPref != null) {
                brightnessControlPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean isEnabled = (boolean) newValue;
                    if (isEnabled && !Settings.System.canWrite(requireContext())) {
                        requestWriteSettingsPermission();
                        return false;
                    }
                    return true;
                });
            }

            Preference aboutAppPreference = findPreference("about_app");
            if (aboutAppPreference != null) {
                aboutAppPreference.setOnPreferenceClickListener(this);
            }

            int darkMode = DarkModeUtils.getDarkMode(mContext);

            HorizontalRadioPreference darkModePref = findPreference("darkMode");
            darkModePref.setOnPreferenceChangeListener(this);
            darkModePref.setDividerEnabled(false);
            darkModePref.setTouchEffectEnabled(false);
            darkModePref.setEnabled(darkMode != DarkModeUtils.DARK_MODE_AUTO);
            darkModePref.setValue(SeslMisc.isLightTheme(mContext) ? "0" : "1");

            SwitchPreferenceCompat autoDarkModePref = findPreference("darkModeAuto");
            autoDarkModePref.setOnPreferenceChangeListener(this);
            autoDarkModePref.setChecked(darkMode == DarkModeUtils.DARK_MODE_AUTO);


            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            // Consistent key names
            SeekBarPreference durationTotalPref = findPreference("total_duration");
            if (durationTotalPref != null) {
                durationTotalPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    sharedPreferences.edit().putInt("total_duration", (int) newValue).commit();
                    return true;
                });
            }

            SeekBarPreference durationNoisePref = findPreference("noise_duration");
            if (durationNoisePref != null) {
                durationNoisePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    sharedPreferences.edit().putInt("noise_duration", (int) newValue).commit();
                    return true;
                });
            }

            SeekBarPreference durationHorizontalLinesPref = findPreference("duration_horizontal_lines");
            if (durationHorizontalLinesPref != null) {
                durationHorizontalLinesPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    sharedPreferences.edit().putInt("duration_horizontal_lines", (int) newValue).commit();
                    return true;
                });
            }

            SeekBarPreference durationVerticalLinesPref = findPreference("duration_vertical_lines");
            if (durationVerticalLinesPref != null) {
                durationVerticalLinesPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    sharedPreferences.edit().putInt("duration_vertical_lines", (int) newValue).commit();
                    return true;
                });
            }
        }

        private void requestWriteSettingsPermission() {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
            startActivityForResult(intent, REQUEST_WRITE_SETTINGS);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == REQUEST_WRITE_SETTINGS) {
                SwitchPreferenceCompat brightnessControlPref = findPreference("brightness_control");
                if (brightnessControlPref != null) {
                    if (Settings.System.canWrite(requireContext())) {
                        brightnessControlPref.setChecked(true);
                    } else {
                        brightnessControlPref.setChecked(false);
                        Toast.makeText(requireContext(), getString(R.string.brightness_permission_r), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }

        public boolean onPreferenceClick(Preference preference) {
            if (preference.getKey().equals("about_app")) {
                Intent intent = new Intent(requireContext(), AboutActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        }

        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String currentDarkMode = String.valueOf(DarkModeUtils.getDarkMode(mContext));
            HorizontalRadioPreference darkModePref = findPreference("darkMode");

            return switch (preference.getKey()) {
                case "darkMode" -> {
                    if (currentDarkMode != newValue) {
                        DarkModeUtils.setDarkMode((AppCompatActivity) requireActivity(), newValue.equals("0")
                                ? DarkModeUtils.DARK_MODE_DISABLED
                                : DarkModeUtils.DARK_MODE_ENABLED);
                    }
                    yield true;
                }
                case "darkModeAuto" -> {
                    if ((boolean) newValue) {
                        darkModePref.setEnabled(false);
                        DarkModeUtils.setDarkMode((AppCompatActivity) requireActivity(),
                                DarkModeUtils.DARK_MODE_AUTO);
                    } else {
                        darkModePref.setEnabled(true);
                    }
                    yield true;
                }
                default -> false;
            };
        }
    }

}