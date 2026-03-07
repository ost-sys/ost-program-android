package com.ost.application.util

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SectionTitle(
    title: String,
) {
    Card(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(100.dp)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(title,
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}