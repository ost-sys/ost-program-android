package com.tribalfs.stargazers.ui.core.util

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import com.ost.application.ui.core.util.defaultActivityWindowBackground
import com.ost.application.R

fun Activity.setWindowTransparent(transparent: Boolean){
    window.apply {
        if (transparent) {
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setBackgroundDrawableResource(R.color.transparent_window_bg_color)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setTranslucent(true)
            }
        }else{
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setBackgroundDrawableResource(defaultActivityWindowBackground)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setTranslucent(false)
            }
        }
    }
}
