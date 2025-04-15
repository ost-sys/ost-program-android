package com.ost.application.ui.screen.display.test

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.util.Log
import android.view.View
import android.view.View.OnLongClickListener
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.ost.application.MainActivity
import com.ost.application.R
import java.util.Random

class BurnInRecoveryActivity : ComponentActivity() {
    private val random = Random()
    private var isRunning = true
    private var handler: Handler? = null
    private var noiseView: NoiseView? = null

    private var currentMode = 0
    private var originalBrightness = -1
    private var originalBrightnessMode = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        noiseView = NoiseView(this)
        setContentView(noiseView)

        if (!Settings.System.canWrite(this)) {
            Toast.makeText(this, getString(R.string.brightness_permission_m), Toast.LENGTH_LONG)
                .show()
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = "package:$packageName".toUri()
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            startActivity(intent)
            finish()
            return
        } else {
            saveCurrentBrightnessSettings()
            setMaxBrightness()
        }

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (sharedPreferences == null) {
            Log.e("BurnInRecovery", "SharedPreferences is null")
        } else {
            val totalDuration = sharedPreferences.getInt("total_duration", 30)
            val noiseDuration = sharedPreferences.getInt("noise_duration", 1)
            val horizontalLinesDuration = sharedPreferences.getInt("horizontal_lines_duration", 1)
            val verticalLinesDuration = sharedPreferences.getInt("vertical_lines_duration", 1)
            val blackWhiteNoiseDuration = sharedPreferences.getInt("black_white_noise_duration", 1)

            handler = Handler(Looper.getMainLooper())
            startModeCycle(
                totalDuration,
                noiseDuration,
                horizontalLinesDuration,
                verticalLinesDuration,
                blackWhiteNoiseDuration
            )
        }

        noiseView!!.setOnLongClickListener(OnLongClickListener { v: View? ->
            isRunning = false
            Toast.makeText(this, getString(R.string.exiting), Toast.LENGTH_SHORT).show()
            finish()
            true
        })

        noiseView!!.setOnClickListener(View.OnClickListener { v: View? ->
            Toast.makeText(
                this,
                R.string.hold_to_exit,
                Toast.LENGTH_SHORT
            ).show()
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        if (handler != null) {
            handler!!.removeCallbacksAndMessages(null)
        }
        restoreOriginalBrightnessSettings()
    }

    override fun onStop() {
        super.onStop()
        restoreOriginalBrightnessSettings()
    }

    private fun saveCurrentBrightnessSettings() {
        try {
            originalBrightness = Settings.System.getInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
            originalBrightnessMode = Settings.System.getInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE
            )
        } catch (e: SettingNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun setMaxBrightness() {
        try {
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                255
            )
            Settings.System.putInt(
                contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.brightness_fail), Toast.LENGTH_SHORT).show()
        }
    }

    private fun restoreOriginalBrightnessSettings() {
        try {
            if (originalBrightness != -1) {
                Settings.System.putInt(
                    contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    originalBrightness
                )
            }

            if (originalBrightnessMode != -1) {
                Settings.System.putInt(
                    contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    originalBrightnessMode
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_WRITE_SETTINGS) {
            if (Settings.System.canWrite(this)) {
                saveCurrentBrightnessSettings()
                setMaxBrightness()
            } else {
                Toast.makeText(this, getString(R.string.permission_not_granted), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun startModeCycle(
        totalDuration: Int,
        noiseDuration: Int,
        horizontalLinesDuration: Int,
        verticalLinesDuration: Int,
        blackWhiteNoiseDuration: Int
    ) {
        val modeDurations = intArrayOf(
            noiseDuration * 60 * 1000,
            horizontalLinesDuration * 60 * 1000,
            verticalLinesDuration * 60 * 1000,
            blackWhiteNoiseDuration * 60 * 1000
        )

        val modeSwitcher: Runnable = object : Runnable {
            var startTime: Long = System.currentTimeMillis()
            var modeIndex: Int = 0

            override fun run() {
                if (!isRunning) return

                val elapsedTime = System.currentTimeMillis() - startTime
                if (elapsedTime >= totalDuration * 60 * 1000) {
                    isRunning = false
                    Toast.makeText(
                        this@BurnInRecoveryActivity,
                        getString(R.string.exiting),
                        Toast.LENGTH_SHORT
                    ).show()
                    showCompletionNotification()
                    finish()
                    return
                }

                currentMode = modeIndex
                noiseView!!.setMode(currentMode)

                modeIndex = (modeIndex + 1) % modeDurations.size

                Log.d(
                    "BurnInRecovery",
                    "Switching to mode " + modeIndex + ", Duration: " + modeDurations[modeIndex]
                )

                handler!!.postDelayed(this, modeDurations[modeIndex].toLong())
            }
        }

        handler!!.post(modeSwitcher)
    }

    private class NoiseView(context: Context?) : View(context) {
        private val paint = Paint()
        private val random = Random()
        private var bitmap: Bitmap? = null

        private var mode: Int = MODE_NOISE

        fun setMode(mode: Int) {
            this.mode = mode
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val width = getWidth()
            val height = getHeight()

            if (bitmap == null || bitmap!!.getWidth() != width || bitmap!!.getHeight() != height) {
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            }

            @SuppressLint("DrawAllocation") val pixels = IntArray(width * height)

            when (mode) {
                MODE_NOISE -> {
                    var i = 0
                    while (i < pixels.size) {
                        pixels[i] =
                            Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
                        i++
                    }
                }

                MODE_HORIZONTAL_LINES -> {
                    var y = 0
                    while (y < height) {
                        val color =
                            Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
                        var x = 0
                        while (x < width) {
                            pixels[y * width + x] = color
                            x++
                        }
                        y++
                    }
                }

                MODE_VERTICAL_LINES -> {
                    var x = 0
                    while (x < width) {
                        val color =
                            Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
                        var y = 0
                        while (y < height) {
                            pixels[y * width + x] = color
                            y++
                        }
                        x++
                    }
                }

                MODE_BLACK_WHITE_NOISE -> {
                    var i = 0
                    while (i < pixels.size) {
                        val gray = random.nextInt(256)
                        pixels[i] = Color.rgb(gray, gray, gray)
                        i++
                    }
                }
            }

            bitmap!!.setPixels(pixels, 0, width, 0, 0, width, height)
            canvas.drawBitmap(bitmap!!, 0f, 0f, paint)

            invalidate()
        }
    }

    private fun showCompletionNotification() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = getString(R.string.success)
        val manager = getSystemService<NotificationManager?>(NotificationManager::class.java)
        if (manager != null) {
            manager.createNotificationChannel(channel)
        }

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val intent = Intent(this, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check_circle_24dp)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Fixing dead/burnt pixels is complete")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = NotificationManagerCompat.from(this)
        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        }
    }

    companion object {
        private const val CHANNEL_ID = "burn_in_recovery_channel"
        private const val NOTIFICATION_ID = 1
        private const val REQUEST_WRITE_SETTINGS = 100

        private const val MODE_NOISE = 0
        private const val MODE_HORIZONTAL_LINES = 1
        private const val MODE_VERTICAL_LINES = 2
        private const val MODE_BLACK_WHITE_NOISE = 3
    }
}

