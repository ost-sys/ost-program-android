package com.ost.application.data.model

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
@Entity(tableName = "stargazers")
data class Stargazer(
    @PrimaryKey
    val login: String,
    val id: Int,
    val node_id: String,
    val avatar_url: String,
    val html_url: String,
    val name: String? = null,
    val organizations_url: String?,
    val location: String?,
    val company: String?,
    val email: String?,
    val twitter_username: String?,
    val blog: String?,
    val bio: String?
): Parcelable {

    fun getDisplayName(): String = name ?: login
    fun getSearchableString(): String = "${name?:""} $html_url"
}