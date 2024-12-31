package com.ost.application;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SeslProgressBar;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.ost.application.databinding.ActivityAboutBinding;
import com.ost.application.databinding.ActivityAboutContentBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import dev.oneuiproject.oneui.layout.internal.util.ToolbarLayoutUtils;
import dev.oneuiproject.oneui.widget.TipPopup;
import dev.oneuiproject.oneui.widget.Toast;

public class AboutActivity extends AppCompatActivity implements OnClickListener {
    private long mLastClickTime;
    private ActivityAboutBinding mBinding;
    private ActivityAboutContentBinding mBottomContent;
    private final AboutAppBarListener mAppBarListener = new AboutAppBarListener();
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_STORAGE = 1;
    private static final int PERMISSION_REQUEST_ALL_FILES = 2;
    private static final int PERMISSION_REQUEST_UNKNOWN_APPS = 3;
    private static final int PERMISSION_REQUEST_NOTIFICATION = 4;
    private String latestVersionName;
    private String apkUrl;
    private String changelog;
    private long downloadId;
    private AlertDialog downloadDialog;
    int update = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        if (Build.VERSION.SDK_INT >= 30 && !getWindow().getDecorView().getFitsSystemWindows()) {
            mBinding.getRoot().setOnApplyWindowInsetsListener((v, insets) -> {
                Insets systemBarsInsets = WindowInsetsCompat.toWindowInsetsCompat(insets)
                        .getInsets(WindowInsetsCompat.Type.systemBars());
                mBinding.getRoot().setPadding(systemBarsInsets.left, systemBarsInsets.top,
                        systemBarsInsets.right, systemBarsInsets.bottom);
                return insets;
            });
        }

        mBottomContent = mBinding.aboutBottomContent;

        setSupportActionBar(mBinding.aboutToolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        mBinding.aboutToolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        resetAppBar(getResources().getConfiguration());
        initContent();
        initOnBackPressed();
        checkUpdate();

    }

    private void checkUpdate() {
        update = 1;
        new FetchLatestVersionTask().execute();
    }

    @SuppressLint("StaticFieldLeak")
    private class FetchLatestVersionTask extends AsyncTask<Void, Void, String> {
        private String errorMessage = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (!isNetworkAvailable()) {
                errorMessage = getString(R.string.no_internet_connection_detected);
                cancel(true);
                return;
            }

            mBinding.checkingUpdateMain.setVisibility(View.VISIBLE);
            mBinding.checkingUpdateSecond.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL("https://api.github.com/repos/ost-sys/ost-program-android/releases");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONArray releases = new JSONArray(response.toString());
                JSONObject latestRelease = releases.getJSONObject(0);
                latestVersionName = latestRelease.getString("tag_name");
                apkUrl = latestRelease.getJSONArray("assets").getJSONObject(0).getString("browser_download_url");
                changelog = latestRelease.getString("body");

