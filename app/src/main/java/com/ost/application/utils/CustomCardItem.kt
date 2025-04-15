package com.ost.application.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ost.application.R
import com.ost.application.ui.theme.OSTToolsTheme

@Composable
fun CustomCardItem(icon: Int?, title: String, summary: String?, status: Boolean, iconPainter: Painter?, onClick: (() -> Unit)?) {
    ElevatedCard(
        onClick = onClick ?: {},
        modifier = Modifier.fillMaxWidth().padding(top = 5.dp, start = 2.5.dp, end = 5.dp, bottom = 2.5.dp),
        enabled = status,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(Modifier.padding(18.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

            icon?.let {
                if (true) {
                    Icon(
                        painterResource(it),
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(26.dp)
                    )
                } else if (iconPainter != null) {
                    Image(
                        painter = iconPainter,
                        contentDescription = title,
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(Modifier.size(12.dp))
            }

            Column(Modifier.fillMaxWidth())
            {
                Text(title, style = MaterialTheme.typography.titleMedium)
                summary?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

        }
    }
}

@Preview(
    showBackground = false, name = "CardItem Preview", device = "spec:width=411dp,height=891dp",
    wallpaper = androidx.compose.ui.tooling.preview.Wallpapers.BLUE_DOMINATED_EXAMPLE,
)
@Composable
fun CardPreview() {
    OSTToolsTheme {
        CustomCardItem(R.drawable.ic_apps_24dp, "Test", "Hello World!", true, null) { null }
    }
}