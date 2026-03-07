package com.ost.application.data.remote

import com.ost.application.data.model.GitHubRepo
import com.ost.application.data.model.GitHubUser
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubApi {
    @GET("user/repos")
    suspend fun getUserRepos(
        @Header("Authorization") token: String,
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 100,
        @Query("type") type: String = "all"
    ): List<GitHubRepo>
    @GET("repos/{owner}/{repo}/stargazers")
    suspend fun getStargazers(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
        @Header("Authorization") token: String
    ): List<GitHubUser>

    @GET("users/{username}")
    suspend fun getUserDetails(
        @Path("username") username: String,
        @Header("Authorization") token: String? = null
    ): GitHubUser
}