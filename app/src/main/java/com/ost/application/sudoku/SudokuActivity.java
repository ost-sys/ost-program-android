package com.ost.application.sudoku;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.ost.application.BuildConfig;
import com.ost.application.GeminiActivity;
import com.ost.application.R;
import com.ost.application.sudoku.adapter.GamesListAdapter;
import com.ost.application.sudoku.adapter.SudokuViewAdapter;
import com.ost.application.sudoku.dialog.NewSudokuDialog;
import com.ost.application.sudoku.game.Field;
import com.ost.application.sudoku.game.Game;
import dev.oneuiproject.oneui.dialog.ProgressDialog;
import dev.oneuiproject.oneui.layout.DrawerLayout;
import dev.oneuiproject.oneui.utils.internal.ReflectUtils;

public class SudokuActivity extends AppCompatActivity {

    public static boolean colorSettingChanged = false;
    public static boolean gameSettingChanged = false;

    private DrawerLayout drawerLayout;
    private Menu toolbarMenu;
    private LinearLayout playOption;
    private LinearLayout solveOption;

    private Context context;
    private SharedPreferences sharedPref_Games;
    private SharedPreferences sharedPref_Settings;

    private List<Game> games;
    private RecyclerView games_list;
    private GamesListAdapter gamesListAdapter;

    private Game current_game;
    private RecyclerView game_recycler;
    private SudokuViewAdapter game_adapter;

    private LinearLayout resume_button_layout;
    private ProgressDialog mLoadingDialog;

    @SuppressLint({"RestrictedApi", "UseCompatLoadingForDrawables"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sudoku);
        context = this;
        sharedPref_Games = getSharedPreferences("Games", Activity.MODE_PRIVATE);
        sharedPref_Settings = PreferenceManager.getDefaultSharedPreferences(context);

        mLoadingDialog = new ProgressDialog(this);
        mLoadingDialog.setProgressStyle(ProgressDialog.STYLE_CIRCLE);
        mLoadingDialog.setCancelable(false);

        drawerLayout = findViewById(R.id.drawer_view);
        drawerLayout.setDrawerButtonIcon(getDrawable(R.drawable.ic_oui_settings_outline));
        drawerLayout.setDrawerButtonTooltip(getString(R.string.settings));
        drawerLayout.setDrawerButtonOnClickListener(v -> startActivity(new Intent().setClass(context, SudokuSettingsActivity.class)));

        resume_button_layout = findViewById(R.id.resume_button_layout);
        drawerLayout.getAppBarLayout().addOnOffsetChangedListener((layout, verticalOffset) -> {
            int totalScrollRange = layout.getTotalScrollRange();
            int inputMethodWindowVisibleHeight = (int) ReflectUtils.genericInvokeMethod(InputMethodManager.class, getSystemService(INPUT_METHOD_SERVICE), "getInputMethodWindowVisibleHeight");
            if (resume_button_layout != null) {
                if (totalScrollRange != 0) {
                    resume_button_layout.setTranslationY(((float) (Math.abs(verticalOffset) - totalScrollRange)) / 2.0f);
                } else {
                    resume_button_layout.setTranslationY(((float) (Math.abs(verticalOffset) - inputMethodWindowVisibleHeight)) / 2.0f);
                }
            }
        });

        drawerLayout.getToolbar().inflateMenu(R.menu.sudoku_menu);
        toolbarMenu = drawerLayout.getToolbar().getMenu();
        setSupportActionBar(null);

        playOption = findViewById(R.id.play_sudoku);
        solveOption = findViewById(R.id.solve_sudoku);

        playOption.setOnClickListener(v -> {
            playOption.setSelected(true);
            solveOption.setSelected(false);
            drawerLayout.setDrawerOpen(false, true);
            loadLastGame();
        });

        solveOption.setOnClickListener(v -> {
            solveOption.setSelected(true);
            playOption.setSelected(false);
            drawerLayout.setDrawerOpen(false, true);
            loadEmptyGame();
        });

        game_recycler = findViewById(R.id.game_recycler);

        games = new Gson().fromJson(sharedPref_Games.getString("games", "[]"), new TypeToken<List<Game>>() {
        }.getType());

        initDrawer();

        Game importedGame = getGameFromIntent(getIntent());
        if (importedGame != null) {
            addGameToList(importedGame);
            loadGame(importedGame);
        } else if (games.isEmpty()) {
            newSudokuDialog(false);
        } else if ("de.dlyt.yanndroid.sudoku.NEW_SUDOKU".equals(getIntent().getAction())) {
            newSudokuDialog(true);
        } else {
            loadLastGame();
        }
    }

