package com.ost.application.ui.activity.launcher

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ost.application.MainActivity
import com.ost.application.ui.activity.welcome.WelcomeActivity
import com.ost.application.util.AppPrefs

@SuppressLint("CustomSplashScreen")
class LauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AppPrefs.isSetupComplete(this)) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, WelcomeActivity::class.java))
        }
        finish()
    }
}