package com.ost.application.ui.fragment.converters;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.ost.application.R;
import com.ost.application.databinding.FragmentCurrencyConverterBinding;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CurrencyConverterFragment extends Fragment {

    private FragmentCurrencyConverterBinding binding;
    private RequestQueue requestQueue;
    private Map<String, Double> currencyRates = new HashMap<>();
    private List<String> currencyCodes = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCurrencyConverterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requestQueue = Volley.newRequestQueue(requireContext());
        spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFrom.setAdapter(spinnerAdapter);
        binding.spinnerTo.setAdapter(spinnerAdapter);

        fetchCurrencyData();

        binding.buttonConvert.setOnClickListener(v -> convertCurrency());
    }

    private void fetchCurrencyData() {
        String url = "https://currency-rate-exchange-api.onrender.com/all";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject ratesObject = response.getJSONObject("rates").getJSONObject("all");
                        Iterator<String> keys = ratesObject.keys();

                        List<String> codes = new ArrayList<>();

                        while (keys.hasNext()) {
                            String currencyCode = keys.next();
                            Double rate = ratesObject.getDouble(currencyCode);
                            currencyRates.put(currencyCode, rate);
                            codes.add(currencyCode);
                        }

                        currencyCodes.clear();
                        currencyCodes.addAll(codes);

                        requireActivity().runOnUiThread(() -> {
                            spinnerAdapter.clear();
                            for (String code : currencyCodes) {
                                spinnerAdapter.add(code.toUpperCase(Locale.getDefault()));
                            }
                            spinnerAdapter.notifyDataSetChanged();
                        });

                    } catch (JSONException e) {
                        Log.e("CurrencyConverter", "Error parsing JSON: " + e.getMessage());
                        binding.mainCurrency.setText(getString(R.string.connection_error_occurred));
                    }
                },
                error -> {
                    Log.e("CurrencyConverter", "Volley error: " + error.getMessage());
                    binding.mainCurrency.setText(getString(R.string.data_analysis_error));
                });

        requestQueue.add(request);
    }

    private void convertCurrency() {
        String fromCurrency = binding.spinnerFrom.getSelectedItem().toString();
        String toCurrency = binding.spinnerTo.getSelectedItem().toString();
        String amountString = binding.inputCurrencyValue.getText().toString();

        double amount;
        if (amountString.isEmpty()) {
            amount = 1.0;
        } else {
            try {
                amount = Double.parseDouble(amountString);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), R.string.invalid_amount_entered, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        try {
            double fromRate = currencyRates.get(fromCurrency.toLowerCase(Locale.getDefault()));
            double toRate = currencyRates.get(toCurrency.toLowerCase(Locale.getDefault()));
            double convertedAmount = amount * toRate / fromRate;

            String fromCurrencyUpper = fromCurrency.toUpperCase(Locale.getDefault());
            String toCurrencyUpper = toCurrency.toUpperCase(Locale.getDefault());
            String result = String.format(getString(R.string.conversion_result), amount, fromCurrencyUpper, convertedAmount, toCurrencyUpper);
            binding.mainCurrency.setText(result);

        } catch (NullPointerException e) {
            Log.e("Error", "Error. Null");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}