    private void initDrawer() {
        games_list = findViewById(R.id.games_list);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        llm.setReverseLayout(true);
        games_list.setLayoutManager(llm);
        gamesListAdapter = new GamesListAdapter(context, games, new GamesListAdapter.GamesListListener() {
            @Override
            public void onNameChange(Game game) {
                if (game == current_game) drawerLayout.setTitle(current_game.getName());
            }

            @Override
            public void onGameDeleted(Game game) {
                if (game == current_game) {
                    current_game.stopTimer();
                    drawerLayout.setTitle("Sudoku");
                    setSubtitle(null);
                    current_game = null;
                    game_recycler.setAdapter(null);

                    showMenu(false, false, false);
                    resume_button_layout.setVisibility(View.GONE);
                }
            }
        });
        games_list.setAdapter(gamesListAdapter);
    }

    private void setSubtitle(CharSequence subtitle) {
        drawerLayout.setExpandedSubtitle(subtitle);

        TypedValue outValue = new TypedValue();
        getResources().getValue(R.dimen.sesl_appbar_height_proportion, outValue, true);
        drawerLayout.setCollapsedSubtitle(outValue.getFloat() == 0.0 ? subtitle:null);
    }

    public void newSudokuDialog(View view) {
        drawerLayout.setDrawerOpen(false, true);
        newSudokuDialog(true);
    }

    private void newSudokuDialog(boolean cancelable) {
        NewSudokuDialog newSudokuDialog = new NewSudokuDialog();
        newSudokuDialog.setCancelable(cancelable);
        newSudokuDialog.setDialogListener(game -> {
            newSudokuDialog.dismiss();
            game.setName("Sudoku " + (games.size() + 1));
            addGameToList(game);
            loadGame(game);
        });
        newSudokuDialog.show(getSupportFragmentManager(), "");
    }

    private void loadLastGame() {
        int index = sharedPref_Games.getInt("last_game_index", games.size() - 1);
        if (games.isEmpty()) {
            newSudokuDialog(false);
            return;
        }
        if (index == -1 || index >= games.size()) index = games.size() - 1;
        loadGame(games.get(index));
    }

    public void loadGame(Game game) {
        drawerLayout.setDrawerOpen(false, true);

        if (game == current_game) return;
        if (current_game != null) current_game.stopTimer();

        playOption.setSelected(true);
        solveOption.setSelected(false);

        current_game = game;
        drawerLayout.setTitle(game.getName());
        setSubtitle(getString(current_game.isCompleted() ? R.string.elapsed_time : R.string.current_time, current_game.getTimeString()));

        //recycler
        game_recycler.setLayoutManager(new GridLayoutManager(context, game.getSize()));
        game_adapter = new SudokuViewAdapter(context, game);
        game_recycler.setAdapter(game_adapter);
        game_recycler.seslSetFillBottomEnabled(true);
        game_recycler.seslSetLastRoundedCorner(true);

        //game
        showMenu(false, true, !current_game.isCompleted());
        current_game.setGameListener(new Game.GameListener() {
            @Override
            public void onHistoryChange(int length) {
                if (toolbarMenu != null) {
                    toolbarMenu.findItem(R.id.menu_undo).setEnabled(current_game.hasHistory());
                }
            }

            @Override
            public void onCompleted() {
                gamesListAdapter.notifyChanged();
                showMenu(false, true, !current_game.isCompleted());
            }

            @Override
            public void onTimeChanged(String time) {
                runOnUiThread(() -> setSubtitle(getString(R.string.current_time, time)));
            }
        });

        resumeGameTimer(null);
    }

