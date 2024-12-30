package com.ost.application.ui.fragment.stargazerslist.model

import com.ost.application.data.model.FetchState

data class StargazersListUiState(
    val itemsList: List<StargazersListItemUiModel> = emptyList(),
    val query: String = "",
    val noItemText: String = "No contacts",
    val fetchStatus: FetchState = FetchState.INITED
)