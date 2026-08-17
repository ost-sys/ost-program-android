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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ost.application.R
import com.ost.application.ui.components.ExpressiveShapeBackground
import com.ost.application.ui.components.ExpressiveShapeType
enum class CardPosition {
    TOP,
    MIDDLE,
    BOTTOM,
    SINGLE
}
@Composable
fun BetaBadge(
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.tertiaryContainer,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onTertiaryContainer
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
    ) {
        Text(
            text = "BETA",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun CustomCardItem(
    title: String,
    status: Boolean = true,
    icon: Int? = null,
    iconPainter: Painter? = null,
    iconVector: ImageVector? = null,
    smallCardIcon: ImageVector? = null,
    smallCardText: String? = null,
    hasBetaBadge: Boolean = false,
    summary: String? = null,
    position: CardPosition = CardPosition.SINGLE,
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
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
                val hasIcon = icon != null || iconPainter != null || iconVector != null
                if (hasIcon) {
                    CardIcon(icon = icon, iconPainter = iconPainter, iconVector = iconVector, status = status)
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (hasBetaBadge) {
                            Spacer(modifier = Modifier.width(6.dp))
                            BetaBadge()
                        }
                    }
                    if (!summary.isNullOrEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (smallCardText != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (smallCardIcon != null) {
                                Icon(
                                    modifier = Modifier.size(12.dp),
                                    imageVector = smallCardIcon,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                            Text(
                                text = smallCardText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                fontFamily = FontFamily(Font(R.font.google_sans_flex)),
                                maxLines = 1
                            )
                        }
                    }
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
    iconVector: ImageVector? = null,
    summary: String? = null,
    status: Boolean = true,
    shape: Shape = MaterialTheme.shapes.large,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .aspectRatio(1f, matchHeightConstraintsFirst = false),
        shape = shape,
        enabled = status,
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
                    val hasIcon = icon != null || iconPainter != null || iconVector != null
                    if (hasIcon) {
                        CardIcon(icon = icon, iconPainter = iconPainter, iconVector = iconVector, status = status)
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
                color = if (status) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
@Composable
private fun CardIcon(
    icon: Int? = null,
    iconPainter: Painter? = null,
    iconVector: ImageVector? = null,
    status: Boolean = true,
    usePrimary: Boolean = true
) {
    val bgColor = if (usePrimary) {
        if (icon != null || iconVector != null) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.primaryContainer
    } else {
        if (status) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.errorContainer
    }
    val forcedShape = if (status) ExpressiveShapeType.CLOVER_8 else ExpressiveShapeType.SQUARE
    Box(contentAlignment = Alignment.Center) {
        ExpressiveShapeBackground(iconSize = 48.dp, color = bgColor, forcedShape = forcedShape)
        when {
            icon != null -> Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = if (usePrimary) MaterialTheme.colorScheme.onPrimary
                else if (status) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            iconPainter != null -> Image(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.size(24.dp).clip(CircleShape),
            )
            iconVector != null -> Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = if (usePrimary) MaterialTheme.colorScheme.onPrimary
                else if (status) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp).clip(CircleShape),
            )
        }
        Spacer(Modifier.size(16.dp))
    }
}
@Preview
@Composable
fun CardPreview() {
    Column {
        CustomCardItem(
            title = "Full",
            status = true,
            summary = "Summary text",
            iconVector = Icons.Rounded.Book,
            smallCardIcon = Icons.Rounded.Star,
            smallCardText = "100",
            position = CardPosition.TOP
        )
        CustomCardItem(
            title = "Full without summary",
            status = true,
            iconVector = Icons.Rounded.Book,
            smallCardIcon = Icons.Rounded.Star,
            smallCardText = "100",
            position = CardPosition.MIDDLE
        )
        CustomCardItem(
            title = "Full without icon",
            status = true,
            summary = "Summary text",
            smallCardIcon = Icons.Rounded.Star,
            smallCardText = "100",
            position = CardPosition.MIDDLE
        )
        CustomCardItem(
            title = "Full without icon and summary",
            status = true,
            smallCardIcon = Icons.Rounded.Star,
            smallCardText = "100",
            position = CardPosition.MIDDLE
        )
        CustomCardItem(
            title = "Full without summary and small chip",
            status = true,
            iconVector = Icons.Rounded.Book,
            position = CardPosition.MIDDLE
        )
        CustomCardItem(
            title = "Full without summary, icon and small chip",
            status = true,
            position = CardPosition.MIDDLE
        )
        CustomCardItem(
            title = "Full without small chip and icon",
            status = true,
            summary = "Summary text",
            position = CardPosition.MIDDLE
        )
        CustomCardItem(
            title = "Inactive Icon",
            status = false,
            iconVector = Icons.Rounded.Close,
            position = CardPosition.BOTTOM
        )
    }
}
@Composable
fun CustomRadioItem(
    title: String,
    selected: Boolean,
    position: CardPosition = CardPosition.SINGLE,
    onClick: () -> Unit
) {
    val largeCornerRadius = 24.dp
    val smallCornerRadius = 4.dp
    val shape = when (position) {
        CardPosition.TOP -> RoundedCornerShape(topStart = largeCornerRadius, topEnd = largeCornerRadius, bottomStart = smallCornerRadius, bottomEnd = smallCornerRadius)
        CardPosition.MIDDLE -> RoundedCornerShape(smallCornerRadius)
        CardPosition.BOTTOM -> RoundedCornerShape(topStart = smallCornerRadius, topEnd = smallCornerRadius, bottomStart = largeCornerRadius, bottomEnd = largeCornerRadius)
        CardPosition.SINGLE -> RoundedCornerShape(largeCornerRadius)
    }
    val containerColor = if (selected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceContainerHigh
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(vertical = if (position == CardPosition.MIDDLE) 1.dp else 2.dp),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            RadioButton(
                selected = selected,
                onClick = null
            )
        }
    }
}
