package com.ost.application.ui.screen.stargazers

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import com.ost.application.R
import com.ost.application.data.model.GitHubRepo
import com.ost.application.ui.component.ExpressiveShapeBackground
import com.ost.application.ui.component.ExpressiveShapeType
import com.ost.application.ui.state.FabSize
import com.ost.application.ui.state.LocalFabController
import com.ost.application.util.CardPosition
import com.ost.application.util.CustomCardItem

@Composable
fun StargazersScreen(
    viewModel: StargazersViewModel
) {
    val token by viewModel.userToken.collectAsState()
    val selectedRepo by viewModel.selectedRepo.collectAsState()
    val repos by viewModel.repos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    BackHandler(enabled = selectedRepo != null) {
        viewModel.clearSelection()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = when {
                selectedRepo != null -> "list"
                token.isNotEmpty() -> "repos"
                else -> "login"
            },
            transitionSpec = {
                (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300)))
                    .togetherWith(fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 1.05f, animationSpec = tween(300)))
            },
            label = "ScreenTransition"
        ) { state ->
            when (state) {
                "login" -> LoginScreen(
                    isLoading = isLoading,
                    error = error,
                    onLoginClick = { viewModel.login(it) }
                )
                "repos" -> RepoListContent(
                    repos = repos,
                    isLoading = isLoading,
                    onRefresh = { viewModel.refreshRepositories() },
                    onRepoClick = { viewModel.selectRepo(it) }
                )
                "list" -> StargazersListContent(
                    viewModel = viewModel,
                    onRefresh = { viewModel.refreshStargazers() },
                    onUserClick = { login ->
                        val intent = Intent(context, ProfileActivity::class.java).apply {
                            putExtra("EXTRA_USERNAME", login)
                            putExtra("EXTRA_TOKEN", token)
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    isLoading: Boolean,
    error: String?,
    onLoginClick: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 5.dp)
        ) {
            ExpressiveShapeBackground(
                iconSize = 120.dp,
                color = MaterialTheme.colorScheme.primaryContainer,
                forcedShape = ExpressiveShapeType.CLOVER_8
            )

            Image(
                painter = painterResource(R.drawable.ic_star_24dp),
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))


        Text(
            text = stringResource(R.string.github_login),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.enter_your_personal_access_token_to_view_your_repositories),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(R.string.token)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            @OptIn(ExperimentalMaterial3ExpressiveApi::class)
            CircularWavyProgressIndicator()
        } else {
            Button(
                onClick = { onLoginClick(text) },
                modifier = Modifier.fillMaxWidth(),
                enabled = text.isNotBlank()
            ) {
                Text("Load Repositories")
            }
            Spacer(modifier = Modifier.height(8.dp))
            FilledTonalButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW,
                        "https://github.com/settings/tokens".toUri()))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Get Token")
            }
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RepoListContent(
    repos: List<GitHubRepo>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onRepoClick: (GitHubRepo) -> Unit
) {
    val state = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = onRefresh,
        state = state,
        indicator = {
            val progress by animateFloatAsState(targetValue = if (isLoading) 1f else state.distanceFraction, label = "")
            Box(modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)) {
                if (isLoading || state.distanceFraction > 0f) {
                    LoadingIndicator(progress = { progress }, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    ) {
        if (repos.isEmpty() && !isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No repositories found.")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(repos) { index, repo ->
                    val position = when {
                        repos.size == 1 -> CardPosition.SINGLE
                        index == 0 -> CardPosition.TOP
                        index == repos.lastIndex -> CardPosition.BOTTOM
                        else -> CardPosition.MIDDLE
                    }

                    CustomCardItem(
                        title = repo.name,
                        summary = repo.htmlUrl,
                        icon = R.drawable.ic_build_24dp,
                        position = position,
                        status = repo.starsCount > 0,
                        onClick = { onRepoClick(repo) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StargazersListContent(
    viewModel: StargazersViewModel,
    onRefresh: () -> Unit,
    onUserClick: (String) -> Unit
) {
    val users by viewModel.users.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedRepo by viewModel.selectedRepo.collectAsState()
    val listState = rememberLazyListState()
    val pullState = rememberPullToRefreshState()
    val context = LocalContext.current
    val fabController = LocalFabController.current

    LaunchedEffect(selectedRepo) {
        if (selectedRepo != null) {
            fabController.setFab(
                icon = R.drawable.ic_star_24dp,
                description = "Star",
                fabSize = FabSize.Small,
                action = {
                    val intent = Intent(Intent.ACTION_VIEW, selectedRepo!!.htmlUrl.toUri())
                    context.startActivity(intent)
                }
            )
        } else {
            fabController.hideFab()
        }
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            totalItems > 0 && (totalItems - lastVisibleItemIndex < 10)
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadNextPage()
        }
    }

    PullToRefreshBox(
        isRefreshing = isLoading && users.isEmpty(),
        onRefresh = onRefresh,
        state = pullState,
        indicator = {
            val progress by animateFloatAsState(targetValue = if (isLoading && users.isEmpty()) 1f else pullState.distanceFraction, label = "")
            Box(modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)) {
                if ((isLoading && users.isEmpty()) || pullState.distanceFraction > 0f) {
                    LoadingIndicator(progress = { progress }, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(users) { index, user ->
                val position = when {
                    users.size == 1 -> CardPosition.SINGLE
                    index == 0 -> CardPosition.TOP
                    index == users.lastIndex -> CardPosition.BOTTOM
                    else -> CardPosition.MIDDLE
                }

                CustomCardItem(
                    title = user.name ?: user.login,
                    summary = user.htmlUrl,
                    iconPainter = rememberAsyncImagePainter(model = user.avatarUrl),
                    position = position,
                    onClick = { onUserClick(user.login) }
                )
            }

            item {
                if (isLoading && users.isNotEmpty()) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp), contentAlignment = Alignment.Center) {
                        CircularWavyProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}