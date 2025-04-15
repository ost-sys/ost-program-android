package com.ost.application.ui.screen.stargazers

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.OSTApp
import com.ost.application.R
import com.ost.application.data.model.FetchState
import com.ost.application.data.model.StargazersRepo
import com.ost.application.utils.openUrl

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StargazersScreen(
    modifier: Modifier = Modifier,
    viewModel: StargazersListViewModel = viewModel(
        factory = StargazersListViewModelFactory(
            StargazersRepo.getInstance(LocalContext.current),
            LocalContext.current.applicationContext as OSTApp
        )
    )
) {
    val uiState by viewModel.stargazersListScreenStateFlow.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(message = it)
            viewModel.clearUserMessage()
        }
    }

    val filteredList = remember(uiState.itemsList, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.itemsList
        } else {
            val filteredItems = mutableListOf<StargazersListItemUiModel>()
            var keepSeparator = false
            uiState.itemsList.forEach { item ->
                when (item) {
                    is StargazersListItemUiModel.StargazerItem -> {
                        if (item.stargazer.getDisplayName().contains(searchQuery, ignoreCase = true)) {
                            if (keepSeparator) {
                                val lastAdded = filteredItems.lastOrNull()
                                if(lastAdded == null || lastAdded !is StargazersListItemUiModel.SeparatorItem){
                                    // Find previous separator if needed (complex, skip for now)
                                }
                                // Need logic to add separator *before* this item if missing
                            }
                            filteredItems.add(item)
                            keepSeparator = false
                        }
                    }
                    is StargazersListItemUiModel.SeparatorItem -> {
                        // Tentatively keep separator, will check next item
                        // This logic is flawed, simple filter is better for now
                        // keepSeparator = true
                    }
                }
            }
            uiState.itemsList.filter { item ->
                item is StargazersListItemUiModel.StargazerItem &&
                        item.stargazer.getDisplayName().contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val searchBarHeight = 64.dp

    Box(modifier = modifier.fillMaxSize()) {

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = searchBarHeight + 4.dp,
                bottom = 120.dp,
                start = 8.dp,
                end = 8.dp
            )
        ) {
            if (uiState.fetchStatus != FetchState.INITING && uiState.fetchStatus != FetchState.INIT_ERROR) {
                if (filteredList.isNotEmpty()) {
                    items(
                        items = filteredList,
                        key = { item -> item.toStableId() }
                    ) { item ->
                        Box(modifier = Modifier.padding(horizontal = 0.dp, vertical = 4.dp)) {
                            when (item) {
                                is StargazersListItemUiModel.StargazerItem -> {
                                    StargazerListItem(stargazerItem = item)
                                }
                                is StargazersListItemUiModel.SeparatorItem -> {
                                    SeparatorItem(separatorItem = item)
                                }
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .height(searchBarHeight)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 6.dp
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                placeholder = { Text(stringResource(R.string.search)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_search_24dp), contentDescription = stringResource(R.string.search)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(painterResource(R.drawable.ic_cancel_24dp), contentDescription = stringResource(R.string.clear_close))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .imePadding(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { viewModel.refreshStargazers() },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_search_24dp),
                    contentDescription = stringResource(R.string.refresh)
                )
            }
            FloatingActionButton(
                onClick = { context.openUrl("https://github.com/ost-sys") }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_star_24dp),
                    contentDescription = stringResource(R.string.star_repositories)
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
                .imePadding()
        )

        Box(modifier = Modifier.fillMaxSize().padding(top = searchBarHeight + 4.dp)) {
            when (uiState.fetchStatus) {
                FetchState.INITING -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                FetchState.INIT_ERROR, FetchState.REFRESH_ERROR -> {
                    if (uiState.itemsList.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = uiState.noItemText.ifEmpty { stringResource(id = R.string.error_loading_stargazers) },
                                textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.refreshStargazers(true) }) {
                                Text(stringResource(id = R.string.retry))
                            }
                        }
                    }
                    // Если ошибка, но список не пуст, LazyColumn выше его покажет
                }
                FetchState.INITED, FetchState.REFRESHED -> {
                    if (filteredList.isEmpty()) { // Проверяем отфильтрованный список
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) {
                                    stringResource(R.string.no_results_found)
                                } else {
                                    uiState.noItemText.ifEmpty { stringResource(id = R.string.no_stargazers_yet) }
                                },
                                textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                else -> {
                    if (uiState.itemsList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

@Composable
fun SeparatorItem(
    separatorItem: StargazersListItemUiModel.SeparatorItem,
    modifier: Modifier = Modifier
) {
    Text(
        text = separatorItem.indexText,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
    )
}