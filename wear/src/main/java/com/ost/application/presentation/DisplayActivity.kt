package com.ost.application.presentation
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.ost.application.R
import com.ost.application.presentation.tools.PixelTestActivity
import com.ost.application.util.CardItem
import com.ost.application.util.ListItem
import com.ost.application.util.ListItems
import kotlinx.coroutines.launch

class DisplayActivity : ComponentActivity() {

    private val writeSettingsRequestCode = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DisplayScreen()
        }
    }

    @Composable
    fun DisplayScreen() {
        val listState = rememberScalingLazyListState()
        Scaffold(
            timeText = { TimeText() },
            vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
            positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
        ) {
            DisplayList(listState = listState)
        }
    }

    @SuppressLint("DefaultLocale")
    @Composable
    fun DisplayList(listState: ScalingLazyListState) {
        val context = LocalContext.current
        val focusRequester = remember { FocusRequester() }
        val coroutineScope = rememberCoroutineScope()

        val displayMetrics = context.resources.displayMetrics
        val densityDpi = displayMetrics.densityDpi
        val xdpi = displayMetrics.xdpi
        val ydpi = displayMetrics.ydpi
        val widthPixels = displayMetrics.widthPixels
        val heightPixels = displayMetrics.heightPixels
        val refreshRate = display.refreshRate

        val x = widthPixels / xdpi
        val y = heightPixels / ydpi

        val screenInches = kotlin.math.sqrt((x * x + y * y).toDouble())

        val screenResolution = "${widthPixels}x${heightPixels}"

        val refresh_rate = stringResource(R.string.refresh_rate)
        val screen_diagonal = stringResource(R.string.screen_diagonal)
        val check_for_dead_pixels = stringResource(R.string.check_for_dead_pixels)

        val items = remember {
            listOf(
                ListItem(refresh_rate, String.format("%.0f ${context.getString(R.string.hz)}", refreshRate), null) { Log.i("CLICK", "CLICK") },
                ListItem("DPI", "$densityDpi dpi", null) { Log.i("CLICK", "CLICK") },
                ListItem(screen_diagonal, String.format("%.1f ${context.getString(R.string.inches)}", screenInches), null) { Log.i("CLICK", "CLICK") },
                ListItem(check_for_dead_pixels, null, null) {
                    checkAndStartPixelTestActivity(context, this@DisplayActivity, writeSettingsRequestCode)
                }
            )
        }

        val sensitivity = 5.0f

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Scroll) {
                                val verticalScroll =
                                    event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                if (verticalScroll != 0f) {
                                    coroutineScope.launch {
                                        listState.scrollBy(verticalScroll * sensitivity)
                                    }
                                }
                            }
                        }
                    }
                },
            contentPadding = PaddingValues(2.dp),
            anchorType = ScalingLazyListAnchorType.ItemCenter,
            state = listState
        ) {

            item {
                ListItems(screenResolution, null)
            }

            items(items.size) { index ->
                val item = items[index]
                CardItem(
                    title = item.title,
                    summary = item.summary,
                    icon = item.icon,
                    onClick = item.onClick
                )
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == writeSettingsRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startPixelTestActivity(this)
            } else {
                Log.d("com.ost.application.presentation.DisplayActivity", "WRITE_SETTINGS permission denied")
            }
        }
    }


    @SuppressLint("WearRecents")
    fun checkAndStartPixelTestActivity(context: Context, activity: ComponentActivity, requestCode: Int) {
        if (Settings.System.canWrite(context)) {
            startPixelTestActivity(activity)
        } else {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = "package:${context.packageName}".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            activity.startActivityForResult(intent, requestCode)
        }
    }

    fun startPixelTestActivity(activity: ComponentActivity) {
        activity.startActivity(Intent(activity, PixelTestActivity::class.java))
    }
}