package com.ost.application.sudoku;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.ost.application.R;

import dev.oneuiproject.oneui.layout.ToolbarLayout;

public class SudokuSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_sudoku);

        ToolbarLayout toolbarLayout = findViewById(R.id.toolbar_layout);
        toolbarLayout.setNavigationButtonAsBack();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.settings, new SettingsFragment()).commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

        private Context mContext;
        private SudokuSettingsActivity mActivity;
        private SharedPreferences sharedPreferences;

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            mContext = getContext();
            if (getActivity() instanceof SudokuSettingsActivity)
                mActivity = ((SudokuSettingsActivity) getActivity());
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String str) {
            addPreferencesFromResource(R.xml.preferences);
        }

        @SuppressLint("RestrictedApi")
        @Override
        public void onCreate(Bundle bundle) {
            super.onCreate(bundle);

            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

            /*initGameColorPicker("hint_color", "recent_hint_colors", R.color.blue);
            initGameColorPicker("error_color", "recent_error_colors", R.color.sesl_error_color);*/

            findPreference("grid_box_colors").setOnPreferenceChangeListener(this);
            findPreference("invert_colors").setOnPreferenceChangeListener(this);

            findPreference("how_to_play").setOnPreferenceClickListener(var1 -> {
                new AlertDialog.Builder(mContext)
                        .setTitle(R.string.how_to_play)
                        .setMessage(R.string.how_to_play_summary)
                        .setPositiveButton(R.string.dismiss, null)
                        .show();
                return true;
            });

            findPreference("restore_all_settings").setOnPreferenceClickListener(var1 -> {
                new AlertDialog.Builder(mContext)
                        .setTitle(R.string.restore_all_settings)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            mContext.getSharedPreferences("ThemeColor", Context.MODE_PRIVATE).edit().clear().apply();
                            sharedPreferences.edit().clear().apply();

                            SudokuActivity.colorSettingChanged = true;
                            SudokuActivity.gameSettingChanged = true;
                            mActivity.recreate();
                        })
                        .show();
                return true;
            });
        }

        /*private void initGameColorPicker(String prefKey, String shprKey, @ColorRes int defaultColor) {
            ColorPickerPreference themeColorPickerPref = (ColorPickerPreference) findPreference(prefKey);
            ArrayList<Integer> recent_colors = new Gson().fromJson(sharedPreferences.getString(shprKey, new Gson().toJson(new int[]{getResources().getColor(defaultColor, mContext.getTheme())})), new TypeToken<ArrayList<Integer>>() {
            }.getType());
            for (Integer recent_color : recent_colors)
                themeColorPickerPref.onColorChanged(recent_color);

            themeColorPickerPref.setOnPreferenceChangeListener((var1, var2) -> {
                recent_colors.add((Integer) var2);
                sharedPreferences.edit().putString("recent_hint_colors", new Gson().toJson(recent_colors)).apply();
                MainActivity.gameSettingChanged = true;
                return true;
            });
        }*/

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            getView().setBackgroundColor(getResources().getColor(R.color.oui_background_color, mContext.getTheme()));
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {

            switch (preference.getKey()) {
                case "grid_box_colors":
                case "invert_colors":
                    SudokuActivity.gameSettingChanged = true;
                    return true;
            }

            return false;
        }

    }
}