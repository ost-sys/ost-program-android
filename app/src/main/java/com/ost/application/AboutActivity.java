package com.ost.application;

import android.Manifest;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
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
import android.view.ViewGroup;
import com.ost.application.R;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

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

import dev.oneuiproject.oneui.utils.ViewUtils;
import dev.oneuiproject.oneui.utils.internal.ToolbarLayoutUtils;
import dev.oneuiproject.oneui.widget.Toast;

public class AboutActivity extends AppCompatActivity implements View.OnClickListener {
    private boolean mEnableBackToHeader;
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
    private ProgressDialog progressDialog;
    private long downloadId;
    private AlertDialog downloadDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        mBottomContent = mBinding.aboutBottomContent;

        setSupportActionBar(mBinding.aboutToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        mBinding.aboutToolbar.setNavigationOnClickListener(v -> onBackPressed());

        resetAppBar(getResources().getConfiguration());
        initContent();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions();
        }
    }
    private void requestPermissions() {
        // Запрос разрешения на запись во внешнее хранилище
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_STORAGE);
        }

        // Запрос разрешения на доступ ко всем файлам (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_ALL_FILES);
        }

        // Запрос разрешения на установку неизвестных приложений (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, PERMISSION_REQUEST_UNKNOWN_APPS);
            }
        }

        // Запрос разрешения на уведомления
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    PERMISSION_REQUEST_NOTIFICATION);
        }
    }

    private void checkUpdate() {
        new FetchLatestVersionTask().execute();
    }

    private class FetchLatestVersionTask extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            String checking_for_updates = getString(R.string.checking_for_updates);
            progressDialog = new ProgressDialog(AboutActivity.this);
            progressDialog.setMessage(checking_for_updates);
            progressDialog.setCancelable(false);
            progressDialog.show();
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

                // Парсим JSON ответ
                JSONArray releases = new JSONArray(response.toString());
                JSONObject latestRelease = releases.getJSONObject(0);
                latestVersionName = latestRelease.getString("tag_name");
                apkUrl = latestRelease.getJSONArray("assets").getJSONObject(0).getString("browser_download_url");

                return latestVersionName;

            } catch (IOException | JSONException e) {
                Log.e(TAG, getString(Integer.parseInt("Error getting version information:")) + e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.dismiss();

            if (result == null) {
                mBottomContent.aboutUpdate.setSummaryText(getString(R.string.error_getting_version_information));
                Toast.makeText(AboutActivity.this, R.string.error_getting_version_information, Toast.LENGTH_SHORT).show();
                return;
            }

            String currentVersionName = getCurrentVersionName();

            if (result.compareTo(currentVersionName) > 0) {
                mBottomContent.aboutUpdate.setSummaryText(getString(R.string.update_available));
                Toast.makeText(AboutActivity.this, R.string.update_available, Toast.LENGTH_SHORT).show();
                startDownload();
            } else {
                // Установлена последняя версия
                mBottomContent.aboutUpdate.setSummaryText(getString(R.string.latest_version_installed));
                Toast.makeText(AboutActivity.this, R.string.latest_version_installed, Toast.LENGTH_SHORT).show();
            }
        }
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
        // Создание запроса на загрузку
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
        request.setDescription(getString(R.string.downloading_update));
        request.setTitle(getString(R.string.update));
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "ost-program-android.apk");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setVisibleInDownloadsUi(true);

        // Запуск загрузки
        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        downloadId = downloadManager.enqueue(request);

        // Создание AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(AboutActivity.this);
        builder.setTitle(getString(R.string.downloading_update))
                .setMessage(getString(R.string.downloading_update))
                .setCancelable(false)
                .setNegativeButton(R.string.cancel, (dialog, id) -> {
                    // Отменяем загрузку
                    downloadManager.remove(downloadId);
                    Toast.makeText(AboutActivity.this, getString(R.string.download_canceled), Toast.LENGTH_SHORT).show();
                });
        downloadDialog = builder.create();
        downloadDialog.show();

        // Получение состояния загрузки
        new DownloadStatusTask().execute();
    }

    private class DownloadStatusTask extends AsyncTask<Void, Long, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

            while (true) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                Cursor cursor = downloadManager.query(query);
                if (cursor.moveToFirst()) {
                    int bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    int bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    int progress = (bytesDownloaded * 100) / bytesTotal;
                    publishProgress((long) progress);

                    if (bytesDownloaded == bytesTotal) {
                        // Загрузка завершена, выходим из цикла
                        break;
                    }
                }
                cursor.close();
                try {
                    Thread.sleep(100); // Пауза для обновления прогресса
                } catch (InterruptedException e) {
                    Log.e(TAG, "Error updating progress: " + e.getMessage());
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            super.onProgressUpdate(values);
            downloadDialog.setMessage(getString(R.string.downloading_update) + values[0] + "%");
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_REQUEST_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Разрешение на запись во внешнее хранилище предоставлено
                } else {
                    // Разрешение на запись во внешнее хранилище не предоставлено
                    Toast.makeText(AboutActivity.this, getString(R.string.write_to_external_storage_permission), Toast.LENGTH_SHORT).show();
                }
                break;
            case PERMISSION_REQUEST_ALL_FILES:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Разрешение на доступ ко всем файлам предоставлено
                } else {
                    // Разрешение на доступ ко всем файлам не предоставлено
                    Toast.makeText(AboutActivity.this, getString(R.string.access_to_all_files_permission), Toast.LENGTH_SHORT).show();
                }
                break;
            case PERMISSION_REQUEST_UNKNOWN_APPS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Разрешение на установку неизвестных приложений предоставлено
                } else {
                    // Разрешение на установку неизвестных приложений не предоставлено
                    Toast.makeText(AboutActivity.this, getString(R.string.install_unknown_apps_permission), Toast.LENGTH_SHORT).show();
                }
                break;
            case PERMISSION_REQUEST_NOTIFICATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Разрешение на уведомления предоставлено
                } else {
                    // Разрешение на уведомления не предоставлено
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
                // Разрешение на установку неизвестных приложений предоставлено
            } else {
                // Разрешение на установку неизвестных приложений не предоставлено
                Toast.makeText(AboutActivity.this, getString(R.string.install_unknown_apps_permission), Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    public void onBackPressed() {
        if (mEnableBackToHeader && mBinding.aboutAppBar.seslIsCollapsed()) {
            mBinding.aboutAppBar.setExpanded(true);
        } else {
            // Fix O memory leak
            if (Build.VERSION.SDK_INT
                    == Build.VERSION_CODES.O && isTaskRoot()) {
                finishAfterTransition();
            } else {
                super.onBackPressed();
            }
        }
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
        }
        return false;
    }

    @Override
    public boolean isInMultiWindowMode() {
        return Build.VERSION.SDK_INT >= 24 && super.isInMultiWindowMode();
    }
    private void resetAppBar(Configuration config) {
        ToolbarLayoutUtils.hideStatusBarForLandscape(this, config.orientation);
        ToolbarLayoutUtils.updateListBothSideMargin(this,
                mBinding.aboutBottomContainer);

        if (config.orientation != Configuration.ORIENTATION_LANDSCAPE
                && !isInMultiWindowMode()) {
            mBinding.aboutAppBar.seslSetCustomHeightProportion(true, 0.5f);
            mEnableBackToHeader = true;
            mBinding.aboutAppBar.addOnOffsetChangedListener(mAppBarListener);
            mBinding.aboutAppBar.setExpanded(true, false);
            mBinding.aboutSwipeUpContainer.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams lp = mBinding.aboutSwipeUpContainer.getLayoutParams();
            lp.height = getResources().getDisplayMetrics().heightPixels / 2;
        } else {
            mBinding.aboutAppBar.setExpanded(false, false);
            mBinding.aboutAppBar.seslSetCustomHeightProportion(true, 0);
            mEnableBackToHeader = false;
            mBinding.aboutAppBar.removeOnOffsetChangedListener(mAppBarListener);
            mBinding.aboutBottomContainer.setAlpha(1f);
            setBottomContentEnabled(true);
            mBinding.aboutSwipeUpContainer.setVisibility(View.GONE);
        }
    }

    private void initContent() {
        ViewUtils.semSetRoundedCorners(
                mBinding.aboutBottomContent.getRoot(),
                ViewUtils.SEM_ROUNDED_CORNER_TOP_LEFT | ViewUtils.SEM_ROUNDED_CORNER_TOP_RIGHT);
        ViewUtils.semSetRoundedCornerColor(mBinding.aboutBottomContent.getRoot(),
                ViewUtils.SEM_ROUNDED_CORNER_TOP_LEFT | ViewUtils.SEM_ROUNDED_CORNER_TOP_RIGHT,
                getColor(R.color.oui_round_and_bgcolor));

        Drawable appIcon = getDrawable(R.mipmap.ic_launcher);
        mBinding.aboutHeaderAppIcon.setImageDrawable(appIcon);
        mBinding.aboutBottomAppIcon.setImageDrawable(appIcon);

        mBinding.aboutHeaderAppVersion.setText(getString(R.string.version) + " " + BuildConfig.VERSION_NAME);
        mBinding.aboutBottomAppVersion.setText(getString(R.string.version) + " " + BuildConfig.VERSION_NAME);

        mBinding.aboutHeaderGithub.setOnClickListener(this);
        TooltipCompat.setTooltipText(mBinding.aboutHeaderGithub, getString(R.string.github));
        mBinding.aboutHeaderTelegram.setOnClickListener(this);
        TooltipCompat.setTooltipText(mBinding.aboutHeaderTelegram, getString(R.string.telegram));
        mBinding.aboutHeaderYoutube.setOnClickListener(this);
        TooltipCompat.setTooltipText(mBinding.aboutHeaderYoutube, getString(R.string.youtube));

        mBottomContent.aboutBottomOst.setOnClickListener(this);
        mBottomContent.aboutUpdate.setOnClickListener(v -> checkUpdate());

        mBottomContent.translatorUk.setOnClickListener(this);
        mBottomContent.translatorKk.setOnClickListener(this);

        mBottomContent.aboutBottomDevYann.setOnClickListener(this);
        mBottomContent.aboutBottomDevMesa.setOnClickListener(this);

        mBottomContent.aboutBottomOssProgram.setOnClickListener(this);
        mBottomContent.aboutBottomOssApache.setOnClickListener(this);
        mBottomContent.aboutBottomOssMit.setOnClickListener(this);

        mBottomContent.aboutBottomRelativeOstGithub.setOnClickListener(this);
        mBottomContent.aboutBottomRelativeJetpack.setOnClickListener(this);
        mBottomContent.aboutBottomRelativeMaterial.setOnClickListener(this);
        mBottomContent.aboutBottomRelativeOuip.setOnClickListener(this);
    }

    private void setBottomContentEnabled(boolean enabled) {
        mBinding.aboutHeaderGithub.setEnabled(!enabled);
        mBinding.aboutHeaderTelegram.setEnabled(!enabled);
        mBinding.aboutHeaderYoutube.setEnabled(!enabled);
        mBottomContent.aboutBottomOst.setEnabled(enabled);
        mBottomContent.aboutUpdate.setEnabled(enabled);
        mBottomContent.translatorUk.setEnabled(enabled);
        mBottomContent.translatorKk.setEnabled(enabled);
        mBottomContent.aboutBottomDevYann.setEnabled(enabled);
        mBottomContent.aboutBottomDevMesa.setEnabled(enabled);
        mBottomContent.aboutBottomOssProgram.setEnabled(enabled);
        mBottomContent.aboutBottomOssApache.setEnabled(enabled);
        mBottomContent.aboutBottomOssMit.setEnabled(enabled);
        mBottomContent.aboutBottomRelativeOstGithub.setEnabled(enabled);
        mBottomContent.aboutBottomRelativeJetpack.setEnabled(enabled);
        mBottomContent.aboutBottomRelativeMaterial.setEnabled(enabled);
        mBottomContent.aboutBottomRelativeOuip.setEnabled(enabled);
    }

    @Override
    public void onClick(View v) {
        long uptimeMillis = SystemClock.uptimeMillis();
        if (uptimeMillis - mLastClickTime > 600L) {
            String url = null;
            if (v.getId() == mBinding.aboutHeaderGithub.getId()) {
                url = "https://github.com/OneUIProject/oneui-design";
            } else if (v.getId() == mBinding.aboutHeaderTelegram.getId()) {
                url = "https://t.me/oneuiproject";
            } else if (v.getId() == mBinding.aboutHeaderYoutube.getId()) {
                url = "https://www.youtube.com/channel/UC6wNi6iQFVSnd-eJivuG3_Q";
            } else if (v.getId() == mBottomContent.aboutBottomOst.getId()) {
                url = "https://github.com/ost-sys/";
            } else if (v.getId() == mBottomContent.translatorUk.getId()) {
                url = "https://github.com/Anton-Aboba1234";
            } else if (v.getId() == mBottomContent.translatorKk.getId()) {
                url = "https://www.youtube.com/@Mazurobi";
            } else if (v.getId() == mBottomContent.aboutBottomDevYann.getId()) {
                url = "https://github.com/Yanndroid";
            } else if (v.getId() == mBottomContent.aboutBottomDevMesa.getId()) {
                url = "https://github.com/XDABlackMesa123";
            } else if (v.getId() == mBottomContent.aboutBottomOssProgram.getId()) {
                url = "https://github.com/ost-sys/ost-program-android";
            } else if (v.getId() == mBottomContent.aboutBottomOssApache.getId()) {
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt";
            } else if (v.getId() == mBottomContent.aboutBottomOssMit.getId()) {
                url = "https://github.com/OneUIProject/sesl/blob/main/LICENSE";
            } else if (v.getId() == mBottomContent.aboutBottomRelativeOstGithub.getId()) {
                url = "https://github.com/ost-sys/ost-program-android";
            } else if (v.getId() == mBottomContent.aboutBottomRelativeJetpack.getId()) {
                url = "https://developer.android.com/jetpack";
            } else if (v.getId() == mBottomContent.aboutBottomRelativeMaterial.getId()) {
                url = "https://material.io/develop/android";
            } else if (v.getId() == mBottomContent.aboutBottomRelativeOuip.getId()) {
                url = "https://github.com/OneUIProject";
            }

            if (url != null) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(
                            this, "No suitable activity found", Toast.LENGTH_SHORT).show();
                }
            }
        }
        mLastClickTime = uptimeMillis;
    }

    private class AboutAppBarListener implements AppBarLayout.OnOffsetChangedListener {
        @Override
        public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
            // Handle the SwipeUp anim view
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

            // Handle the bottom part of the UI
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
        }
    }
}