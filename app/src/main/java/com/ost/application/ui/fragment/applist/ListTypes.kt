package com.ost.application.ui.fragment.applist

import androidx.annotation.StringRes
import androidx.apppickerview.widget.AppPickerView
import com.ost.application.R

enum class ListTypes(@AppPickerView.AppPickerType val type: Int, @StringRes var description: Int) {

    LIST_TYPE(AppPickerView.TYPE_LIST, R.string.list),
    TYPE_LIST_ACTION_BUTTON(AppPickerView.TYPE_LIST_ACTION_BUTTON, R.string.list_action_button);

}
