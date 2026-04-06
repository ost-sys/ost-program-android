package com.ost.application.util

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.ost.application.R

enum class CardPosition {
    TOP,
    MIDDLE,
    BOTTOM,
    SINGLE
}
@Composable
fun CardListItem(
    title: String,
    summary: String?,
    icon: Int?,
    status: Boolean,
    position: CardPosition = CardPosition.SINGLE,
    onClick: (() -> Unit)?
) {
    val largeCornerRadius = 24.dp
    val smallCornerRadius = 4.dp

    val shape = when (position) {
        CardPosition.TOP -> RoundedCornerShape(topStart = largeCornerRadius, topEnd = largeCornerRadius, bottomStart = smallCornerRadius, bottomEnd = smallCornerRadius)
        CardPosition.MIDDLE -> RoundedCornerShape(smallCornerRadius)
        CardPosition.BOTTOM -> RoundedCornerShape(topStart = smallCornerRadius, topEnd = smallCornerRadius, bottomStart = largeCornerRadius, bottomEnd = largeCornerRadius)
        CardPosition.SINGLE -> RoundedCornerShape(largeCornerRadius)
    }
    Chip(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick ?: {},
        enabled = status,
        label = { Text(text = title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        secondaryLabel = {
            if (summary != null) {
                Text(text = summary, overflow = TextOverflow.Ellipsis)
            }
        },
        icon = {
            icon?.let {
                Icon(
                    painter = painterResource(id = it),
                    contentDescription = title,
                    modifier =
                        Modifier
                            .size(ChipDefaults.IconSize)
                            .wrapContentSize(align = Alignment.Center),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        shape = shape,
        colors = ChipDefaults.gradientBackgroundChipColors(
            startBackgroundColor = MaterialTheme.colorScheme.surfaceContainer
                .copy(0f).compositeOver(MaterialTheme.colorScheme.surfaceContainer),
            endBackgroundColor = MaterialTheme.colorScheme.primary
                .copy(0.25f).compositeOver(MaterialTheme.colorScheme.surfaceContainer)
        )
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    MaterialTheme {
        CardListItem("Test", "Test", R.drawable.ic_watch_24dp, true) {  }
    }
}