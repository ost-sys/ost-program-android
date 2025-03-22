package com.ost.application.presentation

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
import com.ost.application.util.CardItem
import com.ost.application.util.ListItem
import com.ost.application.util.ListItems
import kotlinx.coroutines.launch

class DefaultActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DefaultScreen()
        }
    }
}

@Composable
fun DefaultScreen() {
    val listState = rememberScalingLazyListState()
    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
    ) {
        DefaultList(listState)
    }
}

@Composable
fun DefaultList(listState: ScalingLazyListState) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val systemVersion = stringResource(R.string.system_version)
    val brand = stringResource(R.string.brand)
    val board = stringResource(R.string.board)
    val buildNumber = stringResource(R.string.build_number)
    val buildFingerprint = stringResource(R.string.build_fingerprint)

    val items = remember {
        listOf(
            ListItem(systemVersion, Build.VERSION.RELEASE, null) { /*TODO()*/ },
            ListItem(brand, Build.BRAND, null) { /*TODO()*/ },
            ListItem(board, Build.BOARD, null) { /*TODO()*/ },
            ListItem(buildNumber, getBuildNumber(), null) { /*TODO()*/ },
            ListItem("SDK", Build.VERSION.SDK_INT.toString(), null) { /*TODO()*/ },
            ListItem(buildFingerprint, Build.FINGERPRINT, null) { /*TODO()*/ },
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
                            val verticalScroll = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
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
            ListItems(getDeviceModel(), null)
        }

        items(items.size) { index ->
            val item = items[index]
            CardItem(
                title = item.title,
                summary = item.summary,
                icon = item.icon,
                onClick = null
            )
        }
    }
}

@SuppressLint("PrivateApi")
fun getSystemProperty(key: String?): String? {
    var value: String? = null
    try {
        value = Class.forName("android.os.SystemProperties")
            .getMethod("get", String::class.java).invoke(null, key) as String
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return value
}

fun getBuildNumber(): String? {
    return getSystemProperty("ro.system.build.id")
}
fun getDeviceModel(): String? {
    return getSystemProperty("ro.product.system.model")
}