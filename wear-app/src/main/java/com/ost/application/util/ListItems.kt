package com.ost.application.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.ost.application.component.ExpressiveShapeBackground

@Composable
fun ListItems(text: String?, icon: Int?) {
    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null) {
                Box(
                    modifier = Modifier.size(60.dp).padding(bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ExpressiveShapeBackground(
                        iconSize = 48.dp,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    )
                    Image(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)
                    )
                }
            }
            if (text != null) {
                Text(
                    text,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp, 0.dp).align(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}