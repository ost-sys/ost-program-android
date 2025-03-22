package com.ost.application.activity.changelog

data class ChangelogItem (
    val version: String,
    val body: String,
    val downloadUrl: String?
)