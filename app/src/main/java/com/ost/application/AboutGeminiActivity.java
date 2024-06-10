package com.ost.application;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import dev.oneuiproject.oneui.layout.ToolbarLayout;

public class AboutGeminiActivity extends AppCompatActivity {

    ToolbarLayout toolbarLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_gemini);

        toolbarLayout = findViewById(R.id.toolbarLayout);
        toolbarLayout.setNavigationButtonOnClickListener(n -> onBackPressed());
    }
}