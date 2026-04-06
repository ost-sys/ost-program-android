package com.ost.application.ui.screen.converters

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.R
import com.ost.application.ui.screen.converters.currency.CurrencyConverterPage
import com.ost.application.ui.screen.converters.currency.CurrencyConverterViewModel
import com.ost.application.ui.screen.converters.timecalc.TimeCalculatorPage
import com.ost.application.ui.screen.converters.timecalc.TimeCalculatorViewModel
import com.ost.application.ui.screen.converters.timezone.TimeZoneConverterPage
import com.ost.application.ui.state.FabSize
import com.ost.application.ui.state.LocalFabController
import kotlinx.coroutines.launch

@ExperimentalMaterial3ExpressiveApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvertersScreen(modifier: Modifier = Modifier) {
    val tabTitles = listOf(
        stringResource(R.string.time_zone),
        stringResource(R.string.time),
        stringResource(R.string.currency)
    )
    val pagerState = rememberPagerState(pageCount = { tabTitles.size })
    val scope = rememberCoroutineScope()

    val fabController = LocalFabController.current

    val timeCalcViewModel: TimeCalculatorViewModel = viewModel()
    val currencyViewModel: CurrencyConverterViewModel = viewModel()

    LaunchedEffect(pagerState.currentPage) {
        when (pagerState.currentPage) {
            0 -> {
                fabController.hideFab()
            }
            1 -> {
                fabController.setFab(
                    icon = R.drawable.ic_calculate_24dp,
                    description = "Calculate",
                    fabSize = FabSize.Small,
                    action = { timeCalcViewModel.calculateTimeDifference() }
                )
            }
            2 -> {
                fabController.hideFab()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { fabController.hideFab() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(
                        selectedTabIndex = pagerState.currentPage,
                        matchContentSize = true
                    ),
                    width = Dp.Unspecified,
                    shape = MaterialTheme.shapes.small
                )
            },
            divider = {}
        ) {
            tabTitles.forEachIndexed { index, title ->
                val isSelected = pagerState.currentPage == index
                Tab(
                    selected = isSelected,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = if (isSelected) 16.sp else 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            beyondViewportPageCount = 2
        ) { page ->
            when (page) {
                0 -> TimeZoneConverterPage()
                1 -> TimeCalculatorPage(viewModel = timeCalcViewModel)
                2 -> CurrencyConverterPage(viewModel = currencyViewModel)
            }
        }
    }
}
