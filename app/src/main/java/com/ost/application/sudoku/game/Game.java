package com.ost.application.sudoku.game;

import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import com.ost.application.sudoku.adapter.SudokuViewAdapter;

public class Game {

    private transient GameListener gameListener;
    private Integer size, difficulty;
    private String name;
    private long time; //1 = 10th of a second
    private Boolean completed;
    private Field[][] fields;
    private ArrayList<HistoryItem> history = new ArrayList<>();
    private transient Integer tries;
    private transient boolean edit_mode = false;
    private transient Timer timer;
    private transient MenuItem undoMenuItem; // Добавлено поле для пункта меню "Отменить"

    //create new game
    public Game(int size, int difficulty) {
        this.name = "Sudoku";
        this.size = size;
        this.difficulty = difficulty;
        this.tries = 20;
        this.time = 0;
        this.completed = false;
        this.fields = generateFields();

        this.difficulty = difficulty;
    }

    //new empty game (make own)
    public Game(int size) {
        this.name = "Sudoku";
        this.size = size;
        this.difficulty = -1;
        this.time = 0;
        this.completed = false;
        this.edit_mode = true;

        this.fields = new Field[size][size];
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++) this.fields[i][j] = new Field();
    }

    //new empty game (initial & copy)
    private Game() {
    }

    public void setGameListener(GameListener gameListener) {
        this.gameListener = gameListener;
    }

    public Game copy() {
        Game clone = new Game();
        clone.name = this.name;
        clone.size = this.size;
        clone.difficulty = this.difficulty;
        clone.time = this.time;
        clone.completed = this.completed;
        clone.fields = cloneFieldArray(this.fields);
        clone.history.addAll(this.history);
        return clone;
    }

    public Game getInitialGame() {
        Game initial = new Game();
        initial.name = this.name;
        initial.size = this.size;
        initial.difficulty = this.difficulty;
        initial.time = 0;
        initial.completed = false;
        initial.fields = getPreFieldsOnly(this.fields, this.size);
        return initial;
    }

    public Game getSolutionGame() {
        Game solution = new Game();
        solution.size = this.size;
        solution.completed = true;
        solution.fields = cloneFieldArray(this.fields);
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                solution.fields[i][j].setValue(solution.fields[i][j].getSolution());
        return solution;
    }

    private Field[][] getPreFieldsOnly(Field[][] fields, int size) {
        Field[][] preFields = new Field[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                preFields[i][j] = fields[i][j].duplicateInitial();
            }
        }
        return preFields;
    }

    public Field[][] getFields() {
        return fields;
    }

    public Integer getSize() {
        return size;
    }

    public Integer getDifficulty() {
        return difficulty;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCompleted() {
        return completed != null && completed;
    }

    public boolean isEditMode() {
        return edit_mode;
    }

    public void setCompleted() {
        this.completed = true;
        stopTimer();
        if (this.gameListener != null) this.gameListener.onCompleted();
    }

    public void addFieldToHistory(Field field, int position) {
        history.add(new HistoryItem(field.duplicate(), position));
        // Обновление пункта меню "Отменить"
        updateUndoMenuItem();
    }

    public boolean hasHistory() {
        return history.size() != 0;
    }

    public void revertLastChange(SudokuViewAdapter adapter) {
        if (hasHistory()) {
            HistoryItem item = history.get(history.size() - 1);
            if (!fields[item.getPosition() / size][item.getPosition() % size].isHint())
                fields[item.getPosition() / size][item.getPosition() % size] = item.getField();
            adapter.updateFieldView(item.getPosition());
            history.remove(history.size() - 1);
            // Обновление пункта меню "Отменить"
            updateUndoMenuItem();
        }
    }

    //timer
    public void startTimer(long delay) {
        if (completed || edit_mode) return;
        if (timer != null) timer.cancel();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                time++;
                if (gameListener != null) gameListener.onTimeChanged(getTimeString());
            }
        }, delay, 100);
    }

    public void stopTimer() {
        if (timer == null) return;
        timer.cancel();
        timer = null;
    }

    public boolean isTimerRunning() {
        return timer != null;
    }

    public String getTimeString() {
        long timeSec = time / 10;
        if (timeSec >= 3600) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", timeSec / 3600, (timeSec / 60) % 60, timeSec % 60);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", timeSec / 60, timeSec % 60);
        }
    }

    public long getTime() {
        return time;
    }

    //solver
    public Object makeGameFromEdit() {
        Object newGame = makeSolutionFromEdit(false);
        if (newGame instanceof Integer) return newGame;

        ((Game) newGame).completed = false;
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++) {
                ((Game) newGame).fields[i][j].setSolution(((Game) newGame).fields[i][j].getValue());
                if (!((Game) newGame).fields[i][j].isPreNumber())
                    ((Game) newGame).fields[i][j].setValue(null);
            }
        return newGame;
    }

    public Object makeSolutionFromEdit(boolean ignoreMultiple) {
        ArrayList<Field[][]> solutions = new ArrayList<>();
        solveFieldForSolutionGame(cloneFieldArray(this.fields), 0, 0, solutions);

        if (solutions.size() == 1 || ignoreMultiple) {
            Game solutionGame = this.copy();

            solutionGame.edit_mode = false;
            solutionGame.completed = true;
            solutionGame.difficulty = size * size;

            for (int i = 0; i < size; i++)
                for (int j = 0; j < size; j++)
                    if (solutionGame.fields[i][j].getValue() != null) {
                        solutions.get(0)[i][j].setPreNumber();
                        solutionGame.difficulty--;
                    }

            solutionGame.fields = solutions.get(0);
            return solutionGame;
        } else {
            return solutions.size();
        }

    }

    //new Game
    private Field[][] generateFields() {
        Field[][] output = new Field[size][size];
        for (int i = 0; i < size; i++) for (int j = 0; j < size; j++) output[i][j] = new Field();

        //add random numbers
        for (int i = 0; i < (Math.pow(size, 2)) / 3; i++) addRandomNumberTo(output);

        //generate solutions and retry if there aren't
        ArrayList<Field[][]> solutions = new ArrayList<>();
        solveFieldForSolutionGame(cloneFieldArray(output), 0, 0, solutions);
        if (solutions.isEmpty()) return generateFields();

        //remove random numbers while there is one solution
        Field[][] solution = solutions.get(new Random().nextInt(solutions.size()));
        output = removedRandomNumber(solution);

        //append solution and set preNumber
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                output[i][j].setSolution(solution[i][j].getValue());
                if (output[i][j].getValue() != null) output[i][j].setPreNumber();
            }
        }

        return output;
    }

    private void addRandomNumberTo(Field[][] input) {
        Random random = new Random();
        int row = random.nextInt(size);
        int column = random.nextInt(size);
        int number = random.nextInt(size) + 1;
        if (input[row][column].getValue() == null && isValid(input, number, row, column)) {
            input[row][column].setValue(number);
        } else {
            addRandomNumberTo(input);
        }
    }

    private boolean isValid(Field[][] input, Integer i, int row, int column) {
        int size = (int) Math.sqrt(this.size);

        for (Field field : input[row]) if (i.equals(field.getValue())) return false;
        for (Field[] rows : input) if (i.equals(rows[column].getValue())) return false;

        int sRow = (row / size) * size;
        int sColumn = (column / size) * size;
        for (int r = sRow; r < sRow + size; r++)
            for (int c = sColumn; c < sColumn + size; c++)
                if (i.equals(input[r][c].getValue())) return false;

        return true;
    }

    private Field[][] cloneFieldArray(Field[][] input) {
        Field[][] output = new Field[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                output[i][j] = input[i][j].duplicate();
            }
        }
        return output;
    }

    private void solveFieldForSolutionGame(Field[][] input, int row, int column, ArrayList<Field[][]> solutions) {
        if (edit_mode && solutions.size() > 1) return;

        if (column == size) { //go to next row
            column = 0;
            row++;
        }

        if (row == size) { //save the solution once reached the end
            solutions.add(cloneFieldArray(input));
            return;
        }

        if (input[row][column].getValue() == null) {
            for (int n = 1; n <= size; n++) {
                if (isValid(input, n, row, column)) {
                    input[row][column].setValue(n);
                    solveFieldForSolutionGame(input, row, column + 1, solutions);
                }
            }
            input[row][column].setValue(null);
        } else solveFieldForSolutionGame(input, row, column + 1, solutions);
    }

    private Field[][] removedRandomNumber(Field[][] input) {
        Field[][] tmp_fields = cloneFieldArray(input);
        Random random = new Random();
        int row = random.nextInt(size);
        int column = random.nextInt(size);

        if (tmp_fields[row][column].getValue() != null) {
            tmp_fields[row][column].setValue(null);
            ArrayList<Field[][]> solutions = new ArrayList<>();
            solveFieldForSolutionGame(tmp_fields, 0, 0, solutions);
            if (solutions.size() == 1) { //continue while solutions.size() is one
                difficulty--;
                if (difficulty == 0) return tmp_fields;
                return removedRandomNumber(tmp_fields);
            } else { //try to remove more numbers
                tries--;
                if (tries > 0) return removedRandomNumber(input);
                return input;
            }
        } else { //skip already removed numbers
            return removedRandomNumber(input);
        }
    }

    public interface GameListener {
        void onHistoryChange(int length);

        void onCompleted();

        void onTimeChanged(String time);
    }

    public void setUndoMenuItem(MenuItem undoMenuItem) {
        this.undoMenuItem = undoMenuItem;
    }

    private void updateUndoMenuItem() {
        if (undoMenuItem != null) {
            undoMenuItem.setEnabled(hasHistory());
        }
    }
}