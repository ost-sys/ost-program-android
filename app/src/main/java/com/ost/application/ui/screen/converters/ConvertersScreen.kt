package com.ost.application.ui.screen.converters

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.ost.application.R
import com.ost.application.ui.screen.converters.currency.CurrencyConverterPage
import com.ost.application.ui.screen.converters.timecalc.TimeCalculatorPage
import com.ost.application.ui.screen.converters.timezone.TimeZoneConverterPage
import com.ost.application.ui.theme.OSTToolsTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ConvertersScreen(modifier: Modifier = Modifier) {

    val tabTitles = listOf(
        stringResource(R.string.time_zone),
        stringResource(R.string.time),
        stringResource(R.string.currency)
    )
    val pagerState = rememberPagerState(pageCount = { tabTitles.size })
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {

        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(title) }
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            when (page) {
                0 -> TimeZoneConverterPage()
                1 -> TimeCalculatorPage()
                2 -> CurrencyConverterPage()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConvertersScreenPreview() {
    OSTToolsTheme {
        ConvertersScreen()
    }
}