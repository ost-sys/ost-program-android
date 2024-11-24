package com.ost.application.data

import android.content.Context
import android.graphics.Color
import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ost.application.data.model.Stargazer
import dev.oneuiproject.oneui.layout.ToolbarLayout.SearchModeOnBackBehavior
import com.ost.application.data.api.RetrofitClient
import com.ost.application.data.datastore.appPreferences
import com.ost.application.data.room.StargazersDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize

enum class SearchModeOnActionMode{
    DISMISS,
    RESUME
}

enum class DarkMode {
    AUTO,
    DISABLED,
    ENABLED
}



@Parcelize
data class StargazersSettings(
    val isTextModeIndexScroll: Boolean = false,
    val autoHideIndexScroll: Boolean = true,
    val searchOnActionMode: SearchModeOnActionMode = SearchModeOnActionMode.DISMISS,
    val searchModeBackBehavior: SearchModeOnBackBehavior = SearchModeOnBackBehavior.CLEAR_DISMISS,
    @ColorInt
    val searchHighlightColor: Int = Color.parseColor("#2196F3"),
    val darkModeOption: DarkMode = DarkMode.AUTO,
    val enableIndexScroll: Boolean = true
) : Parcelable

class StargazersRepo (context: Context) {
    private val appContext = context.applicationContext
    private val database = StargazersDB.getDatabase(appContext)

    private val dataStore: DataStore<Preferences> = appContext.appPreferences

    val stargazersFlow: Flow<List<Stargazer>> = database.stargazerDao().getAllStargazers()

    suspend fun refreshStargazers(onRefreshComplete: (isSuccess: Boolean) -> Unit) = withContext(Dispatchers.IO) {
        try {
            fetchStargazers()?.let {
                updateLocalDb(it)
                onRefreshComplete(true)
            } ?: run {
                onRefreshComplete(false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onRefreshComplete(false)
        }
    }

    private suspend fun updateLocalDb(stargazers: List<Stargazer>){
        database.stargazerDao().replaceAll(stargazers)
    }

    private suspend fun fetchStargazers(): List<Stargazer>? = withContext(Dispatchers.IO){
        val repoList= listOf(
            "ost-program-android",
            "ost-sys.github.io")

        try {
            with(RetrofitClient.instance) {
                repoList.map {
                    async { getStargazers("ost-sys", it) }
                }.awaitAll()
                    .flatten()
                    .distinct()
                    .map {
                        async {
                            val userDetails = getUserDetails(it.login)
                            it.copy(
                                name = userDetails.name,
                                location = userDetails.location,
                                company = userDetails.company,
                                email = userDetails.email,
                                twitter_username = userDetails.twitter_username,
                                blog = userDetails.blog,
                                bio = userDetails.bio,
                            )
                        }
                    }
                    .awaitAll()
                    .sortedWith(compareBy(
                        { it.getDisplayName().first().isDigit() },
                        { it.getDisplayName().uppercase() }
                    ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    val stargazersSettingsFlow: Flow<StargazersSettings>  = dataStore.data.map {
        val darkModeAutoValue = it[PREF_AUTO_DARK_MODE] ?: false
        val darkMode = if (darkModeAutoValue){
            DarkMode.AUTO
        }else {
            when(it[PREF_DARK_MODE]){
                "0" -> DarkMode.DISABLED
                else -> DarkMode.ENABLED
            }
        }
        
        val searchModeBackBehavior = it[PREF_SEARCHMODE_BACK_BEHAVIOR].let { smob ->
            if (smob == null) SearchModeOnBackBehavior.CLEAR_DISMISS else SearchModeOnBackBehavior.entries[smob.toInt()]
        }

        val searchModeOnActionMode = it[PREF_ACTIONMODE_SEARCH].let { smoam ->
            if (smoam == null) SearchModeOnActionMode.DISMISS else SearchModeOnActionMode.entries[smoam.toInt()]
        }

        StargazersSettings(
            isTextModeIndexScroll = it[PREF_INDEXSCROLL_TEXT_MODE] ?: false,
            autoHideIndexScroll = it[PREF_INDEXSCROLL_AUTO_HIDE] ?: true,
            searchOnActionMode = searchModeOnActionMode,
            searchModeBackBehavior = searchModeBackBehavior,
            searchHighlightColor = it[PREF_SEACH_HIGHLIGHT_COLOR]?: Color.parseColor("#2196F3"),
            darkModeOption =  darkMode,
            enableIndexScroll = it[PREF_INDEXSCROLL_ENABLE] ?: true
        )
    }

    companion object{
        val PREF_INDEXSCROLL_ENABLE = booleanPreferencesKey("enableIndexScroll")
        val PREF_INDEXSCROLL_TEXT_MODE = booleanPreferencesKey("indexScrollTextMode")
        val PREF_INDEXSCROLL_AUTO_HIDE = booleanPreferencesKey("indexScrollAutoHide")
        val PREF_ACTIONMODE_SEARCH = stringPreferencesKey("actionModeSearch")
        val PREF_SEARCHMODE_BACK_BEHAVIOR = stringPreferencesKey("searchModeBackBehavior")
        val PREF_SEACH_HIGHLIGHT_COLOR = intPreferencesKey ("searchColor")
        val PREF_DARK_MODE = stringPreferencesKey("darkMode")
        val PREF_AUTO_DARK_MODE = booleanPreferencesKey("darkModeAuto")
    }
}
