package com.ost.application;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuCompat;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import dev.oneuiproject.oneui.layout.ToolbarLayout;

public class GeminiActivity extends AppCompatActivity {

    EditText geminiEditText;
    Button geminiAskButton;
    TextView geminiAnswer;
    ToolbarLayout geminiToolbar;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gemini);

        geminiEditText = findViewById(R.id.gemini_request_field);
        geminiAskButton = findViewById(R.id.gemini_ask_button);
        geminiAnswer = findViewById(R.id.gemini_answer_field);

        geminiAskButton.setOnClickListener(n -> {
            geminiAnswer.setText("");
            geminiAnswer.setHint(R.string.gemini_typing);
            buttonAskGemini();
        });

        geminiToolbar = findViewById(R.id.toolbarLayout);
        geminiToolbar.setNavigationButtonOnClickListener(n -> onBackPressed());
    }

    public void buttonAskGemini(){
        GenerativeModel gm = new GenerativeModel("gemini-pro", "AIzaSyDXi_Xh1lt_4us-nHwzWghg3q65t93k-0I");
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        Content content = new Content.Builder()
                .addText(String.valueOf(geminiEditText.getText()))
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String resultText = result.getText();
                geminiAnswer.setText(resultText);
            }
            @SuppressLint("SetTextI18n")
            @Override
            public void onFailure(@NonNull Throwable t) {
                geminiAnswer.setText("Sorry, but something going wrong. Error code: /n" + t);
            }
        }, this.getMainExecutor());
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_gemini, menu);
        MenuCompat.setGroupDividerEnabled(menu, true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.about_gemini) {
            startActivity(new Intent(this, AboutGeminiActivity.class));
            return true;
        }
        return false;
    }
}