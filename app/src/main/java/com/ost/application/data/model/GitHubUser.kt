package com.ost.application.data.model

import com.google.gson.annotations.SerializedName

data class GitHubUser(
    val login: String,
    val id: Long,
    @SerializedName("avatar_url") val avatarUrl: String,
    @SerializedName("html_url") val htmlUrl: String,
    val name: String?,
    val email: String?,
    @SerializedName("twitter_username") val twitterUsername: String?,
    val location: String?,
    val company: String?,
    val blog: String?,
    val bio: String?
)