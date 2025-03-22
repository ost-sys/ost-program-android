package com.ost.application.util

data class ListItem(
    val title: String,
    val summary: String?,
    val icon: Int?,
    val onClick: (() -> Unit)?
)