package com.ost.application.sudoku.dialog;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.ost.application.R;
import com.ost.application.sudoku.game.Game;
import com.ost.application.sudoku.utils.PropertiesListView;

public class Tab_Import extends Fragment {

    private static final int PICKER_REQUEST_CODE = 4441;
    private NewSudokuDialog.DialogListener dialogListener;

    private Button dialog_picker_pick;
    private Button dialog_picker_done;
    private PropertiesListView dialog_picker_text;

    public Tab_Import(NewSudokuDialog.DialogListener dialogListener) {
        this.dialogListener = dialogListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_new_sudoku_import, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dialog_picker_pick = view.findViewById(R.id.dialog_picker_pick);
        dialog_picker_done = view.findViewById(R.id.dialog_picker_done);
        dialog_picker_text = view.findViewById(R.id.dialog_picker_text);

        dialog_picker_pick.setOnClickListener(v -> {
            Intent filePicker = new Intent(Intent.ACTION_GET_CONTENT);
            filePicker.setType("application/octet-stream");
            startActivityForResult(Intent.createChooser(filePicker, getString(R.string.select_file)), PICKER_REQUEST_CODE);
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {

            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(getContext().getContentResolver().openInputStream(data.getData())));
                StringBuilder content = new StringBuilder();
                for (String line; (line = bufferedReader.readLine()) != null; ) {
                    content.append(line).append('\n');
                }

                Game game = new Gson().fromJson(content.toString(), Game.class);
                dialog_picker_text.setVisibility(View.VISIBLE);
                dialog_picker_text.clearList();

                if (game.getFields() == null) {
                    dialog_picker_text.addLine(getString(R.string.error), getString(R.string.invalid_game));
                    dialog_picker_done.setVisibility(View.GONE);
                    return;
                }

                dialog_picker_text.addLine(getString(R.string.name), game.getName());
                dialog_picker_text.addLine(getString(R.string.time), game.getTimeString());
                dialog_picker_text.addLine(getString(R.string.size), game.getSize() + "×" + game.getSize());
                dialog_picker_text.addLine(getString(R.string.difficulty), String.valueOf(game.getDifficulty()));

                dialog_picker_done.setVisibility(View.VISIBLE);
                dialog_picker_done.setOnClickListener(v -> dialogListener.onResult(game));

            } catch (Exception e) {
                dialog_picker_text.setVisibility(View.VISIBLE);
                dialog_picker_text.clearList();
                dialog_picker_text.addLine(getString(R.string.error), getString(R.string.invalid_game));
                dialog_picker_done.setVisibility(View.GONE);
            }

        }
    }

}