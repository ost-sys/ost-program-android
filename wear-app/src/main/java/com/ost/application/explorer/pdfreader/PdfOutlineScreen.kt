package com.ost.application.explorer.pdfreader
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
private val LargeCorner = 24.dp
private val SmallCorner = 4.dp
@Composable
fun PdfOutlineScreen(
    outline: List<OutlineItem>,
    currentPage: Int,
    totalPages: Int,
    onItemClick: (OutlineItem) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.activity.compose.BackHandler { onDismiss() }
    AppScaffold(
        timeText = { TimeText() }
    ) {
        val listState = rememberScalingLazyListState(
            initialCenterItemIndex = if (outline.isEmpty()) {
                (currentPage + 1).coerceAtMost(totalPages)
            } else {
                val active = outline.indexOfLast { it.pageIndex <= currentPage }.coerceAtLeast(0)
                (active + 1).coerceAtMost(outline.size)
            }
        )
        ScreenScaffold(scrollState = listState) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                if (outline.isEmpty()) {
                    PageListScreen(
                        currentPage = currentPage,
                        totalPages = totalPages,
                        listState = listState,
                        onPageClick = { page ->
                            onItemClick(OutlineItem(title = "", pageIndex = page, depth = 0))
                        }
                    )
                } else {
                    OutlineListScreen(
                        outline = outline,
                        currentPage = currentPage,
                        listState = listState,
                        onItemClick = onItemClick
                    )
                }
            }
        }
    }
}
@Composable
private fun OutlineListScreen(
    outline: List<OutlineItem>,
    currentPage: Int,
    listState: androidx.wear.compose.foundation.lazy.ScalingLazyListState,
    onItemClick: (OutlineItem) -> Unit
) {
    val activeIndex = outline.indexOfLast { it.pageIndex <= currentPage }.coerceAtLeast(0)
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        anchorType = ScalingLazyListAnchorType.ItemCenter
    ) {
        item { Spacer(Modifier.height(4.dp)) }
        itemsIndexed(outline) { index, item ->
            val isActive = index == activeIndex
            val isSubItem = item.depth > 0
            val prev = outline.getOrNull(index - 1)
            val next = outline.getOrNull(index + 1)
            val samePrev = prev?.depth == item.depth
            val sameNext = next?.depth == item.depth
            val shape = when {
                !samePrev && !sameNext -> RoundedCornerShape(LargeCorner)
                !samePrev             -> RoundedCornerShape(LargeCorner, LargeCorner, SmallCorner, SmallCorner)
                !sameNext             -> RoundedCornerShape(SmallCorner, SmallCorner, LargeCorner, LargeCorner)
                else                  -> RoundedCornerShape(SmallCorner)
            }
            val textColor = when {
                isActive  -> MaterialTheme.colorScheme.primary
                isSubItem -> MaterialTheme.colorScheme.tertiary
                else      -> MaterialTheme.colorScheme.onSurface
            }
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onItemClick(item) },
                enabled = true,
                label = {
                    Text(
                        text = item.title,
                        fontWeight = if (isActive) FontWeight.Bold
                        else if (isSubItem) FontWeight.Normal
                        else FontWeight.Medium,
                        fontSize = if (isSubItem) 12.sp else 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = textColor
                    )
                },
                secondaryLabel = {
                    Text(
                        text = "Page ${item.pageIndex + 1}",
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                },
                shape = shape,
                colors = ChipDefaults.gradientBackgroundChipColors(
                    startBackgroundColor = MaterialTheme.colorScheme.surfaceContainer
                        .copy(0f).compositeOver(MaterialTheme.colorScheme.surfaceContainer),
                    endBackgroundColor = if (isActive)
                        MaterialTheme.colorScheme.primary.copy(0.35f)
                            .compositeOver(MaterialTheme.colorScheme.surfaceContainer)
                    else
                        MaterialTheme.colorScheme.primary.copy(0.15f)
                            .compositeOver(MaterialTheme.colorScheme.surfaceContainer)
                )
            )
        }
        item { Spacer(Modifier.height(4.dp)) }
    }
}
@Composable
private fun PageListScreen(
    currentPage: Int,
    totalPages: Int,
    listState: androidx.wear.compose.foundation.lazy.ScalingLazyListState,
    onPageClick: (Int) -> Unit
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        anchorType = ScalingLazyListAnchorType.ItemCenter
    ) {
        item { Spacer(Modifier.height(4.dp)) }
        items(totalPages) { index ->
            val isActive = index == currentPage
            val posInGroup = index % 5
            val isFirst = posInGroup == 0
            val isLast = posInGroup == 4 || index == totalPages - 1
            val shape = when {
                isFirst && isLast -> RoundedCornerShape(LargeCorner)
                isFirst           -> RoundedCornerShape(LargeCorner, LargeCorner, SmallCorner, SmallCorner)
                isLast            -> RoundedCornerShape(SmallCorner, SmallCorner, LargeCorner, LargeCorner)
                else              -> RoundedCornerShape(SmallCorner)
            }
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onPageClick(index) },
                enabled = true,
                label = {
                    Text(
                        text = "Page ${index + 1}",
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                },
                shape = shape,
                colors = ChipDefaults.gradientBackgroundChipColors(
                    startBackgroundColor = MaterialTheme.colorScheme.surfaceContainer
                        .copy(0f).compositeOver(MaterialTheme.colorScheme.surfaceContainer),
                    endBackgroundColor = if (isActive)
                        MaterialTheme.colorScheme.primary.copy(0.35f)
                            .compositeOver(MaterialTheme.colorScheme.surfaceContainer)
                    else
                        MaterialTheme.colorScheme.primary.copy(0.15f)
                            .compositeOver(MaterialTheme.colorScheme.surfaceContainer)
                )
            )
        }
        item { Spacer(Modifier.height(4.dp)) }
    }
}
