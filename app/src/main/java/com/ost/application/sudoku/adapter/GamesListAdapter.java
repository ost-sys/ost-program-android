package com.ost.application.sudoku.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import com.ost.application.R;
import com.ost.application.sudoku.SudokuActivity;
import com.ost.application.sudoku.game.Game;
import dev.oneuiproject.oneui.utils.DialogUtils;

public class GamesListAdapter extends RecyclerView.Adapter<GamesListAdapter.ViewHolder> {
    private List<Game> games;
    private Context context;

    private int bestIndex = -1;
    private int worstIndex = -1;

    private GamesListListener gamesListListener;

    public GamesListAdapter(Context context, List<Game> games, GamesListListener gamesListListener) {
        this.games = games;
        this.context = context;
        this.gamesListListener = gamesListListener;

        calcBestWorst();
    }

    private void calcBestWorst() {
        long bestTime = -1;
        long worstTime = -1;
        for (int i = 0; i < games.size(); i++) {
            if (games.get(i).isCompleted()) {
                long gTime = games.get(i).getTime();
                if (bestTime == -1 || gTime < bestTime) {
                    bestTime = gTime;
                    bestIndex = i;
                }
                if (worstTime == -1 || gTime > worstTime) {
                    worstTime = gTime;
                    worstIndex = i;
                }
            }
        }
    }

    public void notifyChanged() {
        calcBestWorst();
        notifyDataSetChanged();
    }

    @Override
    public GamesListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View rowItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.game_list_item, parent, false);
        return new ViewHolder(rowItem);
    }

    @Override
    public void onBindViewHolder(GamesListAdapter.ViewHolder holder, int position) {

        final Game game = games.get(position);

        holder.item_text.setText(game.getName());
        holder.item_time.setText(context.getString(R.string.current_time, game.getTimeString()));

        if (position == bestIndex) {
            holder.item_icon.setImageDrawable(context.getDrawable(R.drawable.ic_oui_crown_outline));
            holder.item_icon.setColorFilter(context.getColor(R.color.yellow));
        } else if (position == worstIndex) {
            holder.item_icon.setImageDrawable(context.getDrawable(R.drawable.ic_oui_crown_outline));
            holder.item_icon.setColorFilter(context.getColor(R.color.red));
        } else if (game.isCompleted()) {
            holder.item_icon.setImageDrawable(context.getDrawable(R.drawable.ic_oui_selected));
            holder.item_icon.setColorFilter(null);
        } else {
            holder.item_icon.setImageDrawable(context.getDrawable(R.drawable.ic_oui_time));
            holder.item_icon.setColorFilter(null);
        }

        holder.item_rename.setOnClickListener(v -> {

            EditText editText = new EditText(context);
            editText.setHint(R.string.name);

            FrameLayout container = new FrameLayout(context);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.leftMargin = 52;
            params.rightMargin = 52;
            editText.setLayoutParams(params);
            container.addView(editText);

            new AlertDialog.Builder(context)
                    .setTitle(game.getName())
                    .setView(container)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.rename, (dialog, which) -> {
                        CharSequence cSName = editText.getText();
                        String sName = cSName.length() != 0 ? cSName.toString() : game.getName();
                        game.setName(sName);
                        notifyItemChanged(position);
                        gamesListListener.onNameChange(game);
                    })
                    .show();
        });
        holder.item_delete.setOnClickListener(v -> {
            AlertDialog deleteDialog = new AlertDialog.Builder(context)
                    .setTitle(game.getName())
                    .setMessage(R.string.delete_sudoku)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.delete, (dialog, which) -> {
                        games.remove(position);
                        /*notifyItemRemoved(position);
                        notifyItemRangeChanged(position, games.size());*/
                        notifyChanged();
                        gamesListListener.onGameDeleted(game);
                    }).show();
            DialogUtils.setDialogButtonTextColor(deleteDialog, AlertDialog.BUTTON_POSITIVE, context.getColor(R.color.oui_functional_red_color));
        });

        holder.itemView.setOnClickListener(v -> ((SudokuActivity) context).loadGame(games.get(holder.getAdapterPosition())));
    }

    @Override
    public int getItemCount() {
        return this.games.size();
    }

    public interface GamesListListener {
        void onNameChange(Game game);

        void onGameDeleted(Game game);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView item_icon;
        private AppCompatImageButton item_rename, item_delete;
        private TextView item_text, item_time;

        public ViewHolder(View view) {
            super(view);
            item_text = view.findViewById(R.id.item_text);
            item_time = view.findViewById(R.id.item_time);
            item_icon = view.findViewById(R.id.item_icon);
            item_rename = view.findViewById(R.id.item_rename);
            item_delete = view.findViewById(R.id.item_delete);
        }
    }
}
