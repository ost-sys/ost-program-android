package com.ost.application.data.model

import android.os.Parcelable
import androidx.annotation.ColorInt
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior
import kotlinx.parcelize.Parcelize

@Parcelize
data class StargazersSettings(
    val isTextModeIndexScroll: Boolean = false,
    val autoHideIndexScroll: Boolean = true,
    val searchOnActionMode: SearchModeOnActionMode = SearchModeOnActionMode.DISMISS,
    val searchModeBackBehavior: SearchModeOnBackBehavior = SearchModeOnBackBehavior.CLEAR_DISMISS,
    @ColorInt
    val searchHighlightColor: Int = -1,
    val darkModeOption: DarkMode = DarkMode.AUTO,
    val enableIndexScroll: Boolean = true,
    val lastRefresh: Long = 0L,
    val initTipShown: Boolean = true,
    val updateAvailable: Boolean = false
) : Parcelable

enum class DarkMode {
    AUTO,
    DISABLED,
    ENABLED
}

enum class SearchModeOnActionMode {
    DISMISS,
    RESUME
}

enum class FetchState{
    NOT_INIT,
    INITING,
    INIT_ERROR,
    INITED,
    REFRESHING,
    REFRESH_ERROR,
    REFRESHED
}