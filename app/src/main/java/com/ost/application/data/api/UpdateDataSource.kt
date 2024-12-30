package com.ost.application.data.api

import android.os.Parcelable
import com.ost.application.BuildConfig
import com.ost.application.data.api.RetrofitClient.instance
import dev.oneuiproject.oneui.layout.AppInfoLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import retrofit2.Call

@Parcelize
data class Update(
    val status: Int,
    val latestRelease: String?
): Parcelable

object UpdateDataSource {
    suspend fun getUpdate(): Update = withContext(Dispatchers.IO){
        try {
            val gitHubApi = instance
            val call: Call<List<Tags>> =
                gitHubApi.getTags("ost-sys", "ost-program-android")
            val response = call.execute()

            return@withContext  if (response.isSuccessful && response.body() != null) {
                val latestTag = response.body()!!.firstOrNull()?.name
                if (latestTag == null || latestTag == BuildConfig.VERSION_NAME) {
                    Update(AppInfoLayout.NO_UPDATE, latestTag)
                } else {
                    Update(AppInfoLayout.UPDATE_AVAILABLE, latestTag)
                }
            } else {
                Update(AppInfoLayout.NOT_UPDATEABLE, null)
            }
        } catch (e: Exception) {
            return@withContext  Update(AppInfoLayout.NOT_UPDATEABLE, null)
        }
    }
}