package com.ost.application.ui.fragment.stargazerslist.model

import com.ost.application.ui.fragment.stargazerslist.StargazersListViewModel

data class StargazersListUiState(
    val itemsList: List<StargazersListItemUiModel> = emptyList(),
    val query: String = "",
    val noItemText: String = "No contacts",
    val loadState: StargazersListViewModel.LoadState = StargazersListViewModel.LoadState.LOADING
)