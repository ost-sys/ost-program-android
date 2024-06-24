package com.ost.application.sudoku.dialog;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SeslSeekBar;
import androidx.fragment.app.Fragment;

import com.ost.application.R;
import com.ost.application.sudoku.game.Game;
import dev.oneuiproject.oneui.dialog.ProgressDialog;
import dev.oneuiproject.oneui.utils.SeekBarUtils;

public class Tab_Generate extends Fragment {

    private NewSudokuDialog.DialogListener dialogListener;
    private SeslSeekBar difficulty_seekbar;

    public Tab_Generate(NewSudokuDialog.DialogListener dialogListener) {
        this.dialogListener = dialogListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_new_sudoku_generate, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        difficulty_seekbar = view.findViewById(R.id.dialog_difficulty_seekbar);
        SeekBarUtils.showTickMark(difficulty_seekbar, true);
        difficulty_seekbar.setSeamless(true);
        difficulty_seekbar.setMax(4);
        difficulty_seekbar.setProgress(difficulty_seekbar.getMax() / 2);

        View button4x4 = view.findViewById(R.id.dialog_generate_4x4);
        View button9x9 = view.findViewById(R.id.dialog_generate_9x9);

        button4x4.setOnClickListener(v -> asyncNewGame(4, difficulty_seekbar.getProgress(), difficulty_seekbar.getMax()));
        button9x9.setOnClickListener(v -> asyncNewGame(9, difficulty_seekbar.getProgress(), difficulty_seekbar.getMax()));
    }

    @SuppressLint("StaticFieldLeak")
    private void asyncNewGame(int size, int difficulty, int max) {
        ProgressDialog mLoadingDialog = new ProgressDialog(getContext());
        mLoadingDialog.setProgressStyle(ProgressDialog.STYLE_CIRCLE);
        mLoadingDialog.setCancelable(false);

        int diff = difficulty == max ? -1 : (int) (Math.pow(size, 2) * (difficulty * 0.35f / max + 0.4f));

        new AsyncTask<Void, Void, Game>() {
            @Override
            protected Game doInBackground(Void... voids) {
                return new Game(size, diff);
            }

            @Override
            protected void onPostExecute(Game game) {
                super.onPostExecute(game);
                assert getActivity() != null;
                getActivity().runOnUiThread(() -> {
                    dialogListener.onResult(game);
                    mLoadingDialog.dismiss();
                });
            }
        }.execute();

        mLoadingDialog.show();
    }
}