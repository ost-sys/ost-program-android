package com.ost.application.data.model

import com.google.gson.annotations.SerializedName

data class GitHubRepo(
    val id: Long,
    val name: String,
    @SerializedName("full_name") val fullName: String,
    val owner: GitHubUser,
    @SerializedName("stargazers_count") val starsCount: Int,
    val description: String?,
    @SerializedName("html_url") val htmlUrl: String
)