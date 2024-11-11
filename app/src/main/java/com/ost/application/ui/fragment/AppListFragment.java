package com.ost.application.ui.fragment;

import static dev.oneuiproject.oneui.widget.Toast.makeText;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.menu.SeslMenuItem;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.SeslProgressBar;
import androidx.apppickerview.widget.AppPickerView;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;

import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.ost.application.MainActivity;
import com.ost.application.R;
import com.topjohnwu.superuser.Shell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ost.application.ui.core.base.BaseFragment;

import dev.oneuiproject.oneui.layout.DrawerLayout;
import dev.oneuiproject.oneui.widget.TipPopup;
import dev.oneuiproject.oneui.widget.Toast;

public class AppListFragment extends BaseFragment
        implements AppPickerView.OnBindListener, AdapterView.OnItemSelectedListener {
    private boolean mListInitialized = false;
    private int mListType = AppPickerView.TYPE_LIST;
    private boolean mShowSystemApps = false;

    private final List<Boolean> mItems = new ArrayList<>();
    private boolean mIsAllAppsSelected = false;
    private int mCheckedPosition = 0;

    private AppPickerView mAppPickerView;
    private SeslProgressBar mProgress;
    private DrawerLayout drawerLayout;

    public String appName = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        drawerLayout = ((MainActivity)getActivity()).findViewById(R.id.drawerLayout);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mProgress = view.findViewById(R.id.apppicker_progress);
        mAppPickerView = view.findViewById(R.id.apppicker_list);
        mAppPickerView.setItemAnimator(null);
        mAppPickerView.seslSetSmoothScrollEnabled(true);
        initSpinner(view);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && !mListInitialized) {
            fillListView();
            mListInitialized = true;
        }
        if (!hidden) {
            requireActivity().addMenuProvider(menuProvider, getViewLifecycleOwner(), Lifecycle.State.STARTED);
        } else {
            requireActivity().removeMenuProvider(menuProvider);
        }
    }

    private MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            MenuItem menuItem = menu.findItem(R.id.menu_apppicker_system);
            MenuItem refresh = menu.findItem(R.id.menu_app_refresh);
            menuItem.setVisible((Shell.cmd("su").exec()).isSuccess());
            refresh.setVisible(true);
        }
        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem item) {
            if (item.getItemId() == R.id.menu_apppicker_system) {
                mShowSystemApps = !mShowSystemApps;
                if (mShowSystemApps) {
                    item.setTitle(R.string.hide_system_apps);
                } else {
                    item.setTitle(R.string.show_system_apps);
                }

                refreshListView();
                return true;
            } else if (item.getItemId() == R.id.menu_app_refresh) {
                mAppPickerView.setSearchFilter(appName);
            }

            return false;
        }
    };

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_app_list;
    }

    @Override
    public int getIconResId() {
        return dev.oneuiproject.oneui.R.drawable.ic_oui_all_apps;
    }

    @Override
    public CharSequence getTitle() {
        return getString(R.string.app_list);
    }

    private void initSpinner(@NonNull View view) {
        AppCompatSpinner spinner = view.findViewById(R.id.apppicker_spinner);

        List<String> categories = new ArrayList<>();
        categories.add(getString(R.string.list));
        categories.add(getString(R.string.list_action_button));

        ArrayAdapter<String> adapter
                = new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mListType = position;
        fillListView();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) { }

    private void fillListView() {
        mIsAllAppsSelected = false;
        showProgressCircle(true);
        new Thread(() -> {
            if (!mListInitialized) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) { }
            }
            requireActivity().runOnUiThread(() -> {
                ArrayList<String> installedAppSet
                        = new ArrayList<>(getInstalledPackageNameUnmodifiableSet());

                if (mAppPickerView.getItemDecorationCount() > 0) {
                    for (int i = 0; i < mAppPickerView.getItemDecorationCount(); i++) {
                        mAppPickerView.removeItemDecorationAt(i);
                    }
                }

                mAppPickerView.setAppPickerView(mListType,
                        installedAppSet, AppPickerView.ORDER_ASCENDING_IGNORE_CASE);
                mAppPickerView.setOnBindListener(AppListFragment.this);

                mItems.clear();
                if (mListType == AppPickerView.TYPE_LIST_CHECKBOX_WITH_ALL_APPS
                        || mListType == AppPickerView.TYPE_LIST_SWITCH_WITH_ALL_APPS) {
                    mItems.add(Boolean.FALSE);
                }
                for (String app : installedAppSet) {
                    mItems.add(Boolean.FALSE);
                }

                showProgressCircle(false);
            });
        }).start();
    }

    private void refreshListView() {
        showProgressCircle(true);
        new Thread(() -> requireActivity().runOnUiThread(() -> {
            ArrayList<String> installedAppSet
                    = new ArrayList<>(getInstalledPackageNameUnmodifiableSet());
            mAppPickerView.resetPackages(installedAppSet);

            mItems.clear();
            if (mListType == AppPickerView.TYPE_LIST_CHECKBOX_WITH_ALL_APPS
                    || mListType == AppPickerView.TYPE_LIST_SWITCH_WITH_ALL_APPS) {
                mItems.add(Boolean.FALSE);
            }
            for (String app : installedAppSet) {
                mItems.add(Boolean.FALSE);
            }

            showProgressCircle(false);
        })).start();
    }

    @Override
    public void onBindViewHolder(AppPickerView.ViewHolder holder,
                                 int position, String packageName) {
        switch (mListType) {
            case AppPickerView.TYPE_LIST: {
                holder.getItem().setOnLongClickListener(view -> {
                    Toast.makeText(mContext, appName, Toast.LENGTH_SHORT);
                    return true;
                });
                holder.getItem().setOnClickListener(view -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + packageName));

                    startActivity(intent);
                });
            } break;

            case AppPickerView.TYPE_LIST_ACTION_BUTTON: {
                holder.getActionButton().setOnClickListener(view -> {
                    if (Shell.cmd("su").exec().isSuccess()) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                        alert.setTitle(packageName)
                                .setMessage(R.string.application_action_selection)
                                .setPositiveButton(R.string.uninstall, ((dialog, which) -> Shell.cmd("pm uninstall" + " " + packageName).exec()))
                                .setNegativeButton(R.string.disable, ((dialog, which) -> Shell.cmd("pm disable-user" + " " + packageName).exec()))
                                .create()
                                .show();
                    } else {
                        TipPopup tipPopup = new TipPopup(holder.getActionButton(), TipPopup.MODE_NORMAL);
                        tipPopup.setMessage(getString(R.string.feature_unavailable_root));
                        tipPopup.show(TipPopup.DIRECTION_BOTTOM_RIGHT);
                    }
                });

                holder.getItem().setOnClickListener(view -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + packageName));

                    startActivity(intent);
                });
            } break;
        }
    }

    private void checkAllAppsToggle() {
        mIsAllAppsSelected = true;
        for (boolean selected : mItems) {
            if (!selected) {
                mIsAllAppsSelected = false;
                break;
            }
        }
        mAppPickerView.refreshUI(0);
    }

    private void showProgressCircle(boolean show) {
        mProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        mAppPickerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private Set<String> getInstalledPackageNameUnmodifiableSet() {
        HashSet<String> set = new HashSet<>();
        for (ApplicationInfo appInfo : getInstalledAppList()) {
            set.add(appInfo.packageName);
        }
        return Collections.unmodifiableSet(set);
    }

    private List<ApplicationInfo> getInstalledAppList() {
        ArrayList<ApplicationInfo> list = new ArrayList<>();
        List<ApplicationInfo> apps = mContext.getPackageManager()
                .getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo appInfo : apps) {
            if ((appInfo.flags & (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
                    | ApplicationInfo.FLAG_SYSTEM)) > 0 && !mShowSystemApps) {
                continue;
            }
            list.add(appInfo);
        }
        return list;
    }
}
// code source - https://github.com/tribalfs/oneui-design/blob/b4c72ff197610eacd54dbfe2977b1ade510b9bc2/sample-app/src/main/java/dev/oneuiproject/oneuiexample/activity/SampleAboutActivity.java
// by OneUI Design Team