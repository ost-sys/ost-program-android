package com.ost.application.data.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Update(
    val status: Int,
    val latestRelease: String?
): Parcelable

