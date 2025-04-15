package com.ost.application.ui.screen.stargazers

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ost.application.OSTApp
import com.ost.application.R
import com.ost.application.data.model.FetchState
import com.ost.application.data.model.RefreshResult
import com.ost.application.data.model.StargazersRepo
import com.ost.application.data.model.isOnline
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StargazersListViewModel(
    private val stargazersRepo: StargazersRepo,
    val app: OSTApp
) : AndroidViewModel(app) {

    private val TAG = "StargazersListViewModel"

    private val _repoFilterStateFlow = MutableStateFlow("")

    private val _stargazersListScreenStateFlow = MutableStateFlow(StargazersListUiState(fetchStatus = FetchState.NOT_INIT))
    val stargazersListScreenStateFlow: StateFlow<StargazersListUiState> = _stargazersListScreenStateFlow.asStateFlow()

    private val _userMessage: MutableStateFlow<String?> = MutableStateFlow(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        viewModelScope.launch {
            Log.d(TAG, "ViewModel Initializing and starting data collection...")
            combine(
                stargazersRepo.stargazersFlow,
                stargazersRepo.fetchStatusFlow,
                _repoFilterStateFlow
            ) { stargazers, fetchStatus, repoFilter ->
                Log.d(TAG, "Combine triggered -> Status: $fetchStatus, DB Stargazers: ${stargazers.size}, RepoFilter: '$repoFilter'")

                val itemsList = stargazers.toFilteredStargazerUiModelList(
                    query = "",
                    repoFilter = repoFilter
                )

                val noItemText = getNoItemText(fetchStatus, itemsList.isEmpty())

                Log.d(TAG, "Combine result -> UI Items: ${itemsList.size}, Status: $fetchStatus, NoItemText: '$noItemText'")

                StargazersListUiState(
                    itemsList = itemsList,
                    noItemText = noItemText,
                    fetchStatus = fetchStatus
                )
            }.catch { e ->
                Log.e(TAG, "Error in ViewModel combine", e)
                _stargazersListScreenStateFlow.update {
                    it.copy(
                        fetchStatus = FetchState.INIT_ERROR,
                        noItemText = "Error processing data: ${e.message}"
                    )
                }
            }
                .collectLatest { newState ->
                    _stargazersListScreenStateFlow.value = newState
                    Log.d(TAG, "UI State Updated: Status=${newState.fetchStatus}, Items=${newState.itemsList.size}")
                }
        }

        viewModelScope.launch {
            delay(100)
            if (stargazersRepo.fetchStatusFlow.value == FetchState.NOT_INIT) {
                Log.d(TAG, "Initial status is NOT_INIT, triggering initial refresh...")
                refreshStargazers(notifyResult = false)
            } else {
                Log.d(TAG, "Initial status is ${stargazersRepo.fetchStatusFlow.value}, initial refresh not needed.")
            }
        }
    }

    suspend fun getStargazersById(ids: IntArray) = stargazersRepo.getStargazersById(ids)

    @SuppressLint("RestrictedApi")
    private fun getNoItemText(fetchState: FetchState, isListEmptyAfterFilter: Boolean): String {
        val context = getApplication<OSTApp>()
        return when (fetchState) {
            FetchState.INITING -> context.getString(R.string.loading_stargazers)
            FetchState.INIT_ERROR -> context.getString(R.string.error_loading_stargazers)
            FetchState.REFRESHING -> ""
            FetchState.REFRESH_ERROR -> context.getString(R.string.error_fetching_stargazers)
            FetchState.INITED, FetchState.REFRESHED -> {
                if (isListEmptyAfterFilter) {
                    context.getString(R.string.no_stargazers_yet)
                } else {
                    ""
                }
            }
            FetchState.NOT_INIT -> ""
        }
    }

    @SuppressLint("RestrictedApi")
    fun refreshStargazers(notifyResult: Boolean = true) {
        Log.d(TAG, "UI requested refresh (notifyResult=$notifyResult)")
        viewModelScope.launch {
            if (!isOnline(getApplication())) {
                Log.w(TAG, "Refresh requested but no internet connection.")
                _userMessage.update { getApplication<OSTApp>().getString(R.string.no_internet_connection_detected) }
                return@launch
            }

            stargazersRepo.refreshStargazers { result ->
                if (notifyResult) {
                    val message = when (result) {
                        RefreshResult.UpdateRunning -> app.getString(R.string.s_already_refreshing)
                        is RefreshResult.OtherException -> result.exception.message ?: app.getString(R.string.error_fetching_stargazers)
                        RefreshResult.Updated -> app.getString(R.string.latest_stargazers_fetched)
                        RefreshResult.NetworkException -> app.getString(R.string.connection_error_occurred)
                    }
                    _userMessage.update { message }
                    viewModelScope.launch {
                        delay(3000)
                        clearUserMessage()
                    }
                }
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.update { null }
    }

}

class StargazersListViewModelFactory(
    private val stargazersRepo: StargazersRepo,
    private val app: OSTApp
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StargazersListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StargazersListViewModel(stargazersRepo, app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}