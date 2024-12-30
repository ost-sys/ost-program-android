package com.ost.application.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;

import com.ost.application.R;
import com.ost.application.databinding.FragmentCurrencyConverterBinding;
import com.ost.application.ui.core.base.BaseFragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

import dev.oneuiproject.oneui.widget.TipPopup;

public class CurrencyConverterFragment extends BaseFragment {
    private FragmentCurrencyConverterBinding binding;
    private JSONObject rates;
    private int textInputCount = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCurrencyConverterBinding.inflate(inflater, container, false);

        String savedToken = getSavedToken();
        binding.token.setText(savedToken);

        fetchCurrencies();
        onHiddenChanged(isHidden());

        binding.buttonConvert.setOnClickListener(v -> convertCurrency(rates));

        return binding.getRoot();
    }


    private void fetchCurrencies() {
        String token = binding.token.getText().toString();
        String url = "https://v6.exchangerate-api.com/v6/" + token + "/latest/USD";

        TipPopup tipPopup = new TipPopup(binding.token, TipPopup.MODE_NORMAL);
        tipPopup.setMessage(getString(R.string.enter_your_token_text_input));
        tipPopup.setAction("OK", v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://app.exchangerate-api.com/sign-up"));
            startActivity(intent);
        });
        tipPopup.setExpanded(true);

        binding.token.setOnClickListener(v -> {
            if (textInputCount < 1) {
                tipPopup.show(TipPopup.DIRECTION_DEFAULT);
                textInputCount += 1;
            }
        });

        RequestQueue queue = Volley.newRequestQueue(mContext);
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        rates = jsonObject.getJSONObject("conversion_rates");

                        ArrayList<String> currencyList = new ArrayList<>();
                        Iterator<String> keys = rates.keys();

                        while (keys.hasNext()) {
                            currencyList.add(keys.next());
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_dropdown_item, currencyList);
                        binding.spinnerFrom.setAdapter(adapter);
                        binding.spinnerTo.setAdapter(adapter);


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> binding.mainCurrency.setText(R.string.enter_your_token)
        );

        queue.add(request);
    }

    private void convertCurrency(JSONObject rates) {
        try {
            String fromCurrency = binding.spinnerFrom.getSelectedItem().toString();
            String toCurrency = binding.spinnerTo.getSelectedItem().toString();

            double fromRate = rates.getDouble(fromCurrency);
            double toRate = rates.getDouble(toCurrency);

            double conversionRate = toRate / fromRate;

            binding.mainCurrency.setText(String.format(getString(R.string._1_s_2f_s), fromCurrency, conversionRate, toCurrency));
        } catch (Exception e) {
            e.printStackTrace();
            binding.mainCurrency.setText(R.string.conversion_error);
        }
    }

    private void saveToken(String token) {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("token", token);
        editor.apply();
    }
    private String getSavedToken() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
        return sharedPreferences.getString("token", "");
    }
    private void onTokenChanged() {
        String token = binding.token.getText().toString();
        saveToken(token);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            requireActivity().addMenuProvider(menuProvider);
        } else {
            requireActivity().removeMenuProvider(menuProvider);
        }
    }

    private MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            MenuItem menuItem = menu.findItem(R.id.menu_main_refresh);
            menuItem.setVisible(true);
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem item) {
            if (item.getItemId() == R.id.menu_main_refresh) {
                onTokenChanged();
                fetchCurrencies();
                return true;
            }
            return false;
        }
    };

    @Override
    public int getLayoutResId() {
        return R.layout.fragment_currency_converter;
    }

    @Override
    public int getIconResId() {
        return dev.oneuiproject.oneui.R.drawable.ic_oui_symbol_dollar;
    }

    @Override
    public CharSequence getTitle() {
        return getString(R.string.currency_converter);
    }
}