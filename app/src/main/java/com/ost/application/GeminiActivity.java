package com.ost.application;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuCompat;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Locale;

import dev.oneuiproject.oneui.layout.ToolbarLayout;

public class GeminiActivity extends AppCompatActivity {
    private static final String FIRST_RUN_KEY = "first_run";
    private static final int REQUEST_CODE_SPEECH_INPUT = 100;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private ImageView voiceInputButton;
    private EditText geminiEditText;
    private Button geminiAskButton;
    private TextView geminiAnswer;
    private ToolbarLayout geminiToolbar;

    private GenerativeModel generativeModel;
    private GenerativeModelFutures generativeModelFutures;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gemini);

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean firstRun = prefs.getBoolean(FIRST_RUN_KEY, true);

        voiceInputButton = findViewById(R.id.gemini_ask_button_voice);

        generativeModel = new GenerativeModel("gemini-pro", "AIzaSyDXi_Xh1lt_4us-nHwzWghg3q65t93k-0I");
        generativeModelFutures = GenerativeModelFutures.from(generativeModel);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        REQUEST_RECORD_AUDIO_PERMISSION);
            } else {
                setupVoiceInputButton();
            }
        } else {
            setupVoiceInputButton();
        }

        if (firstRun) {
            showFirstRunDialog(prefs);
        }

        geminiEditText = findViewById(R.id.gemini_request_field);
        geminiAskButton = findViewById(R.id.gemini_ask_button);
        geminiAnswer = findViewById(R.id.gemini_answer_field);

        geminiAskButton.setOnClickListener(v -> {
            geminiAnswer.setText("");
            geminiAnswer.setHint(R.string.gemini_typing);
            buttonAskGemini();
        });

        Intent intent = getIntent();
        String textToInsert = intent.getStringExtra("textToInsert");
        if (textToInsert != null) {
            geminiEditText.setText(textToInsert);
        }

        geminiToolbar = findViewById(R.id.toolbarLayout);
        geminiToolbar.setNavigationButtonOnClickListener(v -> onBackPressed());
    }

    private void showFirstRunDialog(SharedPreferences prefs) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.attention))
                .setMessage(getString(R.string.gemini_attention))
                .setView(getLayoutInflater().inflate(R.layout.gemini_alertdialog, null))
                .setPositiveButton("ОК", (dialog, which) -> {
                    EditText input = ((AlertDialog) dialog).findViewById(R.id.input);
                    String dnsServer = input.getText().toString();

                    Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    startActivity(intent);

                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("DNS", dnsServer);
                    clipboard.setPrimaryClip(clip);

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(FIRST_RUN_KEY, false);
                    editor.apply();
                })
                .setNegativeButton(R.string.ignore, null);
        AlertDialog dialog = builder.create();
        dialog.show();

        EditText input = dialog.findViewById(R.id.input);
        input.setText("comss.dns.controld.com");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
    }

    private void setupVoiceInputButton() {
        voiceInputButton.setOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "");
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000);
            try {
                startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
            } catch (Exception e) {
                Toast.makeText(GeminiActivity.this, "Невозможно запустить распознавание речи", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                String spokenText = result.get(0);
                geminiEditText.setText(spokenText);

                geminiAnswer.setText("");
                geminiAnswer.setHint(R.string.gemini_typing);
                buttonAskGemini();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupVoiceInputButton();
            } else {
                voiceInputButton.setEnabled(false);
                Toast.makeText(this, "Необходимо разрешение на запись звука", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void buttonAskGemini(){
        String request = String.valueOf(geminiEditText.getText());
        if (!request.isEmpty()) {
            Content content = new Content.Builder()
                    .addText(request)
                    .build();

            ListenableFuture<GenerateContentResponse> response = generativeModelFutures.generateContent(content);
            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    String resultText = result.getText();
                    geminiAnswer.setText(resultText);
                }

                @Override
                public void onFailure(@NonNull Throwable t) {
                    geminiAnswer.setText("Sorry, but something went wrong. Error code: \n" + t);
                }
            }, this.getMainExecutor());
        }
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