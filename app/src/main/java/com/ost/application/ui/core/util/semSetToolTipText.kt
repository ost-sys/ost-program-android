package com.ost.application.ui.core.util

import android.view.View
import androidx.appcompat.widget.TooltipCompat

/**
 * Sets OneUI style tooltip text to this View.
 */
inline fun View.semSetToolTipText(toolTipText: CharSequence?) {
    TooltipCompat.setTooltipText(this, toolTipText)
}
