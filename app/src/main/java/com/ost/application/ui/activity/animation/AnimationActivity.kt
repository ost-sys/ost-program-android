package com.ost.application.ui.activity.animation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ost.application.R
import com.ost.application.ui.theme.OSTToolsTheme

@ExperimentalMaterial3ExpressiveApi
class AnimationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSTToolsTheme {
                AnimatedBackgroundScreen()
            }
        }
    }
}

@Composable
fun AnimatedBackgroundScreen() {
    val primaryGradientColor = MaterialTheme.colorScheme.primary
    val secondaryGradientColor = Color(0x00000000)
    val tertiaryGradientColor = MaterialTheme.colorScheme.tertiary

    val systemUiController = rememberSystemUiController()

    LaunchedEffect(systemUiController) {
        systemUiController.isStatusBarVisible = false
        systemUiController.isNavigationBarVisible = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        RadialGradientBackground(
            primaryColor = primaryGradientColor,
            secondaryColor = secondaryGradientColor,
            tertiaryColor = tertiaryGradientColor,
            startAnimation = true,
            alignType = RadialGradientView.AlignType.LTR,
            showArc = false,
            gradientPatternResId = R.raw.radial_gradient
        )
    }
}

@Composable
fun RadialGradientBackground(
    modifier: Modifier = Modifier,
    startAnimation: Boolean = true,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    alignType: RadialGradientView.AlignType,
    showArc: Boolean,
    gradientPatternResId: Int
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val radialGradientView = remember { RadialGradientView(context) }

    DisposableEffect(radialGradientView, lifecycleOwner, primaryColor, secondaryColor, tertiaryColor, alignType, showArc, startAnimation, gradientPatternResId) {
        radialGradientView.init(alignType, gradientPatternResId)
        radialGradientView.setColors(
            primaryColor.toArgb(),
            secondaryColor.toArgb(),
            tertiaryColor.toArgb()
        )
        radialGradientView.setArcShow(showArc)

        if (startAnimation) {
            radialGradientView.startAnimation()
        }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                radialGradientView.stopAnimation()
            } else if (event == Lifecycle.Event.ON_START) {
                if (startAnimation) {
                    radialGradientView.startAnimation()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            radialGradientView.releaseResources()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { radialGradientView },
        update = { view ->
            view.setColors(primaryColor.toArgb(), secondaryColor.toArgb(), tertiaryColor.toArgb())
            view.setArcShow(showArc)

            if (startAnimation) {
                view.startAnimation()
            } else {
                view.stopAnimation()
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewAnimatedBackgroundScreen() {
    AnimatedBackgroundScreen()
}