    private void loadEmptyGame() {
        if (current_game != null) current_game.stopTimer();

        drawerLayout.setDrawerOpen(false, true);
        resume_button_layout.setVisibility(View.GONE);
        game_recycler.setVisibility(View.VISIBLE);

        showMenu(true, false, false);
        drawerLayout.setTitle(getString(R.string.solve_sudoku));
        setSubtitle(null);

        current_game = new Game(9);

        game_recycler.setLayoutManager(new GridLayoutManager(context, current_game.getSize()));
        game_adapter = new SudokuViewAdapter(context, current_game);
        game_recycler.setAdapter(game_adapter);
        game_recycler.seslSetFillBottomEnabled(true);
        game_recycler.seslSetLastRoundedCorner(true);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void popupGame(Game game) {
        RecyclerView game_view = new RecyclerView(context);
        game_view.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        game_view.setBackground(getDrawable(R.drawable.sudoku_view_popup_bg));
        game_view.setClipToOutline(true);
        game_view.setLayoutManager(new GridLayoutManager(context, game.getSize()));
        game_view.setAdapter(new SudokuViewAdapter(context, game));

        PopupWindow popupWindow = new PopupWindow(game_view, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setAnimationStyle(android.R.style.Animation_Dialog);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12.0F, this.context.getResources().getDisplayMetrics()));
        if (popupWindow.isClippingEnabled()) popupWindow.setClippingEnabled(false);
        popupWindow.showAtLocation(game_recycler, Gravity.CENTER, 0, 0);

        View container = popupWindow.getContentView().getRootView();
        WindowManager.LayoutParams wmlp = (WindowManager.LayoutParams) container.getLayoutParams();
        wmlp.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        wmlp.dimAmount = 0.6f;
        ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).updateViewLayout(container, wmlp);
    }

    private Game getGameFromIntent(Intent intent) {
        if (intent.getData() == null) return null;
        Log.d("intent data", String.valueOf(intent.getData()));
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(intent.getData())));
            StringBuilder content = new StringBuilder();
            for (String line; (line = bufferedReader.readLine()) != null; ) {
                content.append(line).append('\n');
            }

            Game game = new Gson().fromJson(content.toString(), Game.class);

            if (game.getFields() == null) throw new Exception(getString(R.string.invalid_game));
            Toast.makeText(context, getString(R.string.successfully_imported, game.getName()), Toast.LENGTH_SHORT).show();
            return game;
        } catch (Exception e) {
            Toast.makeText(context, getString(R.string.failed_to_import_game) + "\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("import failed", Objects.requireNonNull(e.getMessage()));
            return null;
        }
    }

    public void duplicateCurrentGame(View view) {
        if (current_game == null) return;
        Game duplicate = current_game.isCompleted() ? current_game.getInitialGame() : current_game.copy();
        duplicate.setName(duplicate.getName() + "Copy");
        addGameToList(duplicate);
    }

    public void shareCurrentGame(View view) {
        if (current_game == null) return;
        if (current_game.isCompleted()) {
            shareGame(current_game.getInitialGame());
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.share)
                .setNegativeButton(R.string.initial_game, (dialog, which) -> shareGame(current_game.getInitialGame()))
                .setPositiveButton(R.string.current_game, (dialog, which) -> shareGame(current_game))
                .show();
    }

    private void shareGame(Game game) {
        if (game == null) return;
        try {
            String destination = Objects.requireNonNull(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)) + "/" + game.getName() + ".sudoku";

            File file = new File(destination);
            if (file.exists()) file.delete();
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(new Gson().toJson(game));
            fileWriter.flush();
            fileWriter.close();

            Intent shareGame = new Intent(Intent.ACTION_SEND);
            Uri fileUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
            shareGame.setType("application/octet-stream");
            shareGame.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareGame.putExtra(Intent.EXTRA_STREAM, fileUri);

            for (ResolveInfo ri : getPackageManager().queryIntentActivities(shareGame, PackageManager.MATCH_DEFAULT_ONLY))
                grantUriPermission(ri.activityInfo.packageName, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareGame, getString(R.string.share_sudoku)));

        } catch (IOException e) {
            Log.e("share", Objects.requireNonNull(e.getMessage()));
        }
    }

    private void saveAllGames() {
        sharedPref_Games.edit()
                .putString("games", new Gson().toJson(games))
                .putInt("last_game_index", games.indexOf(current_game))
                .apply();
    }

    private void addGameToList(Game game) {
        games.add(game);
        //gamesListAdapter.notifyItemInserted(games.size() - 1);
        gamesListAdapter.notifyChanged();
        games_list.scrollToPosition(games.size() - 1);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public void resumeGameTimer(View view) {
        resume_button_layout.setVisibility(View.GONE);
        if (!(current_game == null || current_game.isCompleted() || current_game.isEditMode())) {
            current_game.startTimer(1500);

            MenuItem item_pause_play = toolbarMenu.findItem(R.id.menu_pause);
            if (item_pause_play != null) {
                item_pause_play.setIcon(getDrawable(R.drawable.ic_oui_control_pause));
                item_pause_play.setTitle(getString(R.string.pause));
            }
            if (toolbarMenu != null) {
                toolbarMenu.findItem(R.id.menu_undo).setEnabled(current_game.hasHistory());
                toolbarMenu.findItem(R.id.menu_show_errors).setEnabled(true);
            }
        }
        game_recycler.setVisibility(View.VISIBLE);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void pauseGameTimer() {
        if (current_game == null || current_game.isCompleted() || current_game.isEditMode()) return;
        game_recycler.setVisibility(View.GONE);
        current_game.stopTimer();

        MenuItem item_pause_play = toolbarMenu.findItem(R.id.menu_pause);
        if (item_pause_play != null) {
            item_pause_play.setIcon(getDrawable(R.drawable.ic_oui_control_play));
            item_pause_play.setTitle(getString(R.string.resume));
        }
        if (toolbarMenu != null) {
            toolbarMenu.findItem(R.id.menu_undo).setEnabled(false);
            toolbarMenu.findItem(R.id.menu_show_errors).setEnabled(false);
        }

        resume_button_layout.setVisibility(View.VISIBLE);
    }

    private void toggleGameTimer() {
        if (current_game.isTimerRunning()) pauseGameTimer();
        else resumeGameTimer(null);
    }

    private void showErrors() {
        if (current_game == null || current_game.isCompleted()) return;
        int size = current_game.getSize();
        Field[][] fields = current_game.getFields();

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                boolean wrong = (fields[i][j].getValue() != null && !fields[i][j].getValue().equals(fields[i][j].getSolution()));
                game_adapter.getFieldView(i * size + j).setBackground(wrong);
                fields[i][j].setError(wrong);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseGameTimer();
        saveAllGames();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sharedPref_Settings.getBoolean("secure_flag", true)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        game_recycler.setKeepScreenOn(sharedPref_Settings.getBoolean("keep_screen_on", true));

        if (colorSettingChanged) {
            colorSettingChanged = false;
            drawerLayout.setDrawerOpen(false, false);
            recreate();
        }
        if (gameSettingChanged) {
            gameSettingChanged = false;
            game_recycler.setAdapter(new SudokuViewAdapter(context, current_game));
        }
    }

    private void showMenu(boolean solver, boolean game, boolean playable) {
        if (toolbarMenu != null) {
            toolbarMenu.setGroupVisible(R.id.solve_menu, solver);
            toolbarMenu.setGroupVisible(R.id.menu_game, game);
            toolbarMenu.setGroupVisible(R.id.menu_game_playable, playable);
        }
    }

    @SuppressLint({"StaticFieldLeak", "NonConstantResourceId"})
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_undo) {
            if (current_game == null) return false;
            current_game.revertLastChange(game_adapter);
        } else if (item.getItemId() == R.id.menu_pause) {
            toggleGameTimer();
        } else if (item.getItemId() == R.id.menu_duplicate) {
            duplicateCurrentGame(null);
        } else if (item.getItemId() == R.id.menu_show_errors) {
            showErrors();
        } else if (item.getItemId() == R.id.menu_solve) {
            pauseGameTimer();
            popupGame(current_game.getSolutionGame());
        } else if (item.getItemId() == R.id.menu_share) {
            pauseGameTimer();
            shareCurrentGame(null);
        } else if (item.getItemId() == R.id.menu_clear) {
            loadEmptyGame();
        } else if (item.getItemId() == R.id.menu_solver_solve) {
            mLoadingDialog.show();
            new AsyncTask<Void, Void, Object>() {
                @Override
                protected Object doInBackground(Void... voids) {
                    return current_game.makeSolutionFromEdit(true);
                }

                @Override
                protected void onPostExecute(Object o) {
                    if (o instanceof Integer)
                        Toast.makeText(context, R.string.no_solution, Toast.LENGTH_SHORT).show();
                    else popupGame((Game) o);
                    mLoadingDialog.dismiss();
                }
            }.execute();
        } else if (item.getItemId() == R.id.menu_save) {
            mLoadingDialog.show();
            new AsyncTask<Void, Void, Object>() {
                @Override
                protected Object doInBackground(Void... voids) {
                    return current_game.makeGameFromEdit();
                }

                @Override
                protected void onPostExecute(Object o) {
                    if (o instanceof Integer)
                        Toast.makeText(context, (int) o == 0 ? R.string.no_solution : R.string.multiple_solutions, Toast.LENGTH_SHORT).show();
                    else {
                        ((Game) o).setName("Sudoku " + (games.size() + 1));
                        addGameToList((Game) o);
                        Toast.makeText(context, getString(R.string.game_added_to_list), Toast.LENGTH_SHORT).show();
                    }
                    mLoadingDialog.dismiss();
                }
            }.execute();
        } else if (item.getItemId() == R.id.menu_back) {
            onBackPressed();
        }
        return false;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sudoku_menu, menu);
        MenuCompat.setGroupDividerEnabled(menu, true);
        if (current_game != null) {
            current_game.setUndoMenuItem(menu.findItem(R.id.menu_undo));
        }
        return true;
    }
}