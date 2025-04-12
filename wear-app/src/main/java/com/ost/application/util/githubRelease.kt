package com.ost.application.util

data class GitHubRelease(
    val tag_name: String,
    val name: String,
    val body: String,
    val published_at: String
)