package com.ost.application.ui.state

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class FabSize {
    Normal,
    Small
}

class FabController {
    var iconRes: Int? by mutableStateOf(null)
        private set
    var contentDescription: String? by mutableStateOf(null)
        private set
    var onClick: () -> Unit by mutableStateOf({})
        private set
    var isVisible: Boolean by mutableStateOf(false)
        private set
    var size: FabSize by mutableStateOf(FabSize.Normal) // Новое поле
        private set

    fun setFab(
        icon: Int,
        description: String,
        fabSize: FabSize = FabSize.Normal, // По дефолту обычный
        action: () -> Unit
    ) {
        iconRes = icon
        contentDescription = description
        size = fabSize
        onClick = action
        isVisible = true
    }

    fun hideFab() {
        isVisible = false
    }
}

val LocalFabController = compositionLocalOf { FabController() }