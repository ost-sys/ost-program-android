package com.ost.application.sudoku.game;

public class HistoryItem {
    private Field field;
    private int position;

    public HistoryItem(Field field, int position) {
        super();
        this.field = field;
        this.position = position;
    }

    public Field getField() {
        return field;
    }

    public int getPosition() {
        return position;
    }
}
