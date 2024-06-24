package com.ost.application.sudoku.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class PropertiesListView extends LinearLayout {

    public PropertiesListView(Context context) {
        this(context, null);
    }

    public PropertiesListView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
    }

    public void addLine(String label, String content) {

        LinearLayout lineLayout = new LinearLayout(getContext());
        lineLayout.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        lineLayout.setOrientation(HORIZONTAL);
        lineLayout.setPadding(0, 5, 0, 5);

        TextView labelText = new TextView(getContext());
        labelText.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        labelText.setText(label);
        labelText.setGravity(Gravity.START);
        labelText.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        lineLayout.addView(labelText);

        TextView contentText = new TextView(getContext());
        contentText.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        contentText.setText(content);
        contentText.setGravity(Gravity.END);
        lineLayout.addView(contentText);

        addView(lineLayout);
    }

    public void clearList() {
        removeAllViews();
    }

}
