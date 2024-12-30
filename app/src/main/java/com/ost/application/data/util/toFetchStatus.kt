package com.ost.application.data.util

import com.ost.application.data.model.FetchState

fun Int?.toFetchStatus(): FetchState = if (this != null){
    FetchState.entries[this]
} else FetchState.NOT_INIT