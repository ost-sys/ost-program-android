package com.ost.application.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

@Composable
fun ListItems(text: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (text != null) {
                Text(
                    text,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(16.dp, 0.dp).align(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}