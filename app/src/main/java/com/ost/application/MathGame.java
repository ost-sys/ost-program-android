package com.ost.application;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Random;

import dev.oneuiproject.oneui.layout.ToolbarLayout;

public class MathGame extends AppCompatActivity {

    com.ost.application.ui.widget.CardView taskTextView;
    EditText enterEditText;
    Button resultButton;
    Button clearButton;
    TextInputLayout outlinedTextField;
    ToolbarLayout topAppBar;
    com.ost.application.ui.widget.CardView resultTrueTextView;
    com.ost.application.ui.widget.CardView resultFalseTextView;
    String [] operationArray = {"*", "/", "-", "+"};
    Random random = new Random();
    int first;
    int second;
    int answer = 0;
    int trueAnswerCount = 0;
    int falseAnswerCount = 0;

    @SuppressLint({"SetTextI18n", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_math_game);

        topAppBar = findViewById(R.id.topAppBar);
        taskTextView = findViewById(R.id.taskTextView);
        enterEditText = findViewById(R.id.enterEditText);
        resultButton = findViewById(R.id.resultButton);
        clearButton = findViewById(R.id.clearButton);
        resultTrueTextView = findViewById(R.id.resultTrueCardView);
        resultFalseTextView = findViewById(R.id.resultFalseCardView);
        setNewTask();

        topAppBar.setNavigationButtonOnClickListener(n -> onBackPressed());

        resultButton.setOnClickListener(v -> {
            try {
                int b = Integer.parseInt(String.valueOf(enterEditText.getText()));
                if (b == answer) {
                    Snackbar.make(taskTextView, R.string.trueAnswer, Snackbar.LENGTH_SHORT).show();
                    setNewTask();
                    trueAnswerCount += 1;
                    resultTrueTextView.setSummaryText(Integer.toString(trueAnswerCount));
                    enterEditText.setText("");

                } else {
                    Snackbar.make(taskTextView, R.string.falseAnswer, Snackbar.LENGTH_SHORT).setAction(R.string.clearButton, v1 -> {
                        enterEditText.setText("");
                        Snackbar.make(taskTextView, R.string.cleanText, Snackbar.LENGTH_SHORT).show();
                    }).show();
                    falseAnswerCount += 1;
                    resultFalseTextView.setSummaryText(Integer.toString(falseAnswerCount));

                }
            } catch (NumberFormatException exception) {
                System.out.println("TextInputEditText error detected");
                Snackbar.make(taskTextView, R.string.nullAnswer, Snackbar.LENGTH_SHORT).show();
            }
        });

        clearButton.setOnClickListener(v -> {
            enterEditText.setText("");
            Snackbar.make(taskTextView, R.string.cleanText, Snackbar.LENGTH_SHORT).show();
        });

    }

    @SuppressLint("SetTextI18n")
    void setNewTask() {
        first = random.nextInt(1000);
        second = random.nextInt(1000);
        int operation = random.nextInt(4);
        switch (operationArray[operation]) {
            case "+":
                answer = first + second;
                taskTextView.setTitleText(first + " + " + second);
                break;
            case "-":
                answer = first - second;
                taskTextView.setTitleText(first + " - " + second);
                break;
            case "*":
                answer = first * second;
                taskTextView.setTitleText(first + " * " + second);
                break;
            case "/":
                answer = first / second;
                taskTextView.setTitleText(first + " / " + second);
                break;
        }
    }
    public void onResume() {
        super.onResume();
    }
}