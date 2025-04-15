package com.ost.application.util

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.ost.application.MainApp
import com.ost.application.R
import com.ost.application.UpdateCheckResult

@Composable
fun CardListItem(title: String, summary: String?, icon: Int?, status: Boolean, onClick: (() -> Unit)?) {
    Chip(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick ?: {},
        enabled = status,
        label = { Text(text = title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        secondaryLabel = {
            if (summary != null) {
                Text(text = summary, overflow = TextOverflow.Ellipsis)
            }
        },
        icon = {
            icon?.let {
                Icon(
                    painter = painterResource(id = it),
                    contentDescription = "airplane",
                    modifier =
                        Modifier
                            .size(ChipDefaults.IconSize)
                            .wrapContentSize(align = Alignment.Center),
                )
            }
        },
        colors = ChipDefaults.secondaryChipColors()
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    MaterialTheme {
        CardListItem("Test", "Test", R.drawable.ic_watch_24dp, true) { null }
    }
}