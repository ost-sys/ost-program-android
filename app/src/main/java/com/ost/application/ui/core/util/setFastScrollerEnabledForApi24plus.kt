package com.ost.application.ui.core.util

import android.os.Build
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.seslSetFastScrollerEnabledForApi24(enable: Boolean = true) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        seslSetFastScrollerEnabled(enable)
    }
}