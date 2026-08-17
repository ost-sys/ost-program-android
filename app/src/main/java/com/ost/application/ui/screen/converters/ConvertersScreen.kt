package com.ost.application.ui.screen.converters
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.R
import com.ost.application.ui.screen.converters.currency.CurrencyConverterPage
import com.ost.application.ui.screen.converters.currency.CurrencyConverterViewModel
import com.ost.application.ui.screen.converters.timecalc.TimeCalculatorPage
import com.ost.application.ui.screen.converters.timezone.TimeZoneConverterPage
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
    val currencyViewModel: CurrencyConverterViewModel = viewModel()
    LaunchedEffect(pagerState.currentPage) {
        fabController.hideFab()
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
                    height = 4.dp,
                    shape = CircleShape
                )
            },
            divider = {}
        ) {
            tabTitles.forEachIndexed { index, title ->
                val isSelected = pagerState.currentPage == index
                val textScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = tween(durationMillis = 300),
                    label = "tab_scale_animation"
                )
                Tab(
                    selected = isSelected,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.graphicsLayer {
                                scaleX = textScale
                                scaleY = textScale
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                1 -> TimeCalculatorPage()
                2 -> CurrencyConverterPage(viewModel = currencyViewModel)
            }
        }
    }
}
