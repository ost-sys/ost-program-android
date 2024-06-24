package com.ost.application.sudoku.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.tabs.TabLayout;

import com.ost.application.R;
import com.ost.application.sudoku.adapter.DialogViewPagerAdapter;
import com.ost.application.sudoku.game.Game;
import com.ost.application.sudoku.utils.DynamicViewPager;

public class NewSudokuDialog extends DialogFragment {

    private DialogListener dialogListener = game -> {
    };

    public void setDialogListener(DialogListener dialogListener) {
        this.dialogListener = dialogListener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();

        View root_view = getLayoutInflater().inflate(R.layout.dialog_new_sudoku_root, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.new_sudoku)
                .setView(root_view);

        TabLayout tabLayout = root_view.findViewById(R.id.dialog_tabLayout);
        DynamicViewPager viewPager = root_view.findViewById(R.id.dialog_viewPager);
        viewPager.setAdapter(new DialogViewPagerAdapter(getChildFragmentManager(), dialogListener, context));
        viewPager.setOffscreenPageLimit(2);
        tabLayout.setupWithViewPager(viewPager);

        return builder.create();
    }

    public interface DialogListener {
        void onResult(Game game);
    }
}
