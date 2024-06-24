package com.ost.application;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ost.application.sudoku.SudokuActivity;
import com.ost.application.ui.friends.FriendsFragment;
import com.ost.application.ui.home.HomeFragment;
import com.ost.application.ui.info.InfoFragment;
import com.ost.application.ui.utilities.UtilitiesFragment;

import java.util.ArrayList;
import java.util.List;

import dev.oneuiproject.oneui.layout.ToolbarLayout;

public class MainActivity extends AppCompatActivity {
    private final String[] facts = {
            "Android was originally developed as an OS for digital cameras",
            "Bill Gates originally planned to name Windows as 'Interface Manager'",
            "But it was many times better before, don't you think?",
            "Samsung says it started One UI development from scratch...",
            "Windows 1.0 was less than 1MB in size and had a 16-bit color interface",
            "ERR_FACTS_DID_NOT_LOAD",
            "Will we wait for the day when Samsung completely abandons Exynos? Or they make it better...",
            "Shell.HyperOS == Shell.MIUI",
            "The design of the first prototype Android smartphone was very similar to the Blackberry.",
            "There are statues of Android on the grounds of Google headquarters that refer to each version of the OS.",
            "Turn the Mi logo upside down. You get the character '心', which translates to 'heart'.",
            "Windows 10 wallpapers are not computer graphics. The logo was photographed!"
    };
    int randomIndex = (int) (Math.random() * facts.length);
    String randomFact = facts[randomIndex];
    BottomNavigationView bottomNavigationView;
    ToolbarLayout toolbarLayout;

    @SuppressLint({"ShowToast", "MissingInflatedId", "WrongThread"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.tabs_bottomnav_icon);
        toolbarLayout = findViewById(R.id.toolbarLayout);

        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

        Intent geminiShortcut = new Intent(this, GeminiActivity.class);
        geminiShortcut.setAction(Intent.ACTION_VIEW);
        Intent sudokuShortcut = new Intent(this, SudokuActivity.class);
        sudokuShortcut.setAction(Intent.ACTION_VIEW);

        ShortcutInfo gemini = new ShortcutInfo.Builder(this, "shortcut_gemini")
                .setShortLabel(getString(R.string.gemini))
                .setLongLabel(getString(R.string.gemini))
                .setIcon(Icon.createWithResource(this, R.drawable.gemini_ask_btn_shortcut))
                .setIntent(geminiShortcut)
                .build();

        ShortcutInfo sudoku = new ShortcutInfo.Builder(this, "shortcut_sudoku_new")
                .setShortLabel("Sudoku")
                .setLongLabel("Sudoku")
                .setIcon(Icon.createWithResource(this, R.drawable.sudoku_shortcut_new_sudoku))
                .setIntent(sudokuShortcut)
                .build();

        List<ShortcutInfo> shortcutInfoList = new ArrayList<>();
        shortcutInfoList.add(gemini);
        shortcutInfoList.add(sudoku);
        shortcutManager.setDynamicShortcuts(shortcutInfoList);

        toolbarLayout.setTitle(getString(R.string.home));
        toolbarLayout.setExpandedSubtitle(randomFact);
        toolbarLayout.setNavigationButtonOnClickListener(v -> onBackPressed());

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.nav_host_fragment_activity_main, new HomeFragment()).commit();

        bottomNavigationView.setOnItemSelectedListener(menuItem -> {
            Fragment fragment = getFragmentBottomNavView(menuItem);
            if (fragment != null) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.nav_host_fragment_activity_main, fragment);
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                transaction.commit();
            }
            return true;
        });
    }
    public Fragment getFragmentBottomNavView(MenuItem menuItem) {
        toolbarLayout.findViewById(R.id.toolbarLayout);
        Fragment fragment = null;
        int itemId = menuItem.getItemId();
        if (itemId == R.id.navigation_home) {
            toolbarLayout.setTitle(getString(R.string.home));
            toolbarLayout.setExpandedSubtitle(randomFact);
            fragment = new HomeFragment();
        } else if (itemId == R.id.navigation_utilities) {
            toolbarLayout.setTitle(getString(R.string.utilities));
            toolbarLayout.setExpandedSubtitle(getString(R.string.utilities_info));
            fragment = new UtilitiesFragment();
        } else if (itemId == R.id.navigation_friends) {
            toolbarLayout.setTitle(getString(R.string.friends));
            toolbarLayout.setExpandedSubtitle(getString(R.string.friends_info));
            fragment = new FriendsFragment();
        } else if (itemId == R.id.navigation_info) {
            toolbarLayout.setTitle(getString(R.string.information));
            toolbarLayout.setExpandedSubtitle(getString(R.string.information_info));
            fragment = new InfoFragment();
        }
        return fragment;
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuCompat.setGroupDividerEnabled(menu, true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_about_app) {
            startActivity(new Intent(this, AboutActivity.class));
        } else if (item.getItemId() == R.id.menu_gemini) {
            startActivity(new Intent(this, GeminiActivity.class));
        } else if (item.getItemId() == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (item.getItemId() == R.id.menu_math_game) {
            startActivity(new Intent(this, MathGame.class));
        } else if (item.getItemId() == R.id.menu_sudoku) {
            startActivity(new Intent(this, SudokuActivity.class));
        }
        return false;
    }
}