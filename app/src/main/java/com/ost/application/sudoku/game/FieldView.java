package com.ost.application.sudoku.game;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;

import com.google.android.material.textview.MaterialTextView;

import java.util.stream.Collectors;

import com.ost.application.R;
import com.ost.application.sudoku.adapter.SudokuViewAdapter;
import com.ost.application.sudoku.utils.PropertiesListView;

public class FieldView extends LinearLayout {

    private Context context;
    private SharedPreferences sharedPreferences;
    private SudokuViewAdapter adapter;

    private Game game;
    private Field[][] fields;
    private Field field;
    private int row, column, size, sqrt_size, position;

    private TextView fieldViewValue, fieldViewNotes;
    private View fieldViewContainer;
    private boolean isColored = false;

    public FieldView(Context context, SharedPreferences sharedPreferences) {
        super(context);
        this.context = context;
        this.sharedPreferences = sharedPreferences;

        AsyncLayoutInflater asyncLayoutInflater = new AsyncLayoutInflater(context);
        asyncLayoutInflater.inflate(R.layout.field_view, this, (view, resid, parent) -> {
            if (adapter.isAnimateLayoutChanges_FieldView())
                parent.setLayoutTransition(new LayoutTransition());
            parent.addView(view);
            fieldViewValue = findViewById(R.id.itemNumber);
            fieldViewNotes = findViewById(R.id.itemNotes);
            fieldViewContainer = findViewById(R.id.itemContainer);
            if (game != null) init(game, position, adapter);
        });
    }

    private void setValue(Integer value) {
        if (value == field.getValue()) return;
        if (!field.isHint() && !game.isEditMode()) game.addFieldToHistory(field, position);

        field.setValue(value);
        field.setError(false);

        fieldViewValue.setText(value == null ? null : String.valueOf(value));
        fieldViewValue.setVisibility(value == null ? GONE : VISIBLE);
        setBackground(false);
        if (value != null && !game.isEditMode()) checkForCompletion();
    }

    private void setHint() {
        field.setHint();
        setEnabled(false);
        fieldViewValue.setTextColor(sharedPreferences.getInt("hint_color", context.getColor(R.color.blue)));

        setValue(field.getSolution());
    }

    private void updateNotes() {
        fieldViewNotes.setVisibility(field.getNotes().size() == 0 ? GONE : VISIBLE);
        fieldViewNotes.setText(field.getNotes().stream().map(Object::toString).collect(Collectors.joining()));

        if (row == size - 1 && (column == 0 || column == size - 1))
            fieldViewNotes.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
    }

    private void checkForCompletion() {
        boolean error = false;
        Boolean[][] correct = new Boolean[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (fields[i][j].getValue() == null || fields[i][j].isError())
                    return; //not completed

                if (fields[i][j].getValue().equals(fields[i][j].getSolution())) {
                    correct[i][j] = true;
                } else {
                    correct[i][j] = false;
                    error = true;
                }
            }
        }

