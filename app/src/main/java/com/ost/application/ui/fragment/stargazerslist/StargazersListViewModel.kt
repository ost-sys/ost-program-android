package com.ost.application.ui.fragment.stargazerslist

import android.annotation.SuppressLint
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ost.application.OSTApp
import com.ost.application.R
import com.ost.application.data.RefreshResult
import com.ost.application.data.StargazersRepo
import com.ost.application.data.model.FetchState
import com.ost.application.data.model.SearchModeOnActionMode
import com.ost.application.data.model.StargazersSettings
import com.ost.application.ui.core.util.isOnline
import com.ost.application.ui.fragment.stargazerslist.model.StargazersListUiState
import com.ost.application.ui.fragment.stargazerslist.util.toFilteredStargazerUiModelList
import dev.oneuiproject.oneui.delegates.AllSelectorState
import dev.oneuiproject.oneui.layout.ToolbarLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Lazily
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class StargazersListViewModel (
    private val stargazersRepo: StargazersRepo,
    app: OSTApp): AndroidViewModel(app) {

    private val _queryStateFlow = MutableStateFlow("")
    private val _repoFilterStateFlow = MutableStateFlow("")

    val stargazerSettingsStateFlow = stargazersRepo.stargazersSettingsFlow
        .stateIn(viewModelScope, Lazily, StargazersSettings())

    fun getSearchModeOnBackBehavior(): ToolbarLayout.SearchModeOnBackBehavior
            = stargazerSettingsStateFlow.value.searchModeBackBehavior

    fun getKeepSearchModeOnActionMode(): Boolean
            = stargazerSettingsStateFlow.value.searchOnActionMode == SearchModeOnActionMode.RESUME

    suspend fun getStargazersById(ids: IntArray) = stargazersRepo.getStargazersById(ids)

    private val _stargazersListScreenStateFlow = MutableStateFlow(StargazersListUiState())
    val stargazersListScreenStateFlow = _stargazersListScreenStateFlow.asStateFlow()

    private val _userMessage: MutableStateFlow<String?> = MutableStateFlow(null)
    val userMessage: StateFlow<String?> = _userMessage

    val app = getApplication<OSTApp>()

    init {
        viewModelScope.launch {
            launch {
                combine(
                    stargazersRepo.stargazersFlow,
                    _queryStateFlow,
                    _repoFilterStateFlow,
                    stargazersRepo.fetchStatusFlow
                ) { stargazers, query, repoFilter, fetchStatus ->
                    val itemsList = stargazers.toFilteredStargazerUiModelList(query, repoFilter)
                    val noItemText = getNoItemText(fetchStatus, query)
                    StargazersListUiState(
                        itemsList = itemsList,
                        query = query,
                        noItemText = noItemText,
                        fetchStatus = fetchStatus
                    )
                }.collectLatest { uiState ->
                    _stargazersListScreenStateFlow.value = uiState
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun getNoItemText(fetchState: FetchState, query: String): String{
        return when (fetchState) {
            FetchState.INITING -> app.getString(R.string.loading_stargazers)
            FetchState.INIT_ERROR -> app.getString(R.string.error_loading_stargazers)
            //These will only be visible when rv is empty.
            FetchState.INITED, FetchState.REFRESHED -> if (query.isEmpty()) app.getString(R.string.no_stargazers_yet) else app.getString(R.string.no_results_found)
            FetchState.NOT_INIT, FetchState.REFRESHING, FetchState.REFRESH_ERROR -> ""
        }
    }

    fun isIndexScrollEnabled() = stargazerSettingsStateFlow.value.enableIndexScroll

    @SuppressLint("RestrictedApi")
    fun refreshStargazers(notifyResult: Boolean = true) = viewModelScope.launch {
        if (!isOnline(getApplication())) {
            _userMessage.update { app.getString(R.string.no_internet_connection_detected) }
            return@launch
        }

        stargazersRepo.refreshStargazers sr@{ result ->
            if (!notifyResult) return@sr
            when (result){
                RefreshResult.UpdateRunning -> {
                    _userMessage.update { app.getString(R.string.s_already_refreshing) }
                }
                is RefreshResult.OtherException -> {
                    _userMessage.update {  result.exception.message ?: app.getString(R.string.error_fetching_stargazers) }
                }
                RefreshResult.Updated -> {
                    _userMessage.update { app.getString(R.string.latest_stargazers_fetched) }
                }
                RefreshResult.NetworkException -> {
                    _userMessage.update { app.getString(R.string.connection_error_occurred) }
                }
            }
        }

    }

    private var setFilterJob: Job? = null
    fun setQuery(query: String?) = viewModelScope.launch {
        setFilterJob?.cancel()
        setFilterJob = launch {
            delay(200)
            _queryStateFlow.value = query ?: ""
        }
    }

    fun setRepoFilter(repoName: String)  { _repoFilterStateFlow.value = repoName }

    val allSelectorStateFlow= MutableStateFlow(AllSelectorState())

    companion object{
        private const val TAG = "StargazersListViewModel"
    }
}

class StargazersListViewModelFactory(private val stargazersRepo: StargazersRepo,
                                     private val  app: OSTApp) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StargazersListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StargazersListViewModel(stargazersRepo, app) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
