package com.ost.application.sudoku.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Field {

    private Integer value, solution;
    private List<Integer> notes = new ArrayList<>();
    private Boolean preNumber, hint, error;

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public boolean isPreNumber() {
        return preNumber != null && preNumber;
    }

    public Integer getSolution() {
        return solution;
    }

    //error

    void setSolution(Integer solution) {
        this.solution = solution;
    }

    public boolean isError() {
        return error != null && error;
    }

    public void setError(Boolean error) {
        this.error = !error ? null : true;
    }

    //hint
    public void setHint() {
        this.hint = true;
    }

    public boolean isHint() {
        return hint != null && hint;
    }

    //notes
    public void addNote(Integer note) {
        if (!notes.contains(note)) {
            notes.add(note);
            notes.sort(Comparator.naturalOrder());
        }
    }

    public void removeNote(Integer note) {
        notes.remove(note);
    }

    public List<Integer> getNotes() {
        return notes;
    }

    //package only
    void setPreNumber() {
        this.preNumber = true;
    }

    Field duplicate() {
        Field clone = new Field();
        clone.value = this.value;
        clone.solution = this.solution;
        clone.notes = new ArrayList<>(this.notes);
        clone.preNumber = this.preNumber;
        clone.hint = this.hint;
        clone.error = this.error;
        return clone;
    }

    Field duplicateInitial() {
        Field clone = new Field();
        clone.solution = this.solution;
        clone.preNumber = this.preNumber;
        if (isPreNumber()) clone.value = this.value;
        return clone;
    }

}
