package com.ost.application.ui.screen.converters.currency

import android.app.Application
import android.content.Context
import android.util.Log
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
import org.json.JSONObject
import java.util.Locale

class CurrencyConverterViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CurrencyConverterUiState())
    val uiState: StateFlow<CurrencyConverterUiState> = _uiState.asStateFlow()

    private val requestQueue: RequestQueue = Volley.newRequestQueue(application)
    private var currencyRates: Map<String, Double> = emptyMap()

    private val sharedPreferences = application.getSharedPreferences("currency_cache", Context.MODE_PRIVATE)

    init {
        fetchCurrencyData()
    }

    fun refreshData() {
        fetchCurrencyData()
    }

    private fun fetchCurrencyData() {
        _uiState.update { it.copy(networkStatus = NetworkStatus.LOADING) }
        val url = "https://currency-rate-exchange-api.onrender.com/all"

        val request = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                viewModelScope.launch(Dispatchers.IO) {
                    val ratesJsonString = response.getJSONObject("rates").getJSONObject("all").toString()
                    saveRatesToCache(ratesJsonString)
                    processRates(ratesJsonString)
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(networkStatus = NetworkStatus.CONNECTED) }
                    }
                }
            },
            { error ->
                Log.e("CurrencyConverterVM", "Volley error: ${error.message}")
                loadRatesFromCache()
            })
        requestQueue.add(request)
    }

    private fun processRates(ratesJsonString: String) {
        try {
            val ratesObject = JSONObject(ratesJsonString)
            val ratesMap = mutableMapOf<String, Double>()
            val codesList = mutableListOf<String>()
            val keys = ratesObject.keys()

            while (keys.hasNext()) {
                val code = keys.next()
                ratesMap[code.lowercase(Locale.getDefault())] = ratesObject.getDouble(code)
                codesList.add(code.uppercase(Locale.getDefault()))
            }
            codesList.sort()
            currencyRates = ratesMap

            val currentBase = _uiState.value.baseCurrency
            val defaultBase = if (codesList.contains(currentBase)) currentBase else "USD"

            _uiState.update { currentState ->
                currentState.copy(
                    allCurrencyCodes = codesList,
                    baseCurrency = defaultBase,
                    availableTargetCodes = codesList.filter { code ->
                        code != currentState.baseCurrency && currentState.targetCurrencies.none { it.code == code }
                    }
                )
            }
        } catch (e: Exception) {
            Log.e("CurrencyConverterVM", "Error parsing rates", e)
            _uiState.update { it.copy(networkStatus = NetworkStatus.ERROR, errorMessage = getString(R.string.data_analysis_error)) }
        }
    }

    private fun saveRatesToCache(ratesJsonString: String) {
        sharedPreferences.edit().putString("rates_json", ratesJsonString).apply()
    }

    private fun loadRatesFromCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val cachedRates = sharedPreferences.getString("rates_json", null)
            if (cachedRates != null) {
                processRates(cachedRates)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(networkStatus = NetworkStatus.OFFLINE) }
                }
            } else {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            networkStatus = NetworkStatus.ERROR,
                            errorMessage = getString(R.string.connection_error_occurred)
                        )
                    }
                }
            }
        }
    }

    fun convertCurrency() {
        val state = _uiState.value
        if (state.networkStatus == NetworkStatus.LOADING || state.networkStatus == NetworkStatus.ERROR) return

        val baseCode = state.baseCurrency.lowercase(Locale.getDefault())
        val amount = state.amountInput.toDoubleOrNull() ?: return

        val baseRate = currencyRates[baseCode]
        if (baseRate == null || baseRate == 0.0) {
            _uiState.update { it.copy(errorMessage = getString(R.string.conversion_rate_error)) }
            return
        }

        val updatedTargets = state.targetCurrencies.map { target ->
            val targetRate = currencyRates[target.code.lowercase(Locale.getDefault())]
            if (targetRate != null) {
                val convertedAmount = amount * targetRate / baseRate
                val result = String.format(Locale.getDefault(), "%.4f", convertedAmount)
                target.copy(result = result)
            } else {
                target.copy(result = "N/A")
            }
        }
        _uiState.update { it.copy(targetCurrencies = updatedTargets) }
    }

    fun setAmount(amount: String) {
        _uiState.update { it.copy(amountInput = amount) }
    }

    fun setBaseCurrency(currency: String) {
        _uiState.update { currentState ->
            val newAvailable = currentState.allCurrencyCodes.filter { code ->
                code != currency && currentState.targetCurrencies.none { it.code == code }
            }
            currentState.copy(
                baseCurrency = currency,
                availableTargetCodes = newAvailable,
                showBaseCurrencyWarning = currentState.targetCurrencies.any { it.code == currency }
            )
        }
    }

    fun dismissWarning() {
        _uiState.update { it.copy(showBaseCurrencyWarning = false) }
    }

    fun onAddCurrencyClicked() {
        _uiState.update { it.copy(isAddingCurrency = true, editingCurrencyCode = null) }
    }

    fun onCancelAddCurrency() {
        _uiState.update { it.copy(isAddingCurrency = false) }
    }

    fun onConfirmAddCurrency(code: String) {
        _uiState.update { currentState ->
            val newTarget = TargetCurrencyInfo(code = code)
            val updatedTargets = currentState.targetCurrencies + newTarget
            val newAvailable = currentState.availableTargetCodes.filter { it != code }
            currentState.copy(
                targetCurrencies = updatedTargets,
                isAddingCurrency = false,
                availableTargetCodes = newAvailable
            )
        }
    }

    fun onEditCurrencyClicked(code: String) {
        _uiState.update { it.copy(editingCurrencyCode = code, isAddingCurrency = false) }
    }

    fun onCancelEditCurrency() {
        _uiState.update { it.copy(editingCurrencyCode = null) }
    }

    fun onSaveEditCurrency(oldCode: String, newCode: String) {
        _uiState.update { currentState ->
            val updatedTargets = currentState.targetCurrencies.map {
                if (it.code == oldCode) it.copy(code = newCode) else it
            }
            val newAvailable = currentState.allCurrencyCodes.filter { code ->
                code != currentState.baseCurrency && updatedTargets.none { it.code == code }
            }
            currentState.copy(
                targetCurrencies = updatedTargets,
                editingCurrencyCode = null,
                availableTargetCodes = newAvailable,
                showBaseCurrencyWarning = newCode == currentState.baseCurrency
            )
        }
    }

    fun onDeleteCurrency(code: String) {
        _uiState.update { currentState ->
            val updatedTargets = currentState.targetCurrencies.filter { it.code != code }
            val newAvailable = (currentState.availableTargetCodes + code).sorted()
            currentState.copy(
                targetCurrencies = updatedTargets,
                editingCurrencyCode = null,
                availableTargetCodes = newAvailable
            )
        }
    }

    private fun getString(resId: Int): String {
        return getApplication<Application>().getString(resId)
    }
}