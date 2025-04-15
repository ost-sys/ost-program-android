package com.ost.application.ui.screen.stargazers

import com.ost.application.data.model.Stargazer

sealed class StargazersListItemUiModel {

    data class StargazerItem(val stargazer: Stargazer) : StargazersListItemUiModel() {
        companion object { const val VIEW_TYPE = 0 }
    }

    data class SeparatorItem(val indexText: String) : StargazersListItemUiModel() {
        companion object { const val VIEW_TYPE = 1 }
    }

    fun toStableId(): Long {
        return when (this) {
            is StargazerItem -> stargazer.id.toLong()
            is SeparatorItem -> indexText.hashCode().toLong()
        }
    }
}