package com.ost.application.appmanager

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwipeToReveal
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.rememberRevealState
import coil.compose.rememberAsyncImagePainter
import com.ost.application.R

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun AppManagerScreen(
    viewModel: AppViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadApps(context.packageManager)
    }

    AppScaffold(
        timeText = { TimeText() }
    ) {
        val listState = rememberScalingLazyListState()

        ScreenScaffold(
            scrollState = listState
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(onClick = { viewModel.loadApps(context.packageManager) }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_refresh_24dp),
                                    contentDescription = "Refresh list"
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { viewModel.toggleSystemApps() }) {
                                val iconRes = if (uiState.showSystemApps)
                                    R.drawable.ic_visibility_24dp
                                else
                                    R.drawable.ic_visibility_off_24dp
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = "Toggle system apps"
                                )
                            }
                        }
                    }

                    val filteredApps = if (uiState.showSystemApps) {
                        uiState.apps
                    } else {
                        uiState.apps.filter { !it.isSystemApp }
                    }

                    items(filteredApps, key = { it.packageName }) { app ->
                        rememberCoroutineScope()
                        val revealState = rememberRevealState()

                        SwipeToReveal(
                            revealState = revealState,
                            secondaryAction = {
                                SecondaryActionButton(
                                    onClick = {
                                        viewModel.openAppInfoScreen(context, app.packageName)
                                    },
                                    icon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_info_24dp),
                                            contentDescription = "App info"
                                        )
                                    }
                                )
                            },
                            primaryAction = {
                                PrimaryActionButton(
                                onClick = {
                                    if (!app.isSystemApp) {
                                        viewModel.requestAppDeletion(context, app.packageName)
                                    } else {
                                        Toast.makeText(context, "You can't delete system app",
                                            Toast.LENGTH_SHORT).show()
                                    }
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_delete_24dp),
                                        contentDescription = "Delete app"
                                    )
                                },
                                text = { Text(stringResource(R.string.delete)) }
                            )

                            },
                            content = {
                                Chip(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        // TODO: Открыть наш детальный экран
                                    },
                                    icon = {
                                        Image(
                                            painter = rememberAsyncImagePainter(model = app.icon),
                                            contentDescription = "${app.name} icon",
                                            modifier = Modifier.size(ChipDefaults.IconSize)
                                        )
                                    },
                                    label = {
                                        Text(
                                            app.name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    secondaryLabel = {
                                        Text(
                                            app.packageName,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = ChipDefaults.secondaryChipColors()
                                )
                            },
                            onSwipePrimaryAction = {
                                if (!app.isSystemApp) {
                                    viewModel.requestAppDeletion(context, app.packageName)
                                } else {
                                    Toast.makeText(context, "You can't delete system app",
                                        Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}