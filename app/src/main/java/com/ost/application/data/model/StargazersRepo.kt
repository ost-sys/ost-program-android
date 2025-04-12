package com.ost.application.data.model // Убедись что пакет правильный, возможно com.ost.application.data ?

import android.content.Context
import android.util.Log
import com.ost.application.data.model.FetchState.INITED
import com.ost.application.data.model.FetchState.INITING
import com.ost.application.data.model.FetchState.INIT_ERROR
import com.ost.application.data.model.FetchState.NOT_INIT
import com.ost.application.data.model.FetchState.REFRESHED
import com.ost.application.data.model.FetchState.REFRESHING
import com.ost.application.data.model.FetchState.REFRESH_ERROR
import com.ost.application.data.room.StargazersDB
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.CoroutineContext

// RefreshResult остается без изменений
sealed class RefreshResult{
    object Updated : RefreshResult()
    object UpdateRunning : RefreshResult()
    object NetworkException : RefreshResult() // Оставим для общего случая сети
    data class OtherException(val exception: Throwable) : RefreshResult()
}

class StargazersRepo private constructor( // Сделаем конструктор приватным для синглтона
    private val appContext: Context,
    override val coroutineContext: CoroutineContext = Dispatchers.IO + SupervisorJob()
): CoroutineScope {

    private val database = StargazersDB.getDatabase(appContext, this) // Используем appContext
    private val stargazersDao = database.stargazerDao() // Получаем DAO один раз

    private val refreshMutex = Mutex()

    // --- FLOW ДЛЯ СТАТУСА ---
    private val _fetchStatusFlow = MutableStateFlow(NOT_INIT)
    val fetchStatusFlow: StateFlow<FetchState> = _fetchStatusFlow.asStateFlow()
    // --- КОНЕЦ FLOW СТАТУСА ---

    // --- FLOW ДАННЫХ ИЗ DAO ---
    val stargazersFlow: Flow<List<Stargazer>> = stargazersDao.getAllStargazers()
    // --- КОНЕЦ FLOW ДАННЫХ ---

    // --- ЛОГИРОВАНИЕ ---
    private val TAG = "StargazersRepo"

    // Первичную загрузку лучше инициировать из ViewModel, когда он создается
    // init { checkInitialData() }
    // ... (checkInitialData() убрана для простоты, запуск из ViewModel)

    suspend fun refreshStargazers(callback: ((result: RefreshResult) -> Unit)? = null) {
        refreshMutex.withLock {
            // Проверяем, не идет ли уже обновление
            if (_fetchStatusFlow.value == REFRESHING || _fetchStatusFlow.value == INITING) {
                Log.d(TAG, "Refresh already running, current state: ${_fetchStatusFlow.value}")
                callback?.invoke(RefreshResult.UpdateRunning)
                return@withLock // Выходим, если уже обновляется
            }

            setOnStartFetchStatus() // Устанавливаем статус REFRESHING или INITING

            Log.d(TAG, "Starting network fetch...")
            NetworkDataSource.fetchStargazers() // Используем fetchStargazers, который возвращает Result
                .onSuccess { stargazers ->
                    Log.d(TAG, "Fetched ${stargazers.size} stargazers from network.")
                    // Сохраняем в БД
                    stargazersDao.replaceAll(stargazers)
                    Log.d(TAG, "Saved to DB. Setting status to REFRESHED/INITED.")
                    setOnFinishFetchStatus(true) // Устанавливаем статус REFRESHED или INITED
                    callback?.invoke(RefreshResult.Updated)
                }
                .onFailure { error ->
                    Log.e(TAG, "Fetch failed", error)
                    setOnFinishFetchStatus(false) // Устанавливаем статус REFRESH_ERROR или INIT_ERROR
                    val result = when (error) {
                        is HttpException -> RefreshResult.NetworkException
                        is IOException -> RefreshResult.NetworkException
                        else -> RefreshResult.OtherException(error)
                    }
                    callback?.invoke(result)
                }
        }
    }

    private fun setOnStartFetchStatus() {
        val currentStatus = _fetchStatusFlow.value
        val newStatus = when (currentStatus) {
            NOT_INIT, INIT_ERROR -> INITING
            INITED, REFRESH_ERROR, REFRESHED -> REFRESHING
            INITING, REFRESHING -> currentStatus // Не меняем, если уже идет загрузка
        }
        if (newStatus != currentStatus) {
            Log.d(TAG, "Setting fetch status from $currentStatus to $newStatus")
            _fetchStatusFlow.value = newStatus
        }
    }

    private fun setOnFinishFetchStatus(isSuccess: Boolean) {
        val currentStatus = _fetchStatusFlow.value
        val newStatus = when (currentStatus) {
            REFRESHING -> if (isSuccess) REFRESHED else REFRESH_ERROR
            INITING -> if (isSuccess) INITED else INIT_ERROR
            else -> currentStatus // Если статус изменился, пока шла загрузка, не трогаем
        }
        if (newStatus != currentStatus) {
            Log.d(TAG, "Setting fetch status from $currentStatus to $newStatus (Success: $isSuccess)")
            _fetchStatusFlow.value = newStatus
        }
    }

    suspend fun getStargazersById(ids: IntArray): List<Stargazer> = withContext(Dispatchers.IO) {
        try { // Добавим try-catch и для запроса из БД
            stargazersDao.getStargazersById(ids)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting stargazers by ID from DB", e)
            emptyList()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: StargazersRepo? = null

        fun getInstance(context: Context): StargazersRepo = INSTANCE
            ?: synchronized(this) {
                INSTANCE ?: StargazersRepo(context.applicationContext).also { INSTANCE = it }
            }
    }
}