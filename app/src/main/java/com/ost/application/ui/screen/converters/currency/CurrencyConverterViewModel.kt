package com.ost.application.ui.screen.converters.currency

import android.app.Application
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.ost.application.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.util.Locale

@Stable
data class CurrencyConverterUiState(
    val currencyCodes: List<String> = emptyList(),
    val fromCurrency: String = "USD",
    val toCurrency: String = "EUR",
    val amountInput: String = "1",
    val resultText: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class CurrencyConverterViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CurrencyConverterUiState())
    val uiState: StateFlow<CurrencyConverterUiState> = _uiState.asStateFlow()

    private val requestQueue: RequestQueue = Volley.newRequestQueue(application)
    private var currencyRates: Map<String, Double> = emptyMap()

    init {
        fetchCurrencyData()
    }

    fun setFromCurrency(currency: String) {
        _uiState.update { it.copy(fromCurrency = currency) }
    }

    fun setToCurrency(currency: String) {
        _uiState.update { it.copy(toCurrency = currency) }
    }

    fun setAmount(amount: String) {
        _uiState.update { it.copy(amountInput = amount) }
    }

    private fun fetchCurrencyData() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        val url = "https://currency-rate-exchange-api.onrender.com/all"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                viewModelScope.launch(Dispatchers.Default) {
                    try {
                        val ratesObject = response.getJSONObject("rates").getJSONObject("all")
                        val ratesMap = mutableMapOf<String, Double>()
                        val codesList = mutableListOf<String>()
                        val keys = ratesObject.keys()

                        while (keys.hasNext()) {
                            val code = keys.next()
                            val rate = ratesObject.getDouble(code)
                            ratesMap[code.lowercase(Locale.getDefault())] = rate
                            codesList.add(code.uppercase(Locale.getDefault()))
                        }
                        codesList.sort()
                        currencyRates = ratesMap

                        val defaultFrom = codesList.firstOrNull { it == _uiState.value.fromCurrency } ?: codesList.firstOrNull() ?: ""
                        val defaultTo = codesList.firstOrNull { it == _uiState.value.toCurrency } ?: codesList.getOrNull(1) ?: ""

                        withContext(Dispatchers.Main) {
                            _uiState.update {
                                it.copy(
                                    currencyCodes = codesList,
                                    fromCurrency = defaultFrom,
                                    toCurrency = defaultTo,
                                    isLoading = false,
                                    error = null
                                )
                            }
                        }
                    } catch (e: JSONException) {
                        Log.e("CurrencyConverterVM", "Error parsing JSON", e)
                        withContext(Dispatchers.Main) {
                            _uiState.update { it.copy(isLoading = false, error = getString(R.string.data_analysis_error)) }
                        }
                    }
                }
            },
            { error ->
                Log.e("CurrencyConverterVM", "Volley error", error)
                _uiState.update { it.copy(isLoading = false, error = getString(R.string.connection_error_occurred)) }
            })

        requestQueue.add(request)
    }

    fun convertCurrency() {
        val state = _uiState.value
        if (state.isLoading || state.error != null) return

        val fromCode = state.fromCurrency.lowercase(Locale.getDefault())
        val toCode = state.toCurrency.lowercase(Locale.getDefault())
        val amountString = state.amountInput

        val amount = amountString.toDoubleOrNull() ?: 1.0

        val fromRate = currencyRates[fromCode]
        val toRate = currencyRates[toCode]

        if (fromRate == null || toRate == null || fromRate == 0.0) {
            _uiState.update { it.copy(resultText = getString(R.string.conversion_rate_error)) }
            return
        }

        val convertedAmount = amount * toRate / fromRate

        val result = String.format(
            Locale.getDefault(),
            "%.2f %s = %.4f %s",
            amount, state.fromCurrency,
            convertedAmount, state.toCurrency
        )
        _uiState.update { it.copy(resultText = result) }
    }

    private fun getString(resId: Int): String {
        return getApplication<Application>().getString(resId)
    }
}