        if (error) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.completed_error_title)
                    .setMessage(R.string.completed_error_message)
                    .setPositiveButton(R.string.show_errors, (dialog, which) -> {
                        for (int i = 0; i < size; i++) {
                            for (int j = 0; j < size; j++) {
                                adapter.getFieldView(i * size + j).setBackground(!correct[i][j]);
                                fields[i][j].setError(!correct[i][j]);
                            }
                        }
                    })
                    .setNegativeButton(R.string.dismiss, null)
                    .show();
        } else {
            game.setCompleted();
            PropertiesListView propertiesListView = new PropertiesListView(context);
            propertiesListView.addLine(context.getString(R.string.name), game.getName());
            propertiesListView.addLine(context.getString(R.string.time), game.getTimeString());
            propertiesListView.addLine(context.getString(R.string.size), game.getSize() + "×" + game.getSize());
            propertiesListView.addLine(context.getString(R.string.difficulty), String.valueOf(game.getDifficulty()));
            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 26, getResources().getDisplayMetrics());
            propertiesListView.setPadding(padding, 0, padding, 0);
            new AlertDialog.Builder(context)
                    .setTitle(R.string.completed_no_error_title)
                    .setView(propertiesListView)
                    .setPositiveButton(R.string.dismiss, null)
                    .show();
        }
    }

    public void setBackground(boolean error) {
        if (error)
            setBackgroundColor(sharedPreferences.getInt("error_color", context.getColor(R.color.sesl_error_color)));
        else if (isColored)
            setBackgroundColor(getResources().getColor(R.color.control_color_normal, context.getTheme()));
        else setBackgroundColor(Color.TRANSPARENT);
    }

    public void init(Game game, int position, SudokuViewAdapter adapter) {

        //init vars
        this.adapter = adapter;
        this.position = position;
        this.game = game;

        if (fieldViewContainer == null) return;

        this.fields = game.getFields();
        this.size = game.getSize();
        this.row = position / size;
        this.column = position % size;
        this.field = fields[row][column];
        this.sqrt_size = (int) Math.sqrt(size);

        //init view
        fieldViewValue.setText(field.getValue() == null ? null : String.valueOf(field.getValue()));

        if (!field.isPreNumber() && !field.isHint() && !game.isCompleted()) {
            setOnClickListener(v -> showMainPopup());

            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            setOnLongClickListener(v -> {
                if (field.getValue() != null) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                    setValue(null);
                    return true;
                }
                return false;
            });
        }

        updateNotes();

        //init appearance
        if (field.isPreNumber() != sharedPreferences.getBoolean("invert_colors", false)) {
            TypedValue typedValue = new TypedValue();
            getContext().getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
            fieldViewValue.setTextColor(typedValue.data);
        }
        if (field.isHint()) {
            fieldViewValue.setTextColor(sharedPreferences.getInt("hint_color", context.getColor(R.color.blue)));
        }

        if (sharedPreferences.getBoolean("grid_box_colors", true)) {
            int rm = row % (sqrt_size * 2);
            int cm = column % (sqrt_size * 2);
            isColored = (rm >= sqrt_size && rm < sqrt_size * 2) != (cm >= sqrt_size && cm < sqrt_size * 2);
        }
        setBackground(field.isError());
    }

    private void showMainPopup() {
        View popupView = ((LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.popup_number_input, null);
        final PopupWindow popupWindow = new PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setAnimationStyle(R.style.MenuPopupAnimStyle);

        setSelected(true);
        popupWindow.setOnDismissListener(() -> setSelected(false));

        //buttons
        AppCompatImageButton popup_notes = popupView.findViewById(R.id.popup_notes);
        popup_notes.setTooltipText(context.getString(R.string.notes));
        popup_notes.setVisibility(game.isEditMode() ? GONE : VISIBLE);
        popup_notes.setOnClickListener(v -> {
            popupWindow.dismiss();
            showNotesPopup();
        });

        AppCompatImageButton popup_remove = popupView.findViewById(R.id.popup_remove);
        popup_remove.setTooltipText(context.getString(R.string.remove));
        popup_remove.setVisibility(field.getValue() == null ? GONE : VISIBLE);
        popup_remove.setOnClickListener(v -> {
            popupWindow.dismiss();
            setValue(null);
        });

        AppCompatImageButton popup_hint = popupView.findViewById(R.id.popup_hint);
        popup_hint.setTooltipText(context.getString(R.string.hint));
        popup_hint.setVisibility(game.isEditMode() ? GONE : VISIBLE);
        popup_hint.setOnClickListener(v -> {
            popupWindow.dismiss();
            setHint();
        });

        //Number grid
        GridLayout popupGrid = popupView.findViewById(R.id.popup_numbers);
        popupGrid.setColumnCount(sqrt_size);
        popupGrid.setRowCount(sqrt_size);
        for (int i = 1; i <= size; i++) {
            MaterialTextView materialTextView = new MaterialTextView(context);
            materialTextView.setPadding(28, 4, 28, 4);

            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
            materialTextView.setBackgroundResource(outValue.resourceId);

            materialTextView.setTextSize(24);
            materialTextView.setTypeface(Typeface.DEFAULT_BOLD);

            materialTextView.setText(String.valueOf(i));
            materialTextView.setOnClickListener(v -> {
                popupWindow.dismiss();
                setValue(Integer.valueOf(((MaterialTextView) v).getText().toString()));
            });

            popupGrid.addView(materialTextView);
        }

        //show at position
        Rect rect = new Rect();
        fieldViewContainer.getGlobalVisibleRect(rect);
        popupView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        popupWindow.showAtLocation(fieldViewContainer,
                Gravity.TOP | Gravity.START,
                rect.left - popupView.getMeasuredWidth() / 2 + rect.width() / 2,
                rect.bottom - popupView.getMeasuredHeight() / 2 - rect.height() / 2);

        //dim screen
        View container = (View) popupWindow.getContentView().getParent();
        WindowManager.LayoutParams wmlp = (WindowManager.LayoutParams) container.getLayoutParams();
        wmlp.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        wmlp.dimAmount = 0.3f;
        ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).updateViewLayout(container, wmlp);
    }

    private void showNotesPopup() {
        View popupView = ((LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.popup_notes_input, null);
        final PopupWindow popupWindow = new PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setAnimationStyle(R.style.MenuPopupAnimStyle);

        setSelected(true);
        popupWindow.setOnDismissListener(() -> setSelected(false));

        //content
        GridLayout popupGrid = popupView.findViewById(R.id.popup_notes_grid);
        popupGrid.setColumnCount(sqrt_size);
        popupGrid.setRowCount(sqrt_size);
        for (int i = 1; i <= size; i++) {
            MaterialTextView materialTextView = new MaterialTextView(context);
            materialTextView.setPadding(28, 4, 28, 4);
            materialTextView.setTextSize(24);
            materialTextView.setTextColor(getResources().getColorStateList(R.color.dialog_notes_selector, context.getTheme()));
            materialTextView.setTypeface(Typeface.DEFAULT_BOLD);
            materialTextView.setText(String.valueOf(i));
            materialTextView.setSelected(field.getNotes().contains(i));

            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
            materialTextView.setBackgroundResource(outValue.resourceId);

            int finalI = i;
            materialTextView.setOnClickListener(v -> {
                game.addFieldToHistory(field, position);

                materialTextView.setSelected(!materialTextView.isSelected());
                if (materialTextView.isSelected()) field.addNote(finalI);
                else field.removeNote(finalI);
                updateNotes();
            });

            popupGrid.addView(materialTextView);
        }

        //calculate position
        Rect rect = new Rect();
        fieldViewContainer.getGlobalVisibleRect(rect);
        popupView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        final int[] mPosX = {rect.left - popupView.getMeasuredWidth() / 2 + rect.width() / 2};
        final int[] mPosY = {rect.bottom - popupView.getMeasuredHeight() / 2 - rect.height() / 2};

        //dragging
        popupView.findViewById(R.id.drag_icon).setOnTouchListener(new OnTouchListener() {
            private int dx = 0;
            private int dy = 0;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dx = (int) (mPosX[0] - motionEvent.getRawX());
                        dy = (int) (mPosY[0] - motionEvent.getRawY());
                        break;
                    case MotionEvent.ACTION_MOVE:
                        mPosX[0] = (int) (motionEvent.getRawX() + dx);
                        mPosY[0] = (int) (motionEvent.getRawY() + dy);
                        popupWindow.update(mPosX[0], mPosY[0], -1, -1);
                        break;
                }
                return true;
            }
        });

        //show at position
        popupWindow.showAtLocation(fieldViewContainer, Gravity.TOP | Gravity.START, mPosX[0], mPosY[0]);
    }


    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
