package com.ost.application.ui.fragment.converters;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.picker.widget.SeslDatePicker;
import androidx.picker.widget.SeslTimePicker;

import com.ost.application.R;
import com.ost.application.databinding.FragmentTimeCalculatorBinding;
import com.ost.application.ui.core.base.BaseFragment;

import java.util.Calendar;
import java.util.Locale;

import dev.oneuiproject.oneui.widget.Toast;

public class TimeCalculatorFragment extends Fragment {

    FragmentTimeCalculatorBinding binding;
    private TextView resultTextView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentTimeCalculatorBinding.inflate(inflater, container, false);

        resultTextView = binding.timeCalculatorResult;
        binding.timeCalculatorCalculate.setOnClickListener(v -> calculateTimeDifference());

        return binding.getRoot();
    }

    private void calculateTimeDifference() {
        try {
            Calendar firstDate = getCalendarFromPickers(binding.timeCalculatorFirstDate, binding.timeCalculatorFirstTime);
            Calendar secondDate = getCalendarFromPickers(binding.timeCalculatorSecondDate, binding.timeCalculatorSecondTime);

            if (firstDate.after(secondDate)) {
                Calendar temp = firstDate;
                firstDate = secondDate;
                secondDate = temp;
            }

            long diffInYears = secondDate.get(Calendar.YEAR) - firstDate.get(Calendar.YEAR);
            int diffInMonths = secondDate.get(Calendar.MONTH) - firstDate.get(Calendar.MONTH);
            int diffInDays = secondDate.get(Calendar.DAY_OF_MONTH) - firstDate.get(Calendar.DAY_OF_MONTH);
            int diffInHours = secondDate.get(Calendar.HOUR_OF_DAY) - firstDate.get(Calendar.HOUR_OF_DAY);
            int diffInMinutes = secondDate.get(Calendar.MINUTE) - firstDate.get(Calendar.MINUTE);

            if (diffInMinutes < 0) {
                diffInMinutes += 60;
                diffInHours--;
            }
            if (diffInHours < 0) {
                diffInHours += 24;
                diffInDays--;
            }
            if (diffInDays < 0) {
                firstDate.add(Calendar.MONTH, 1);
                diffInDays = firstDate.getActualMaximum(Calendar.DAY_OF_MONTH) - firstDate.get(Calendar.DAY_OF_MONTH) + secondDate.get(Calendar.DAY_OF_MONTH);
                diffInMonths--;
            }
            if (diffInMonths < 0) {
                diffInMonths += 12;
                diffInYears--;
            }

            String yearsString = formatYears(diffInYears);
            String formattedResult = String.format(Locale.getDefault(),
                    "%s, %d %s, %d %s, %d %s, %d %s",
                    yearsString, diffInMonths, getString(R.string.months),
                    diffInDays, getString(R.string.days), diffInHours,
                    getString(R.string.hours), diffInMinutes, getString(R.string.minutes));

            resultTextView.setText(formattedResult);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String formatYears(long diffInYears) {
        int lastDigit = (int) (diffInYears % 10);
        String yearsString;
        if (diffInYears == 0) {
            yearsString = getString(R.string.years_one);
        } else if (diffInYears % 10 >= 2 && diffInYears % 10 <= 4) {
            yearsString = getString(R.string.years_two_four);
        } else if (lastDigit == 1) {
            yearsString = getString(R.string.years_one);
        } else {
            yearsString = getString(R.string.years_five_nine);
        }
        return String.format(Locale.getDefault(), getString(R.string.diff_years_format), diffInYears, yearsString);
    }

    private Calendar getCalendarFromPickers(SeslDatePicker datePicker, SeslTimePicker timePicker) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(),
                timePicker.getHour(), timePicker.getMinute());
        return calendar;
    }
}
