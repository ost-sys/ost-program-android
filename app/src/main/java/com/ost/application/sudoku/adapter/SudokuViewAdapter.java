package com.ost.application.sudoku.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;

import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ost.application.sudoku.game.FieldView;
import com.ost.application.sudoku.game.Game;

public class SudokuViewAdapter extends RecyclerView.Adapter<SudokuViewAdapter.ViewHolder> {

    private Context context;
    private SharedPreferences sharedPreferences;
    private boolean animateLayoutChanges_FieldView;

    private Game game;
    private FieldView[] fieldViews;

    public SudokuViewAdapter(Context context, Game game) {
        this(context, game, true);
    }

    public SudokuViewAdapter(Context context, Game game, boolean animateLayoutChanges_FieldView) {
        this.context = context;
        this.game = game;
        this.animateLayoutChanges_FieldView = animateLayoutChanges_FieldView;
        fieldViews = new FieldView[getItemCount()];
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public FieldView getFieldView(int position) {
        return fieldViews[position];
    }

    public boolean isAnimateLayoutChanges_FieldView() {
        return animateLayoutChanges_FieldView;
    }

    public void updateFieldView(int position) {
        fieldViews[position].init(game, position, this);
    }

    @Override
    public int getItemCount() {
        return (int) Math.pow(game.getSize(), 2);
    }

    @Override
    public void onBindViewHolder(SudokuViewAdapter.ViewHolder holder, int position) {
        fieldViews[position] = ((FieldView) holder.itemView);
        ((FieldView) holder.itemView).init(game, position, this);
    }

    @Override
    public SudokuViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SudokuViewAdapter.ViewHolder(new FieldView(context, sharedPreferences));
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
