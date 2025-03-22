package com.ost.application.util

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

@Composable
fun CardItem(title: String, summary: String?, icon: Int?, onClick: (() -> Unit)?) {
    Card(
        onClick = onClick ?: {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp, 0.dp),
        backgroundPainter = CardDefaults.cardBackgroundPainter(Color.DarkGray),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let { painterResource(it) }?.let {
                Icon(
                    painter = it,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(title)
                summary?.let { Text(it, style = MaterialTheme.typography.body2) }
            }
        }
    }
}

