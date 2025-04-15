package com.ost.application.ui.screen.stargazers

import android.content.Intent
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ost.application.R
import com.ost.application.ui.screen.stargazers.profile.ProfileActivity
import com.ost.application.utils.openUrl
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StargazerListItem(
    stargazerItem: StargazersListItemUiModel.StargazerItem,
    modifier: Modifier = Modifier
) {
    val stargazer = stargazerItem.stargazer
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    val blogUrl = stargazer.blog
    val isBlogUrlValid = !blogUrl.isNullOrBlank() &&
            (blogUrl.startsWith("https://", ignoreCase = true) || blogUrl.startsWith("http://", ignoreCase = true))

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    Log.d("StargazerListItem", "Swipe LTR -> GitHub: ${stargazer.html_url}")
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    context.openUrl(stargazer.html_url)
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    return@rememberSwipeToDismissBoxState false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    if (isBlogUrlValid) {
                        Log.d("StargazerListItem", "Swipe RTL -> Blog: $blogUrl")
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        context.openUrl(blogUrl!!)
                    } else {
                        Log.d("StargazerListItem", "Swipe RTL -> No valid blog URL for ${stargazer.login}")
                    }
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    return@rememberSwipeToDismissBoxState false
                }
                SwipeToDismissBoxValue.Settled -> {
                    return@rememberSwipeToDismissBoxState true
                }
            }
        },
        positionalThreshold = { distance -> distance * 0.25f }
    )

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            coroutineScope.launch {
                dismissState.reset()
            }
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromEndToStart = isBlogUrlValid,
        enableDismissFromStartToEnd = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.tertiaryContainer
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                }, label = "SwipeBackgroundColor"
            )
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                SwipeToDismissBoxValue.Settled -> Alignment.Center
            }
            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> painterResource(id = R.drawable.about_page_github)
                SwipeToDismissBoxValue.EndToStart -> painterResource(id = R.drawable.ic_internet_24dp)
                SwipeToDismissBoxValue.Settled -> null
            }
            val iconTint = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.onPrimaryContainer
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.onTertiaryContainer
                SwipeToDismissBoxValue.Settled -> Color.Transparent
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(color, shape = RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                icon?.let {
                    if (it is ImageVector) {
                        Icon(
                            imageVector = it,
                            contentDescription = "Swipe Action",
                            tint = iconTint
                        )
                    } else if (it is androidx.compose.ui.graphics.painter.Painter) {
                        Icon(
                            painter = it,
                            contentDescription = "Swipe Action",
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    ) {
        ElevatedCard(
            onClick = {
                val intent = Intent(context, ProfileActivity::class.java).apply {
                    putExtra(ProfileActivity.KEY_STARGAZER, stargazer)
                }
                context.startActivity(intent)
            },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
        ) {
            ListItem(
                headlineContent = { Text(stargazer.getDisplayName()) },
                supportingContent = {
                    if (stargazer.html_url.isNotEmpty()) {
                        Text(
                            text = stargazer.html_url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                leadingContent = {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(stargazer.avatar_url)
                            .crossfade(true)
                            .placeholder(R.drawable.ic_account_circle_24dp)
                            .error(R.drawable.ic_account_circle_24dp)
                            .build(),
                        contentDescription = "${stargazer.getDisplayName()} avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )
        }
    }
}