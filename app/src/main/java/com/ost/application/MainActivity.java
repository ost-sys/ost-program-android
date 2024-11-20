package com.ost.application;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ost.application.databinding.ActivityMainBinding;
import com.ost.application.ui.core.DarkModeUtils;
import com.ost.application.ui.core.drawer.DrawerListAdapter;
import com.ost.application.ui.fragment.applist.AppListFragment;
import com.ost.application.ui.fragment.phoneinfo.BatteryInfoFragment;
import com.ost.application.ui.fragment.phoneinfo.CPUInfoFragment;
import com.ost.application.ui.fragment.phoneinfo.CameraInfoFragment;
import com.ost.application.ui.fragment.phoneinfo.DefaultInfoFragment;
import com.ost.application.ui.fragment.FriendsListFragment;
import com.ost.application.ui.fragment.PowerMenuFragment;
import com.ost.application.ui.fragment.InfoFragment;
import com.ost.application.ui.fragment.phoneinfo.DisplayInfoFragment;
import com.ost.application.ui.fragment.phoneinfo.NetworkInfoFragment;
import com.topjohnwu.superuser.Shell;

import java.util.ArrayList;
import java.util.List;

import com.ost.application.ui.core.base.FragmentInfo;

import dev.oneuiproject.oneui.layout.DrawerLayout;
import dev.oneuiproject.oneui.utils.ActivityUtils;
import dev.oneuiproject.oneui.widget.Toast;

public class MainActivity extends AppCompatActivity implements DrawerListAdapter.DrawerListener {

    private ActivityMainBinding mBinding;
    private FragmentManager mFragmentManager;
    private final List<Fragment> fragments = new ArrayList<>();

    @SuppressLint({"ShowToast", "MissingInflatedId", "WrongThread"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DarkModeUtils.setDarkMode(this, DarkModeUtils.getDarkMode(this));
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        EdgeToEdge.enable(this);

        initFragmentList();
        initDrawer();
        initFragments();

    }

    private void initFragmentList() {
        fragments.add(new DefaultInfoFragment());
        fragments.add(new CPUInfoFragment());
        fragments.add(new BatteryInfoFragment());
        fragments.add(new DisplayInfoFragment());
        fragments.add(new NetworkInfoFragment());
        fragments.add(new CameraInfoFragment());
        fragments.add(null);
        fragments.add(new PowerMenuFragment());
        fragments.add(new AppListFragment());
        fragments.add(null);
        fragments.add(new FriendsListFragment());
        fragments.add(new InfoFragment());
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void initDrawer() {
        mBinding.drawerLayout.setDrawerButtonIcon(getDrawable(dev.oneuiproject.oneui.R.drawable.ic_oui_settings_outline));
        mBinding.drawerLayout.setDrawerButtonTooltip(getString(R.string.settings));
        mBinding.drawerLayout.setDrawerButtonOnClickListener(v ->
                ActivityUtils.startPopOverActivity(this,
                        new Intent(MainActivity.this, SettingsActivity.class),
                        null,
                        ActivityUtils.POP_OVER_POSITION_TOP | ActivityUtils.POP_OVER_POSITION_CENTER_HORIZONTAL));
        mBinding.drawerListView.setLayoutManager(new LinearLayoutManager(this));
        mBinding.drawerListView.setAdapter(new DrawerListAdapter(this, fragments, this));
        mBinding.drawerListView.setItemAnimator(null);
        mBinding.drawerListView.setHasFixedSize(true);
        mBinding.drawerListView.seslSetLastRoundedCorner(false);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuCompat.setGroupDividerEnabled(menu, true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_check_root) {
            Shell.Result result = Shell.cmd("su").exec();
            if (result.isSuccess()) {
                Toast.makeText(this, R.string.success, Toast.LENGTH_SHORT).show();
            }
        }
        return false;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    private void initFragments() {
        mFragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        for (Fragment fragment : fragments) {
            if (fragment != null) transaction.add(R.id.main_content, fragment);
        }
        transaction.commit();
        mFragmentManager.executePendingTransactions();

        onDrawerItemSelected(0);
    }

    @Override
    public boolean onDrawerItemSelected(int position) {
        Fragment newFragment = fragments.get(position);
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        for (Fragment fragment : mFragmentManager.getFragments()) {
            transaction.hide(fragment);
        }
        transaction.show(newFragment).commit();

        if (newFragment instanceof FragmentInfo) {
            if (!((FragmentInfo) newFragment).isAppBarEnabled()) {
                mBinding.drawerLayout.setExpanded(false, false);
                mBinding.drawerLayout.setExpandable(false);
            } else {
                mBinding.drawerLayout.setExpandable(true);
                mBinding.drawerLayout.setExpanded(false, false);
            }
            mBinding.drawerLayout.setTitle(getString(R.string.app_name), ((FragmentInfo) newFragment).getTitle());
            mBinding.drawerLayout.setExpandedSubtitle(((FragmentInfo) newFragment).getTitle());
        }
        mBinding.drawerLayout.setDrawerOpen(false, true);

        return true;
    }

    public DrawerLayout getDrawerLayout() {
        return mBinding.drawerLayout;
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            int res = checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE);
            if (res != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.READ_PHONE_STATE}, 123);
            }

        }
    }
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1002;

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_ASK_PERMISSIONS) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), getString(R.string.read_phone_state_denied), Toast.LENGTH_SHORT)
                        .show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}