package com.ost.application.ui.screen.stargazers

import com.ost.application.data.model.Stargazer

fun List<Stargazer>.toFilteredStargazerUiModelList(
    query: String,
    repoFilter: String
): List<StargazersListItemUiModel.StargazerItem> {
    val filteredList = if (repoFilter.isBlank()) {
        this
    } else {
        this.filter { it.starredRepos.contains(repoFilter) }
    }

    return filteredList.map { StargazersListItemUiModel.StargazerItem(it) }
}