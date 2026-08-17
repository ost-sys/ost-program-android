package com.ost.application.presentation.tools
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
import android.util.Log
import android.view.View
import android.view.View.OnLongClickListener
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.createBitmap
import androidx.lifecycle.lifecycleScope
import com.ost.application.MainActivity
import com.ost.application.R
import com.ost.application.core.settings.TimingSettings
import com.ost.application.settings.WearTimingSettingsRepository
import kotlinx.coroutines.launch
import java.util.Random
class BurnInRecoveryActivity : ComponentActivity() {
    private var isRunning = true
    private var handler: Handler? = null
    private var noiseView: NoiseView? = null
    private var currentMode = 0
    private var originalBrightness: Float = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    private lateinit var timingRepository: WearTimingSettingsRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        noiseView = NoiseView(this)
        setContentView(noiseView)
        setMaxBrightness()
        timingRepository = WearTimingSettingsRepository(applicationContext, lifecycleScope)
        handler = Handler(Looper.getMainLooper())
        lifecycleScope.launch {
            val timing: TimingSettings = timingRepository.settings.value
            startModeCycle(
                totalDuration = timing.totalDuration,
                noiseDuration = timing.noiseDuration,
                horizontalLinesDuration = timing.horizontalDuration,
                verticalLinesDuration = timing.verticalDuration,
                blackWhiteNoiseDuration = timing.blackWhiteNoiseDuration
            )
        }
        noiseView!!.setOnLongClickListener(OnLongClickListener { v: View? ->
            isRunning = false
            Toast.makeText(this, getString(R.string.exiting), Toast.LENGTH_SHORT).show()
            finish()
            true
        })
        noiseView!!.setOnClickListener(View.OnClickListener { v: View? ->
            Toast.makeText(this, R.string.hold_to_exit, Toast.LENGTH_SHORT).show()
        })
    }
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler?.removeCallbacksAndMessages(null)
        restoreOriginalBrightness()
    }
    override fun onStop() {
        super.onStop()
        restoreOriginalBrightness()
    }
    private fun setMaxBrightness() {
        val layoutParams = window.attributes
        originalBrightness = layoutParams.screenBrightness
        layoutParams.screenBrightness = 1.0f
        window.attributes = layoutParams
    }
    private fun restoreOriginalBrightness() {
        try {
            val layoutParams = window.attributes
            layoutParams.screenBrightness = originalBrightness
            window.attributes = layoutParams
        } catch (e: Exception) {
            e.printStackTrace()
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
                if (totalDuration <= 120 && elapsedTime >= totalDuration * 60 * 1000) {
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
                    "Switching to mode $modeIndex, Duration: ${modeDurations[modeIndex]}"
                )
                handler!!.postDelayed(this, modeDurations[modeIndex].toLong())
            }
        }
        handler!!.post(modeSwitcher)
    }
    private class NoiseView(context: Context?) : View(context) {
        private val paint = Paint()
        private val fastRandom = kotlin.random.Random.Default
        private var bitmap: Bitmap? = null
        private var pixels = IntArray(0)
        private var mode: Int = MODE_NOISE

        private var noiseBitmap: Bitmap? = null
        private var noisePixels = IntArray(512 * 512)

        fun setMode(mode: Int) {
            this.mode = mode
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val width = width
            val height = height

            if (width == 0 || height == 0) return

            when (mode) {
                MODE_NOISE -> {
                    if (noiseBitmap == null) {
                        noiseBitmap = createBitmap(512, 512, Bitmap.Config.RGB_565)
                    }
                    for (i in noisePixels.indices) {
                        noisePixels[i] = fastRandom.nextInt() or -0x1000000
                    }
                    noiseBitmap!!.setPixels(noisePixels, 0, 512, 0, 0, 512, 512)
                    paint.shader = android.graphics.BitmapShader(noiseBitmap!!, android.graphics.Shader.TileMode.REPEAT, android.graphics.Shader.TileMode.REPEAT)
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                    paint.shader = null
                }
                MODE_BLACK_WHITE_NOISE -> {
                    if (noiseBitmap == null) {
                        noiseBitmap = createBitmap(512, 512, Bitmap.Config.RGB_565)
                    }
                    for (i in noisePixels.indices) {
                        val gray = fastRandom.nextInt(256)
                        noisePixels[i] = -0x1000000 or (gray shl 16) or (gray shl 8) or gray
                    }
                    noiseBitmap!!.setPixels(noisePixels, 0, 512, 0, 0, 512, 512)
                    paint.shader = android.graphics.BitmapShader(noiseBitmap!!, android.graphics.Shader.TileMode.REPEAT, android.graphics.Shader.TileMode.REPEAT)
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                    paint.shader = null
                }
                MODE_HORIZONTAL_LINES -> {
                    if (bitmap == null || bitmap!!.width != width || bitmap!!.height != height) {
                        bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)
                        pixels = IntArray(width * height)
                    }
                    for (y in 0 until height) {
                        val color = fastRandom.nextInt() or -0x1000000
                        val offset = y * width
                        for (x in 0 until width) {
                            pixels[offset + x] = color
                        }
                    }
                    bitmap!!.setPixels(pixels, 0, width, 0, 0, width, height)
                    canvas.drawBitmap(bitmap!!, 0f, 0f, paint)
                }
                MODE_VERTICAL_LINES -> {
                    if (bitmap == null || bitmap!!.width != width || bitmap!!.height != height) {
                        bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)
                        pixels = IntArray(width * height)
                    }
                    for (x in 0 until width) {
                        val color = fastRandom.nextInt() or -0x1000000
                        for (y in 0 until height) {
                            pixels[y * width + x] = color
                        }
                    }
                    bitmap!!.setPixels(pixels, 0, width, 0, 0, width, height)
                    canvas.drawBitmap(bitmap!!, 0f, 0f, paint)
                }
            }
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
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
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
        private const val MODE_NOISE = 0
        private const val MODE_HORIZONTAL_LINES = 1
        private const val MODE_VERTICAL_LINES = 2
        private const val MODE_BLACK_WHITE_NOISE = 3
    }
}
