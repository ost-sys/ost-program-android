package com.ost.application.data.api

import androidx.annotation.Keep
import com.ost.application.data.model.Stargazer
import com.ost.application.data.model.StargazerDetails
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface GitHubService {
    @GET("repos/{owner}/{repo}/stargazers")
    suspend fun getStargazers(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): List<Stargazer>

    @GET("users/{username}")
    suspend fun getUserDetails(
        @Path("username") username: String
    ): StargazerDetails

    @GET("repos/{owner}/{repo}/tags")
    fun getTags(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Call<List<Tags>>

}

@Keep
data class Tags(
    @JvmField
    val name: String
)