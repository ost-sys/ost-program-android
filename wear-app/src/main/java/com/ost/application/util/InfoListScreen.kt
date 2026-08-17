package com.ost.application.util
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.ost.application.component.ExpressiveShapeType
@Composable
fun Modifier.wearListInteraction(
    listState: ScalingLazyListState,
    focusRequester: FocusRequester
): Modifier {
    val view = LocalView.current
    return this
        .focusRequester(focusRequester)
        .onRotaryScrollEvent { event ->
            val delta = event.verticalScrollPixels
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            listState.dispatchRawDelta(delta)
            true
        }
        .focusable()
}
@Composable
fun InfoListScreenContent(
    listState: ScalingLazyListState,
    screenTitle: String?,
    icon: Int?,
    items: List<ListItem>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(start = 8.dp, end = 8.dp),
    headerShape: ExpressiveShapeType = ExpressiveShapeType.SQUARE,
    headerRotationDegrees: Float = 0f,
    headerBackgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    headerIconTint: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    ScalingLazyColumn(
        modifier = modifier
            .fillMaxSize()
            .wearListInteraction(listState, focusRequester),
        state = listState,
        contentPadding = contentPadding,
        anchorType = ScalingLazyListAnchorType.ItemCenter
    ) {
        screenTitle?.let { title ->
            item {
                ListHeader {
                    Text(title)
                }
            }
        }
        items(items.size, key = { index -> items[index].title }) { index ->
            val item = items[index]
            CardListItem(
                title = item.title,
                summary = item.summary,
                icon = item.icon,
                status = item.status,
                position = item.position,
                onClick = item.onClick ?: {}
            )
        }
    }
}
