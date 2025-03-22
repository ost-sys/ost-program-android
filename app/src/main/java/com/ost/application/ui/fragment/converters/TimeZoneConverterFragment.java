package com.ost.application.ui.fragment.converters;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ost.application.R;
import com.ost.application.databinding.FragmentTimeZoneConverterBinding;
import com.ost.application.ui.core.base.BaseFragment;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TimeZoneConverterFragment extends Fragment {

    private FragmentTimeZoneConverterBinding binding;
    private Calendar selectedTime;
    private String sourceTimeZoneId;
    private String targetTimeZoneId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTimeZoneConverterBinding.inflate(inflater, container, false);
        selectedTime = Calendar.getInstance();

        List<String> timeZones = getAvailableTimeZones();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, timeZones);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        binding.sourceTimeZoneSpinner.setAdapter(adapter);
        binding.targetTimeZoneSpinner.setAdapter(adapter);

        sourceTimeZoneId = "Etc/UTC";
        targetTimeZoneId = ZoneId.systemDefault().getId();

        setSpinnerSelections();
        updateDateTime();

        binding.sourceTimeZoneSpinner.setOnItemSelectedListener(spinnerListener);
        binding.targetTimeZoneSpinner.setOnItemSelectedListener(spinnerListener);

        binding.timePicker.setOnTimeChangedListener((view, hourOfDay, minute) -> {
            selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            selectedTime.set(Calendar.MINUTE, minute);
            updateDateTime();
        });

        return binding.getRoot();
    }

    private void setSpinnerSelections() {
        ArrayAdapter<String> sourceAdapter = (ArrayAdapter<String>) binding.sourceTimeZoneSpinner.getAdapter();
        int sourcePosition = sourceAdapter.getPosition(sourceTimeZoneId);

        ArrayAdapter<String> targetAdapter = (ArrayAdapter<String>) binding.targetTimeZoneSpinner.getAdapter();
        int targetPosition = targetAdapter.getPosition(targetTimeZoneId);

        if (sourcePosition != AdapterView.INVALID_POSITION) {
            binding.sourceTimeZoneSpinner.setSelection(sourcePosition);
        }

        if (targetPosition != AdapterView.INVALID_POSITION) {
            binding.targetTimeZoneSpinner.setSelection(targetPosition);
        }
    }

    private final AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (parent.getId() == binding.sourceTimeZoneSpinner.getId()) {
                sourceTimeZoneId = parent.getItemAtPosition(position).toString();
            } else if (parent.getId() == binding.targetTimeZoneSpinner.getId()) {
                targetTimeZoneId = parent.getItemAtPosition(position).toString();
            }
            updateDateTime();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    private void updateDateTime() {
        try {
            ZonedDateTime zonedDateTime = ZonedDateTime.of(
                    selectedTime.get(Calendar.YEAR),
                    selectedTime.get(Calendar.MONTH) + 1,
                    selectedTime.get(Calendar.DAY_OF_MONTH),
                    selectedTime.get(Calendar.HOUR_OF_DAY),
                    selectedTime.get(Calendar.MINUTE),
                    0, 0, ZoneId.of(sourceTimeZoneId)
            );

            ZonedDateTime convertedDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of(targetTimeZoneId));
            String formattedDateTime = convertedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()));

            binding.timeZoneResult.setText(formattedDateTime);

        } catch (DateTimeParseException e) {
            binding.timeZoneResult.setText(getString(R.string.error));
        }
    }

    private List<String> getAvailableTimeZones() {
        List<String> timeZones = new ArrayList<>(Arrays.asList(ZoneId.getAvailableZoneIds().toArray(new String[0])));
        Collections.sort(timeZones);
        return timeZones;
    }
}
