package com.ost.application.util // Или другое подходящее место

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Общий модификатор для обработки скролла и фокуса
fun Modifier.wearListInteraction(
    listState: ScalingLazyListState,
    focusRequester: FocusRequester,
    coroutineScope: CoroutineScope
): Modifier = this
    .focusRequester(focusRequester)
    .onRotaryScrollEvent {
        coroutineScope.launch {
            listState.scrollBy(it.verticalScrollPixels)
        }
        true
    }
    // Можно добавить .pointerInput здесь если нужен скролл пальцем как альтернатива
    .focusable()

// Универсальный экран для отображения списка информации
@Composable
fun InfoListScreenContent(
    listState: ScalingLazyListState,
    screenTitle: String?,
    items: List<ListItem>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(start = 8.dp, end = 8.dp) // Дефолтные паддинги
) {
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    ScalingLazyColumn(
        modifier = modifier
            .fillMaxSize()
            .wearListInteraction(listState, focusRequester, coroutineScope),
        state = listState,
        contentPadding = contentPadding,
        anchorType = ScalingLazyListAnchorType.ItemCenter // Обычно лучше для списков информации
    ) {
        // Отображаем заголовок, если он есть
        screenTitle?.let { title ->
            item {
                ListItems(
                    text = title,
                )
            }
        }

        // Отображаем основной список элементов
        items(items.size, key = { index -> items[index].title /* Можно улучшить ключ */ }) { index ->
            val item = items[index]
            CardListItem(
                title = item.title,
                summary = item.summary,
                icon = item.icon,
                status = item.status,
                // Предоставляем пустую лямбду, если onClick не задан
                onClick = item.onClick ?: {}
            )
        }
    }
}