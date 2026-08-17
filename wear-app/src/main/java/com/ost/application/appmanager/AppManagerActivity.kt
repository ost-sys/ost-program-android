package com.ost.application.appmanager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material3.MaterialTheme
sealed class AppManagerDestination {
    object List : AppManagerDestination()
    data class Detail(val packageName: String) : AppManagerDestination()
}
class AppManagerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppManagerNavHost()
            }
        }
    }
}
@Composable
fun AppManagerNavHost() {
    var destination: AppManagerDestination by remember {
        mutableStateOf(AppManagerDestination.List)
    }
    val isDetail = destination is AppManagerDestination.Detail
    val swipeState = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = swipeState,
        onDismissed = {
            if (isDetail) destination = AppManagerDestination.List
        },
        hasBackground = isDetail
    ) { isBackground ->
        if (isBackground) {
            AppManagerScreen(onOpenDetail = {})
        } else {
            AnimatedContent(
                targetState = destination,
                transitionSpec = {
                    when {
                        targetState is AppManagerDestination.Detail ->
                            (slideInHorizontally { it } + fadeIn()) togetherWith
                                    (slideOutHorizontally { -it / 3 } + fadeOut())
                        else ->
                            (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                                    (slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "nav_anim"
            ) { dest ->
                when (dest) {
                    is AppManagerDestination.List -> {
                        AppManagerScreen(
                            onOpenDetail = { packageName ->
                                destination = AppManagerDestination.Detail(packageName)
                            }
                        )
                    }
                    is AppManagerDestination.Detail -> {
                        AppDetailScreen(
                            packageName = dest.packageName,
                            onBack = { destination = AppManagerDestination.List }
                        )
                    }
                }
            }
        }
    }
}
