package com.ost.application.ui.activity.main
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
enum class CordPosition {
    START,
    MIDDLE,
    END,
}
@Composable
fun CordItem(
    label: String,
    value: String,
    position: CordPosition,
    modifier: Modifier = Modifier
) {
    val largeCornerRadius = 24.dp
    val smallCornerRadius = 4.dp
    val shape = when (position) {
        CordPosition.START -> RoundedCornerShape(topStart = largeCornerRadius, topEnd = smallCornerRadius, bottomStart = largeCornerRadius, bottomEnd = smallCornerRadius)
        CordPosition.MIDDLE -> RoundedCornerShape(smallCornerRadius)
        CordPosition.END -> RoundedCornerShape(topStart = smallCornerRadius, topEnd = largeCornerRadius, bottomStart = smallCornerRadius, bottomEnd = largeCornerRadius)
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = shape,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp, start = 2.dp, end = 2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}
