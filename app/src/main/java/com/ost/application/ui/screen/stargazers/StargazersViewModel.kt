package com.ost.application.ui.screen.stargazers

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ost.application.data.model.GitHubRepo
import com.ost.application.data.model.GitHubUser
import com.ost.application.data.remote.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StargazersViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("github_prefs", Context.MODE_PRIVATE)

    private val _userToken = MutableStateFlow("")
    val userToken = _userToken.asStateFlow()

    private val _repos = MutableStateFlow<List<GitHubRepo>>(emptyList())
    val repos = _repos.asStateFlow()

    private val _users = MutableStateFlow<List<GitHubUser>>(emptyList())
    val users = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _selectedRepo = MutableStateFlow<GitHubRepo?>(null)
    val selectedRepo = _selectedRepo.asStateFlow()

    private var currentPage = 1
    private var isEndReached = false
    private val pageSize = 30
    private var loadJob: Job? = null

    init {
        val savedToken = prefs.getString("token", "") ?: ""
        if (savedToken.isNotBlank()) {
            _userToken.value = savedToken
            loadMyRepositories()
        }
    }

    fun login(token: String) {
        if (token.isBlank()) {
            _error.value = "Token cannot be empty"
            return
        }
        prefs.edit().putString("token", token).apply()
        _userToken.value = token
        loadMyRepositories()
    }

    fun logout() {
        prefs.edit().remove("token").apply()
        _userToken.value = ""
        _repos.value = emptyList()
        clearSelection()
    }

    private fun loadMyRepositories() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val tokenHeader = if (_userToken.value.startsWith("token ")) _userToken.value else "token ${_userToken.value}"
                val loadedRepos = RetrofitClient.api.getUserRepos(token = tokenHeader)
                _repos.value = loadedRepos
            } catch (e: Exception) {
                _error.value = e.localizedMessage
                _userToken.value = ""
                prefs.edit().remove("token").apply()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshRepositories() {
        loadMyRepositories()
    }

    fun selectRepo(repo: GitHubRepo) {
        _selectedRepo.value = repo
        refreshStargazers()
    }

    fun clearSelection() {
        _selectedRepo.value = null
        _users.value = emptyList()
        loadJob?.cancel()
    }

    fun loadNextPage() {
        val repo = _selectedRepo.value ?: return
        if (_isLoading.value || isEndReached) return

        loadJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                val tokenHeader = if (_userToken.value.startsWith("token ")) _userToken.value else "token ${_userToken.value}"

                val newUsers = RetrofitClient.api.getStargazers(
                    owner = repo.owner.login,
                    repo = repo.name,
                    page = currentPage,
                    perPage = pageSize,
                    token = tokenHeader
                )

                if (newUsers.isEmpty()) {
                    isEndReached = true
                } else {
                    val currentList = if (currentPage == 1) emptyList() else _users.value
                    _users.value = currentList + newUsers
                    currentPage++
                }
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshStargazers() {
        loadJob?.cancel()
        currentPage = 1
        isEndReached = false
        loadNextPage()
    }
}