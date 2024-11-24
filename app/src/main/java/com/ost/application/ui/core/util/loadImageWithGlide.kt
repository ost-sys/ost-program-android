package com.ost.application.ui.core.util

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.ost.application.R

fun ImageView.loadImageFromUrl(imageUrl: String){
    Glide.with(context)
        .load(imageUrl)
        .placeholder(R.drawable.indexscroll_item_icon)
        .error(dev.oneuiproject.oneui.R.drawable.ic_oui_error_2)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .circleCrop()
        .into(this)
}