package com.ost.application.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ost.application.data.api.NetworkDataSource
import com.ost.application.data.datastore.PreferenceDataStoreImpl
import com.ost.application.data.model.FetchState
import com.ost.application.data.model.FetchState.INITED
import com.ost.application.data.model.FetchState.INITING
import com.ost.application.data.model.FetchState.INIT_ERROR
import com.ost.application.data.model.FetchState.NOT_INIT
import com.ost.application.data.model.FetchState.REFRESHED
import com.ost.application.data.model.FetchState.REFRESHING
import com.ost.application.data.model.FetchState.REFRESH_ERROR
import com.ost.application.data.model.SearchModeOnActionMode
import com.ost.application.data.model.Stargazer
import com.ost.application.data.model.StargazersSettings
import com.ost.application.data.room.StargazersDB
import com.ost.application.data.util.determineDarkMode
import com.ost.application.data.util.toFetchStatus
import dev.oneuiproject.oneui.layout.ToolbarLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import kotlin.coroutines.CoroutineContext

sealed class RefreshResult{
    object Updated : RefreshResult()
    object UpdateRunning : RefreshResult()
    object NetworkException : RefreshResult()
    data class OtherException(val exception: Throwable) : RefreshResult()
}

class StargazersRepo(
    context: Context,
    override val coroutineContext: CoroutineContext = Dispatchers.IO + SupervisorJob()
): CoroutineScope {

    private val appContext = context.applicationContext
    private val database = StargazersDB.getDatabase(appContext, this)

    private val prefDataStoreImpl = PreferenceDataStoreImpl.getInstance(appContext)
    private val dataStore = prefDataStoreImpl.dataStore

    val stargazersFlow: Flow<List<Stargazer>> = database.stargazerDao().getAllStargazers()

    private val refreshMutex = Mutex()

    suspend fun refreshStargazers(callback: ((result: RefreshResult) -> Unit)? = null) =
        withContext(Dispatchers.IO) {
            if (!refreshMutex.tryLock()) {
                callback?.invoke(RefreshResult.UpdateRunning)
                return@withContext
            }

            setOnStartFetchStatus()
            NetworkDataSource.fetchStargazers()
                .onSuccess {
                    database.stargazerDao().replaceAll(it)
                    updateLastRefresh(System.currentTimeMillis())
                    setOnFinishFetchStatus(true)
                    callback?.invoke(RefreshResult.Updated)
                }
                .onFailure {
                    setOnFinishFetchStatus(false)
                    when(it){
                        is HttpException -> callback?.invoke(RefreshResult.NetworkException)
                        else -> callback?.invoke(RefreshResult.OtherException(it))
                    }
                }

            refreshMutex.unlock()
        }

    val fetchStatusFlow: Flow<FetchState> = dataStore.data.map { it[PREF_INIT_FETCH_STATE].toFetchStatus() }

    private var fetchStatusCache: FetchState = NOT_INIT

    init {
        launch {
            fetchStatusCache = fetchStatusFlow.first()
        }
    }

    private fun setOnStartFetchStatus() {
        when (fetchStatusCache) {
            NOT_INIT, INIT_ERROR -> setFetchStatus(INITING)
            INITED, REFRESH_ERROR, REFRESHED -> setFetchStatus(REFRESHING)
            INITING, REFRESHING -> Unit
        }
    }

    private fun setOnFinishFetchStatus(isSuccess: Boolean) {
        when (fetchStatusCache) {
            REFRESHING -> setFetchStatus(if (isSuccess) REFRESHED else REFRESH_ERROR)
            INITING -> setFetchStatus(if (isSuccess) INITED else INIT_ERROR)
            else -> Unit
        }
    }

    fun setFetchStatus(state: FetchState) {
        fetchStatusCache = state
        prefDataStoreImpl.putInt(PREF_INIT_FETCH_STATE.name, state.ordinal)
    }

    fun updateLastRefresh(timeMillis: Long) = prefDataStoreImpl.putLong(PREF_LAST_REFRESH.name, timeMillis)

    val stargazersSettingsFlow: Flow<StargazersSettings> = dataStore.data.map {
        val darkMode = determineDarkMode(
            it[PREF_DARK_MODE] ?: "0",
            it[PREF_AUTO_DARK_MODE] ?: true)

        StargazersSettings(
            isTextModeIndexScroll = it[PREF_INDEXSCROLL_TEXT_MODE] ?: false,
            autoHideIndexScroll = it[PREF_INDEXSCROLL_AUTO_HIDE] ?: true,
            searchOnActionMode = it[PREF_ACTIONMODE_SEARCH].toSearchModeOnActionMode(),
            searchModeBackBehavior = it[PREF_SEARCHMODE_BACK_BEHAVIOR].toSearchModeOnBackBehavior(),
            darkModeOption = darkMode,
            enableIndexScroll = it[PREF_INDEXSCROLL_ENABLE] ?: true,
            lastRefresh = it[PREF_LAST_REFRESH] ?: 0,
            initTipShown = it[PREF_INIT_TIP_SHOWN] ?: false,
            updateAvailable = it[PREF_UPDATE_AVAILABLE] ?: false
        )
    }

    suspend fun getStargazersById(ids: IntArray) = database.stargazerDao().getStargazersById(ids)

    companion object {
        @Volatile
        private var INSTANCE: StargazersRepo? = null

        fun getInstance(context: Context): StargazersRepo = INSTANCE
            ?: synchronized(this) {
                StargazersRepo(context.applicationContext).also {
                    INSTANCE = it
                }
            }

        val PREF_INDEXSCROLL_ENABLE = booleanPreferencesKey("enableIndexScroll")
        val PREF_INDEXSCROLL_TEXT_MODE = booleanPreferencesKey("indexScrollTextMode")
        val PREF_INDEXSCROLL_AUTO_HIDE = booleanPreferencesKey("indexScrollAutoHide")
        val PREF_ACTIONMODE_SEARCH = stringPreferencesKey("actionModeSearch")
        val PREF_SEARCHMODE_BACK_BEHAVIOR = stringPreferencesKey("searchModeBackBehavior")
        val PREF_DARK_MODE = stringPreferencesKey("darkMode")
        val PREF_AUTO_DARK_MODE = booleanPreferencesKey("darkModeAuto")
        val PREF_LAST_REFRESH = longPreferencesKey("lastRefresh")
        val PREF_UPDATE_AVAILABLE = booleanPreferencesKey("updateAvailable")
        private val PREF_INIT_TIP_SHOWN = booleanPreferencesKey("initTipShown")
        private val PREF_INIT_FETCH_STATE = intPreferencesKey("initFetch")
    }

    private fun String?.toSearchModeOnBackBehavior() =
        if (this == null) ToolbarLayout.SearchModeOnBackBehavior.CLEAR_DISMISS else ToolbarLayout.SearchModeOnBackBehavior.entries[toInt()]

    private fun String?.toSearchModeOnActionMode() =
        if (this == null) SearchModeOnActionMode.DISMISS else SearchModeOnActionMode.entries[toInt()]
}