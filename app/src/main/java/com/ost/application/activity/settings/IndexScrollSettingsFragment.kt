package com.ost.application.activity.settings

import android.os.Bundle
import com.ost.application.R
import com.ost.application.activity.settings.base.AbsBasePreferencesFragment

class IndexScrollSettingsFragment : AbsBasePreferencesFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.preferences_indexscroll, rootKey)
    }

}