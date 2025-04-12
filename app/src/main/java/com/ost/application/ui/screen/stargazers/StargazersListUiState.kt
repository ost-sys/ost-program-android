package com.ost.application.ui.screen.stargazers

import com.ost.application.data.model.FetchState

data class StargazersListUiState(
    val itemsList: List<StargazersListItemUiModel> = emptyList(),
    val noItemText: String = "No stargazers",
    val fetchStatus: FetchState = FetchState.INITED
)