                return latestVersionName;

            } catch (IOException e) {
                errorMessage = getString(R.string.no_internet_connection_detected) + ": " + e.getMessage();
            } catch (JSONException e) {
                errorMessage = getString(R.string.data_analysis_error) + ": " + e.getMessage();
            }
            return null;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (errorMessage != null) {
                Toast.makeText(AboutActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                mBinding.checkingUpdateMain.setVisibility(View.GONE);
                mBinding.checkingUpdateSecond.setVisibility(View.GONE);
                return;
            }

            if (result == null) {
                Toast.makeText(AboutActivity.this, R.string.error_getting_version_information, Toast.LENGTH_SHORT).show();
                mBinding.checkingUpdateMain.setVisibility(View.GONE);
                mBinding.checkingUpdateSecond.setVisibility(View.GONE);
                return;
            }

            String currentVersionName = getCurrentVersionName();

            if (result.compareTo(Objects.requireNonNull(currentVersionName)) > 0) {
                mBinding.aboutHeaderAppVersion.setText(getString(R.string.version) + " " + BuildConfig.VERSION_NAME + " (" + getString(R.string.update_available) + ")");
                mBinding.aboutBottomAppVersion.setText(getString(R.string.version) + " " + BuildConfig.VERSION_NAME + " (" + getString(R.string.update_available) + ")");
                mBinding.checkingUpdateMain.setVisibility(View.GONE);
                mBinding.checkingUpdateSecond.setVisibility(View.GONE);
                TipPopup tipPopup = new TipPopup(mBinding.aboutHeaderAppVersion, TipPopup.MODE_NORMAL);
                tipPopup.setExpanded(true);
                tipPopup.setMessage(getString(R.string.update_available) + " " + getString(R.string.install_update_q));
                tipPopup.setAction(getString(R.string.install), v -> new AlertDialog.Builder(AboutActivity.this)
                        .setTitle(getString(R.string.update_available))
                        .setMessage(getString(R.string.install_update_q) + " " + latestVersionName + "\n\n" + changelog)
                        .setPositiveButton(R.string.install, (dialog, id) -> {
                            if (isNetworkAvailable() && changelog != null) {
                                startDownload();
                            } else if (BuildConfig.VERSION_NAME.equals(latestVersionName)) {
                                TipPopup q = new TipPopup(mBinding.aboutHeaderAppIcon, TipPopup.MODE_NORMAL);
                                q.setMessage(getString(R.string.latest_version_installed));
                                q.setExpanded(true);
                                q.show(TipPopup.DIRECTION_DEFAULT);
                            } else {
                                TipPopup q = new TipPopup(mBinding.aboutHeaderAppIcon, TipPopup.MODE_NORMAL);
                                q.setMessage(getString(R.string.are_you_kidding_me));
                                q.setExpanded(true);
                                q.show(TipPopup.DIRECTION_DEFAULT);
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show());
                tipPopup.show(TipPopup.DIRECTION_DEFAULT);
            } else {
                mBinding.aboutHeaderAppVersion.setText(getString(R.string.version) + " " + BuildConfig.VERSION_NAME + " (" + getString(R.string.latest_version_installed) + ")");
                mBinding.aboutBottomAppVersion.setText(getString(R.string.version) + " " + BuildConfig.VERSION_NAME + " (" + getString(R.string.latest_version_installed) + ")");
                mBinding.checkingUpdateMain.setVisibility(View.GONE);
                mBinding.checkingUpdateSecond.setVisibility(View.GONE);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mBinding.checkingUpdateMain.setVisibility(View.GONE);
            mBinding.checkingUpdateSecond.setVisibility(View.GONE);
            if (errorMessage != null) {
                Toast.makeText(AboutActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                mBinding.checkingUpdateMain.setVisibility(View.GONE);
                mBinding.checkingUpdateSecond.setVisibility(View.GONE);
            }
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    private String getCurrentVersionName() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting current version: " + e.getMessage());
            return null;
        }
    }

    private void startDownload() {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
            request.setDescription(getString(R.string.downloading_update));
            request.setTitle(getString(R.string.update));
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "ost-program-android.apk");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setVisibleInDownloadsUi(true);

            View dialogView = getLayoutInflater().inflate(R.layout.update_dialog, null);

            SeslProgressBar progressBar = dialogView.findViewById(R.id.update_progressbar);

            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            downloadId = downloadManager.enqueue(request);

            AlertDialog.Builder builder = new AlertDialog.Builder(AboutActivity.this);
            builder.setTitle(getString(R.string.downloading_update))
                    .setMessage(getString(R.string.downloading_update))
                    .setCancelable(false)
                    .setView(dialogView)
                    .setNegativeButton(R.string.cancel, (dialog, id) -> {
                        downloadManager.remove(downloadId);
                        Toast.makeText(this, getString(R.string.download_canceled), Toast.LENGTH_SHORT).show();
                    });
            downloadDialog = builder.create();
            downloadDialog.show();

            new DownloadStatusTask(progressBar).execute();
        } catch (SecurityException e) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_STORAGE);
            }
            Toast.makeText(this, getString(R.string.storage_permission_r), Toast.LENGTH_SHORT).show();
        }

    }

    @SuppressLint("StaticFieldLeak")
    private class DownloadStatusTask extends AsyncTask<Void, Long, Void> {
        private final SeslProgressBar progressBar;

        public DownloadStatusTask(SeslProgressBar progressBar) {
            this.progressBar = progressBar;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

            while (true) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                Cursor cursor = downloadManager.query(query);
                if (cursor.moveToFirst()) {
                    @SuppressLint("Range") int bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    @SuppressLint("Range") int bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                    if (bytesTotal == 0) {
                        break;
                    }

                    int progress = (bytesDownloaded * 100) / bytesTotal;
                    publishProgress((long) progress);

                    if (bytesDownloaded == bytesTotal) {
                        break;
                    }
                }
                cursor.close();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Error updating progress: " + e.getMessage());
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            super.onProgressUpdate(values);
            downloadDialog.setMessage(getString(R.string.downloading_update) + " " + values[0] + "%");
            progressBar.setProgress(Math.toIntExact(values[0]));
        }

        @Override
        protected void onPostExecute(Void result) {
            File apkFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ost-program-android.apk");
            if (apkFile.exists()) {
                Uri apkUri = FileProvider.getUriForFile(AboutActivity.this, "com.ost.application.fileprovider", apkFile);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            } else {
                Toast.makeText(AboutActivity.this, getString(R.string.update_file_not_found), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_REQUEST_STORAGE:
                break;
            case PERMISSION_REQUEST_ALL_FILES:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(AboutActivity.this, getString(R.string.access_to_all_files_permission), Toast.LENGTH_SHORT).show();
                }
                break;
            case PERMISSION_REQUEST_UNKNOWN_APPS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(AboutActivity.this, getString(R.string.install_unknown_apps_permission), Toast.LENGTH_SHORT).show();
                }
                break;
            case PERMISSION_REQUEST_NOTIFICATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(AboutActivity.this, getString(R.string.notification_permission), Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PERMISSION_REQUEST_UNKNOWN_APPS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Settings.canDrawOverlays(this)) {

            } else {
                Toast.makeText(AboutActivity.this, getString(R.string.install_unknown_apps_permission), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private OnBackPressedCallback mBackPressedCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            mBinding.aboutAppBar.setExpanded(true);
        }
    };

    private void initOnBackPressed() {
        getOnBackPressedDispatcher().addCallback(this, mBackPressedCallback);
        mBackPressedCallback.setEnabled(mBinding.aboutAppBar.seslIsCollapsed()
                && getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE
                && !isInMultiWindowMode());
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        resetAppBar(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_about, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_app_info) {
            Intent intent = new Intent(
                    "android.settings.APPLICATION_DETAILS_SETTINGS",
                    Uri.fromParts("package", BuildConfig.APPLICATION_ID, null));
            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.check_update) {
            mBinding.checkingUpdateMain.setVisibility(View.GONE);
            mBinding.checkingUpdateSecond.setVisibility(View.GONE);
            if (update == 0 && isNetworkAvailable()) {
                checkUpdate();
            } else if ((update == 0 && !isNetworkAvailable()) || changelog == null) {
                TipPopup q = new TipPopup(mBinding.aboutHeaderAppIcon, TipPopup.MODE_NORMAL);
                q.setMessage(getString(R.string.no_internet_connection_detected));
                q.setExpanded(true);
                q.show(TipPopup.DIRECTION_DEFAULT);
                mBinding.checkingUpdateMain.setVisibility(View.GONE);
                mBinding.checkingUpdateSecond.setVisibility(View.GONE);
            } else if ((update == 1 && !isNetworkAvailable()) || changelog == null) {
                TipPopup q = new TipPopup(mBinding.aboutHeaderAppIcon, TipPopup.MODE_NORMAL);
                q.setMessage(getString(R.string.no_internet_connection_detected));
                q.setExpanded(true);
                q.show(TipPopup.DIRECTION_DEFAULT);
                mBinding.checkingUpdateMain.setVisibility(View.GONE);
                mBinding.checkingUpdateSecond.setVisibility(View.GONE);
            } else if (BuildConfig.VERSION_NAME.equals(latestVersionName)) {
                TipPopup q = new TipPopup(mBinding.aboutHeaderAppIcon, TipPopup.MODE_NORMAL);
                q.setMessage(getString(R.string.latest_version_installed));
                q.setExpanded(true);
                q.show(TipPopup.DIRECTION_DEFAULT);
                mBinding.checkingUpdateMain.setVisibility(View.GONE);
                mBinding.checkingUpdateSecond.setVisibility(View.GONE);
            } else if (!BuildConfig.VERSION_NAME.equals(latestVersionName)){
                new AlertDialog.Builder(AboutActivity.this)
                        .setTitle(getString(R.string.update_available))
                        .setMessage(getString(R.string.install_update_q) + " " + latestVersionName + "\n\n" + changelog)
                        .setPositiveButton(R.string.install, (dialog, id) -> {
                            if (isNetworkAvailable() && changelog != null) {
                                startDownload();
                            } else if (BuildConfig.VERSION_NAME.equals(latestVersionName)) {
                                TipPopup q = new TipPopup(mBinding.aboutHeaderAppIcon, TipPopup.MODE_NORMAL);
                                q.setMessage(getString(R.string.latest_version_installed));
                                q.setExpanded(true);
                                q.show(TipPopup.DIRECTION_DEFAULT);
                            } else {
                                TipPopup q = new TipPopup(mBinding.aboutHeaderAppIcon, TipPopup.MODE_NORMAL);
                                q.setMessage(getString(R.string.are_you_kidding_me));
                                q.setExpanded(true);
                                q.show(TipPopup.DIRECTION_DEFAULT);
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show();
                mBinding.checkingUpdateMain.setVisibility(View.GONE);
                mBinding.checkingUpdateSecond.setVisibility(View.GONE);
            } else {
                new AlertDialog.Builder(AboutActivity.this)
                        .setTitle(getString(R.string.version) + ": " + BuildConfig.VERSION_NAME)
                        .setMessage(changelog)
                        .setPositiveButton("OK", null)
                        .show();
                mBinding.checkingUpdateMain.setVisibility(View.GONE);
                mBinding.checkingUpdateSecond.setVisibility(View.GONE);
            }
        } else if (item.getItemId() == R.id.menu_main_changelog) {
            Intent intent = new Intent(AboutActivity.this, ChangelogActivity.class);
            startActivity(intent);
        }
        return false;
    }

    @Override
    public boolean isInMultiWindowMode() {
        return super.isInMultiWindowMode();
    }
    @SuppressLint("RestrictedApi")
    private void resetAppBar(Configuration config) {
        ToolbarLayoutUtils.hideStatusBarForLandscape(this, config.orientation);
        ToolbarLayoutUtils.updateListBothSideMargin(this,
                mBinding.aboutBottomContainer);

        if (config.orientation != Configuration.ORIENTATION_LANDSCAPE
                && !isInMultiWindowMode()) {
            mBinding.aboutAppBar.seslSetCustomHeightProportion(true, 0.5f);
            mBinding.aboutAppBar.addOnOffsetChangedListener(mAppBarListener);
            mBinding.aboutAppBar.setExpanded(true, false);
            mBinding.aboutSwipeUpContainer.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams lp = mBinding.aboutSwipeUpContainer.getLayoutParams();
            lp.height = getResources().getDisplayMetrics().heightPixels / 2;
        } else {
            mBinding.aboutAppBar.setExpanded(false, false);
            mBinding.aboutAppBar.seslSetCustomHeightProportion(true, 0);
            mBinding.aboutAppBar.removeOnOffsetChangedListener(mAppBarListener);
            mBinding.aboutBottomContainer.setAlpha(1f);
            setBottomContentEnabled(true);
            mBinding.aboutSwipeUpContainer.setVisibility(View.GONE);
        }
    }

    @SuppressLint("SetTextI18n")
    private void initContent() {

        List<String> messages = Arrays.asList(
                getString(R.string.callback_ping),
                "sesl6",
                "It's me, OST Tools",
                "You got notification!",
                "Привет, мир!",
                getString(R.string.success),
                "S C H L E C K",
                "Who are you?",
                "Operating System Tester",
                "Subscribe to my channel please :D",
                "Access denied",
                "I know you here!",
                "I sent your IP-address моему создателю! Жди докс",
                "0x000000",
                "OK Google",
                "ыыыыыыыыыыыыы",
                "java.lang.NoClassDefFoundError: com.sun.jna.Native",
                "Блять",
                "Hello and, again, welcome to the Aperture Science computer-aided enrichment center.",
                "Easier to assimilate than explain anyway",
                "User is dead",
                getString(R.string.grant_permission_to_continue),
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
        );

        @SuppressLint("UseCompatLoadingForDrawables") Drawable appIcon = getDrawable(R.mipmap.ic_launcher);
        mBinding.aboutHeaderAppIcon.setImageDrawable(appIcon);
        mBinding.aboutHeaderAppIcon.setOnClickListener(view -> {
            Random random = new Random();
            int randomIndex = random.nextInt(messages.size());
            String randomMessage = messages.get(randomIndex);
            TipPopup tipPopup = new TipPopup(view, TipPopup.MODE_NORMAL);
            tipPopup.setMessage(randomMessage);
            tipPopup.setExpanded(true);
            tipPopup.show(TipPopup.DIRECTION_DEFAULT);
        });
        mBinding.aboutBottomAppIcon.setImageDrawable(appIcon);

        mBinding.aboutHeaderAppVersion.setText(getString(R.string.version) + " " + BuildConfig.VERSION_NAME);
        mBinding.aboutBottomAppVersion.setText(getString(R.string.version) + " " + BuildConfig.VERSION_NAME);

        mBinding.aboutHeaderGithub.setOnClickListener(this);
        TooltipCompat.setTooltipText(mBinding.aboutHeaderGithub, getString(R.string.github));
        mBinding.aboutHeaderTelegram.setOnClickListener(this);
        TooltipCompat.setTooltipText(mBinding.aboutHeaderTelegram, getString(R.string.telegram));
        mBinding.aboutHeaderYoutube.setOnClickListener(this);
        TooltipCompat.setTooltipText(mBinding.aboutHeaderYoutube, getString(R.string.youtube));
        mBinding.aboutHeaderTt.setOnClickListener(this);
        TooltipCompat.setTooltipText(mBinding.aboutHeaderTt, getString(R.string.tiktok));
        mBinding.aboutHeaderWebsite.setOnClickListener(this);
        TooltipCompat.setTooltipText(mBinding.aboutHeaderWebsite, getString(R.string.website));

        mBottomContent.aboutBottomOst.setOnClickListener(this);

        mBottomContent.aboutBottomDevYann.setOnClickListener(this);
        mBottomContent.aboutBottomDevSalvo.setOnClickListener(this);
        mBottomContent.aboutBottomDevTribalfs.setOnClickListener(this);

        mBottomContent.aboutBottomOssProgram.setOnClickListener(this);
        mBottomContent.aboutBottomOssApache.setOnClickListener(this);
        mBottomContent.aboutBottomOssMit.setOnClickListener(this);

        mBottomContent.aboutBottomRelativeJetpack.setOnClickListener(this);
        mBottomContent.aboutBottomRelativeMaterial.setOnClickListener(this);
        mBottomContent.aboutBottomRelativeSeslAndroidx.setOnClickListener(this);
        mBottomContent.aboutBottomRelativeSeslMaterial.setOnClickListener(this);
        mBottomContent.aboutBottomRelativeDesign6.setOnClickListener(this);
    }

    private void setBottomContentEnabled(boolean enabled) {
        mBinding.aboutHeaderGithub.setEnabled(!enabled);
        mBinding.aboutHeaderTelegram.setEnabled(!enabled);
        mBinding.aboutHeaderYoutube.setEnabled(!enabled);
        mBinding.aboutHeaderTt.setEnabled(!enabled);
        mBinding.aboutHeaderWebsite.setEnabled(!enabled);
        mBottomContent.aboutBottomOst.setEnabled(enabled);
        mBottomContent.aboutBottomDevYann.setEnabled(enabled);
        mBottomContent.aboutBottomDevSalvo.setEnabled(enabled);
        mBottomContent.aboutBottomDevTribalfs.setEnabled(enabled);
        mBottomContent.aboutBottomOssProgram.setEnabled(enabled);
        mBottomContent.aboutBottomOssApache.setEnabled(enabled);
        mBottomContent.aboutBottomOssMit.setEnabled(enabled);
        mBottomContent.aboutBottomRelativeJetpack.setEnabled(enabled);
        mBottomContent.aboutBottomRelativeMaterial.setEnabled(enabled);
    }

    @Override
    public void onClick(View v) {
        long uptimeMillis = SystemClock.uptimeMillis();
        if (uptimeMillis - mLastClickTime > 600L) {
            String url = null;
            if (v.getId() == mBinding.aboutHeaderGithub.getId()) {
                url = "https://github.com/ost-sys/";
            } else if (v.getId() == mBinding.aboutHeaderTelegram.getId()) {
                url = "https://t.me/ost_news5566";
            } else if (v.getId() == mBinding.aboutHeaderYoutube.getId()) {
                url = "https://www.youtube.com/channel/UC6wNi6iQFVSnd-eJivuG3_Q";
            } else if (v.getId() == mBinding.aboutHeaderTt.getId()) {
                url = "https://www.tiktok.com/@ost5566?_t=8qVU4QZzE3n&_r=1";
            } else if (v.getId() == mBinding.aboutHeaderWebsite.getId()) {
                url = "https://ost-sys.github.io";
            } else if (v.getId() == mBottomContent.aboutBottomOst.getId()) {
                url = "https://github.com/ost-sys/";
            } else if (v.getId() == mBottomContent.aboutBottomDevYann.getId()) {
                url = "https://github.com/Yanndroid";
            } else if (v.getId() == mBottomContent.aboutBottomDevSalvo.getId()) {
                url = "https://github.com/salvogiangri";
            } else if (v.getId() == mBottomContent.aboutBottomDevTribalfs.getId()) {
                url = "https://github.com/tribalfs";
            } else if (v.getId() == mBottomContent.aboutBottomOssProgram.getId()) {
                url = "https://github.com/ost-sys/ost-program-android";
            } else if (v.getId() == mBottomContent.aboutBottomOssApache.getId()) {
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt";
            } else if (v.getId() == mBottomContent.aboutBottomOssMit.getId()) {
                url = "https://github.com/OneUIProject/sesl/blob/main/LICENSE";
            } else if (v.getId() == mBottomContent.aboutBottomRelativeJetpack.getId()) {
                url = "https://developer.android.com/jetpack";
            } else if (v.getId() == mBottomContent.aboutBottomRelativeMaterial.getId()) {
                url = "https://material.io/develop/android";
            } else if (v.getId() == mBottomContent.aboutBottomRelativeSeslAndroidx.getId()) {
                url = "https://github.com/tribalfs/sesl-androidx";
            } else if (v.getId() == mBottomContent.aboutBottomRelativeSeslMaterial.getId()) {
                url = "https://github.com/tribalfs/sesl-material-components-android";
            } else if (v.getId() == mBottomContent.aboutBottomRelativeDesign6.getId()) {
                url = "https://github.com/tribalfs/oneui-design";
            } else if (v.getId() == mBottomContent.aboutBottomDevTribalfs.getId()) {
                url = "https://github.com/tribalfs";
            }

            if (url != null) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, getString(R.string.no_suitable_activity_found), Toast.LENGTH_SHORT).show();
                }
            }
        }
        mLastClickTime = uptimeMillis;
    }

    private class AboutAppBarListener implements AppBarLayout.OnOffsetChangedListener {
        @Override
        public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
            final int totalScrollRange = appBarLayout.getTotalScrollRange();
            final int abs = Math.abs(verticalOffset);

            if (abs >= totalScrollRange / 2) {
                mBinding.aboutSwipeUpContainer.setAlpha(0f);
                setBottomContentEnabled(true);
            } else if (abs == 0) {
                mBinding.aboutSwipeUpContainer.setAlpha(1f);
                setBottomContentEnabled(false);
            } else {
                float offsetAlpha = (appBarLayout.getY() / totalScrollRange);
                float arrowAlpha = 1 - (offsetAlpha * -3);
                if (arrowAlpha < 0) {
                    arrowAlpha = 0;
                } else if (arrowAlpha > 1) {
                    arrowAlpha = 1;
                }
                mBinding.aboutSwipeUpContainer.setAlpha(arrowAlpha);
            }

            final float alphaRange = mBinding.aboutCtl.getHeight() * 0.143f;
            final float layoutPosition = Math.abs(appBarLayout.getTop());
            float bottomAlpha = (150.0f / alphaRange)
                    * (layoutPosition - (mBinding.aboutCtl.getHeight() * 0.35f));

            if (bottomAlpha < 0) {
                bottomAlpha = 0;
            } else if (bottomAlpha >= 255) {
                bottomAlpha = 255;
            }

            mBinding.aboutBottomContainer.setAlpha(bottomAlpha / 255);

            mBackPressedCallback.setEnabled(appBarLayout.getTotalScrollRange() + verticalOffset == 0);
        }
    }
}