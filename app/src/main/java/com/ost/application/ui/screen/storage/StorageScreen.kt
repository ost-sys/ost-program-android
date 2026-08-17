package com.ost.application.ui.screen.storage
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.LocalBottomSpacing
import com.ost.application.R
import com.ost.application.util.AdaptiveSquareCard
import java.util.Locale
private enum class GridCardKind { USED, FREE, TOTAL, STATE }
private fun GridCardKind.narrowGridShape(bigRadius: Dp, smallRadius: Dp): RoundedCornerShape =
    when (this) {
        GridCardKind.USED -> RoundedCornerShape(
            topStart = bigRadius, topEnd = smallRadius,
            bottomStart = smallRadius, bottomEnd = smallRadius
        )
        GridCardKind.FREE -> RoundedCornerShape(
            topStart = smallRadius, topEnd = bigRadius,
            bottomStart = smallRadius, bottomEnd = smallRadius
        )
        GridCardKind.TOTAL -> RoundedCornerShape(
            topStart = smallRadius, topEnd = smallRadius,
            bottomStart = bigRadius, bottomEnd = smallRadius
        )
        GridCardKind.STATE -> RoundedCornerShape(
            topStart = smallRadius, topEnd = smallRadius,
            bottomStart = smallRadius, bottomEnd = bigRadius
        )
    }
private fun formatBytes(bytes: Long): String {
    val gb = bytes.toDouble() / (1024 * 1024 * 1024)
    return if (gb >= 1.0) {
        String.format(Locale.getDefault(), "%.2f GB", gb)
    } else {
        val mb = bytes.toDouble() / (1024 * 1024)
        String.format(Locale.getDefault(), "%.1f MB", mb)
    }
}
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    modifier: Modifier = Modifier,
    viewModel: StorageViewModel = viewModel()
) {
    val storageInfo by viewModel.storageInfo.collectAsStateWithLifecycle()
    val bottomSpacing = LocalBottomSpacing.current
    val animatedProgress by animateFloatAsState(
        targetValue = (storageInfo.usedPercentage / 100f).coerceIn(0f, 1f),
        label = "storageProgress"
    )
    val progressColor = when {
        storageInfo.usedPercentage > 90 -> MaterialTheme.colorScheme.error
        storageInfo.usedPercentage > 75 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val usedTitle = stringResource(R.string.used_storage)
    val freeTitle = stringResource(R.string.free_storage)
    val totalTitle = stringResource(R.string.total_storage)
    val stateTitle = stringResource(R.string.storage_state)
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 16.dp,
            bottom = bottomSpacing + 16.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(160.dp)
                    ) {
                        CircularWavyProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${storageInfo.usedPercentage}%",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = usedTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${formatBytes(storageInfo.usedBytes)} / ${formatBytes(storageInfo.totalBytes)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        item {
            AdaptiveSquareCard(
                title = usedTitle,
                summary = formatBytes(storageInfo.usedBytes),
                icon = com.ost.application.core.R.drawable.ic_storage_24dp,
                status = true,
                shape = GridCardKind.USED.narrowGridShape(24.dp, 4.dp)
            )
        }
        item {
            AdaptiveSquareCard(
                title = freeTitle,
                summary = formatBytes(storageInfo.freeBytes),
                icon = com.ost.application.core.R.drawable.ic_storage_24dp,
                status = true,
                shape = GridCardKind.FREE.narrowGridShape(24.dp, 4.dp)
            )
        }
        item {
            AdaptiveSquareCard(
                title = totalTitle,
                summary = formatBytes(storageInfo.totalBytes),
                icon = com.ost.application.core.R.drawable.ic_storage_24dp,
                status = true,
                shape = GridCardKind.TOTAL.narrowGridShape(24.dp, 4.dp)
            )
        }
        item {
            AdaptiveSquareCard(
                title = stateTitle,
                summary = storageInfo.state,
                icon = com.ost.application.core.R.drawable.ic_storage_24dp,
                status = true,
                shape = GridCardKind.STATE.narrowGridShape(24.dp, 4.dp)
            )
        }
    }
}
