package com.ost.application.ui.screen.converters.currency

import androidx.compose.runtime.Stable

enum class NetworkStatus {
    LOADING,
    CONNECTED,
    OFFLINE,
    ERROR
}
@Stable
data class TargetCurrencyInfo(
    val code: String,
    val result: String? = null
)

@Stable
data class CurrencyConverterUiState(
    val networkStatus: NetworkStatus = NetworkStatus.LOADING,
    val allCurrencyCodes: List<String> = emptyList(),
    val baseCurrency: String = "USD",
    val amountInput: String = "1",
    val targetCurrencies: List<TargetCurrencyInfo> = listOf(TargetCurrencyInfo(code = "EUR")),
    val isAddingCurrency: Boolean = false,
    val editingCurrencyCode: String? = null,
    val availableTargetCodes: List<String> = emptyList(),
    val errorMessage: String? = null,
    val showBaseCurrencyWarning: Boolean = false
)