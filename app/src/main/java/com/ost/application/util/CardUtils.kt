package com.ost.application.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ost.application.R
import com.ost.application.ui.component.ExpressiveShapeBackground
import com.ost.application.ui.component.ExpressiveShapeType

enum class CardPosition {
    TOP,
    MIDDLE,
    BOTTOM,
    SINGLE
}

@Composable
fun CustomCardItem(
    title: String,
    status: Boolean = true,
    icon: Int? = null,
    iconPainter: Painter? = null,
    summary: String? = null,
    position: CardPosition = CardPosition.SINGLE,
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    onClick: (() -> Unit)? = null
) {
    val largeCornerRadius = 24.dp
    val smallCornerRadius = 4.dp

    val shape = when (position) {
        CardPosition.TOP -> RoundedCornerShape(topStart = largeCornerRadius, topEnd = largeCornerRadius, bottomStart = smallCornerRadius, bottomEnd = smallCornerRadius)
        CardPosition.MIDDLE -> RoundedCornerShape(smallCornerRadius)
        CardPosition.BOTTOM -> RoundedCornerShape(topStart = smallCornerRadius, topEnd = smallCornerRadius, bottomStart = largeCornerRadius, bottomEnd = largeCornerRadius)
        CardPosition.SINGLE -> RoundedCornerShape(largeCornerRadius)
    }

    Card(
        onClick = onClick ?: {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(vertical = if (position == CardPosition.MIDDLE) 1.dp else 2.dp),
        enabled = status,
        shape = shape,
        colors = colors
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    ExpressiveShapeBackground(
                        iconSize = 48.dp,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        forcedShape = ExpressiveShapeType.CLOVER_8
                    )
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.size(16.dp))
                } else if (iconPainter != null) {
                    ExpressiveShapeBackground(
                        iconSize = 48.dp,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        forcedShape = ExpressiveShapeType.CLOVER_8
                    )
                    Image(
                        painter = iconPainter,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.size(16.dp))
                }
            }

            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!summary.isNullOrEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun AdaptiveSquareCard(
    title: String,
    modifier: Modifier = Modifier,
    icon: Int? = null,
    iconPainter: Painter? = null,
    summary: String? = null,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.large,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .aspectRatio(1f, matchHeightConstraintsFirst = false),
        shape = shape,
        enabled = enabled,
        onClick = onClick ?: {},
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        ExpressiveShapeBackground(
                            iconSize = 48.dp,
                            color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                            forcedShape = ExpressiveShapeType.CLOVER_8
                        )
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.size(16.dp))
                    } else if (iconPainter != null) {
                        ExpressiveShapeBackground(
                            iconSize = 48.dp,
                            color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                            forcedShape = ExpressiveShapeType.CLOVER_8
                        )
                        Image(
                            painter = iconPainter,
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.size(16.dp))
                    }
                }

                if (!summary.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(wallpaper = androidx.compose.ui.tooling.preview.Wallpapers.GREEN_DOMINATED_EXAMPLE)
@Composable
fun CardPreview() {
    CustomCardItem("Looooooooooooong text", status = true, summary = "yeahm text here", icon =
        R.drawable.ic_info_24dp)
}