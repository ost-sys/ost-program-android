package com.ost.application.ui.screen.stargazers.profile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.IntentCompat
import com.ost.application.data.model.Stargazer
import com.ost.application.ui.theme.OSTToolsTheme

class ProfileActivity : ComponentActivity() {

    companion object {
        const val KEY_STARGAZER = "key_stargazer"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val stargazer = IntentCompat.getParcelableExtra(intent, KEY_STARGAZER, Stargazer::class.java)

        if (stargazer == null) {
            finish()
            return
        }

        setContent {
            OSTToolsTheme {
                ProfileScreenWithSharedZoom(
                    stargazer = stargazer,
                    onFinish = { finish() }
                )
            }
        }
    }
}