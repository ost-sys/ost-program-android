package com.ost.application.util

import androidx.annotation.DrawableRes

data class ListItem(
    val title: String,
    val summary: String?,
    @DrawableRes val icon: Int?,
    val status: Boolean,
    val onClick: (() -> Unit)?
)