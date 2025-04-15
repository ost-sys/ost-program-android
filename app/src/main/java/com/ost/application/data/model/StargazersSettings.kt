package com.ost.application.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StargazersSettings(
    val lastRefresh: Long = 0L,
    val initTipShown: Boolean = true,
    val updateAvailable: Boolean = false
) : Parcelable

enum class FetchState {
    NOT_INIT,
    INITING,
    INIT_ERROR,
    INITED,
    REFRESHING,
    REFRESH_ERROR,
    REFRESHED
}