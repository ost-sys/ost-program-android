package com.ost.application.data.model

import androidx.annotation.Keep

@Keep
data class StargazerDetails(
    val login: String,
    val name: String?,
    val id: Int,
    val email: String?,
    val twitter_username: String?,
    val location: String?,
    val company: String?,
    val blog: String?,
    val bio: String?
)