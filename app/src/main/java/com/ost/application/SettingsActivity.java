package com.ost.application;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.util.SeslMisc;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import dev.oneuiproject.oneui.base.FragmentInfo;
import dev.oneuiproject.oneui.layout.ToolbarLayout;
import dev.oneuiproject.oneui.preference.HorizontalRadioPreference;
import dev.oneuiproject.oneui.preference.internal.PreferenceRelatedCard;
import dev.oneuiproject.oneui.utils.DarkModeUtils;

public class SettingsActivity extends AppCompatActivity {

    ToolbarLayout toolbarLayout;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
            Preference.OnPreferenceChangeListener  {

        private Context mContext;
        private PreferenceRelatedCard mRelativeLinkCard;

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
            int darkMode = DarkModeUtils.getDarkMode(mContext);

            HorizontalRadioPreference darkModePref = findPreference("dark_mode");
            darkModePref.setOnPreferenceChangeListener( this);
            darkModePref.setDividerEnabled(false);
            darkModePref.setTouchEffectEnabled(false);
            darkModePref.setEnabled(darkMode != DarkModeUtils.DARK_MODE_AUTO);
            darkModePref.setValue(SeslMisc.isLightTheme(mContext) ? "0" : "1");

            SwitchPreferenceCompat autoDarkModePref = findPreference("dark_mode_auto");
            autoDarkModePref.setOnPreferenceChangeListener( this);
            autoDarkModePref.setChecked(darkMode == DarkModeUtils.DARK_MODE_AUTO);
        }

        public boolean onPreferenceClick(Preference preference) {
            return false;
        }

        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String currentDarkMode = String.valueOf(DarkModeUtils.getDarkMode(mContext));
            HorizontalRadioPreference darkModePref = findPreference("dark_mode");

            switch (preference.getKey()) {
                case "dark_mode":
                    if (currentDarkMode != newValue) {
                        DarkModeUtils.setDarkMode((AppCompatActivity) requireActivity(), newValue.equals("0")
                                ? DarkModeUtils.DARK_MODE_DISABLED
                                : DarkModeUtils.DARK_MODE_ENABLED);
                    }
                    return true;
                case "dark_mode_auto":
                    if ((boolean) newValue) {
                        darkModePref.setEnabled(false);
                        DarkModeUtils.setDarkMode((AppCompatActivity) requireActivity(),
                                DarkModeUtils.DARK_MODE_AUTO);
                    } else {
                        darkModePref.setEnabled(true);
                    }
                    return true;
            }

            return false;
        }
    }
}