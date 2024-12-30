package com.ost.application.activity.settings.base

import android.os.Bundle
import android.view.animation.PathInterpolator
import androidx.annotation.CallSuper
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.transition.MaterialSharedAxis
import com.ost.application.data.datastore.PreferenceDataStoreImpl

abstract class AbsBasePreferencesFragment : PreferenceFragmentCompat(),
    PreferenceDataStoreImpl.OnPreferencesChangeListener {

    @CallSuper
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = PreferenceDataStoreImpl.getInstance(requireContext())
    }

    @CallSuper
    override fun onPreferenceChanged(key: String, newValue: Any?) {
        findPreference<Preference>(key)?.apply {
            onPreferenceChangeListener?.onPreferenceChange(this, newValue)
        }
    }

    @CallSuper
    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceDataStoreImpl.getInstance(requireContext()).addOnPreferencesChangeListener(this)
        setupFragmentTransitions()
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        PreferenceDataStoreImpl.getInstance(requireContext()).removeOnPreferencesChangeListener(this)
    }

    private fun setupFragmentTransitions() {
        val interpolator = PathInterpolator(0.1f, 0.1f, 0f, 1f)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            this.interpolator = interpolator
        }
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
            this.interpolator = interpolator
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
            this.interpolator = interpolator
        }
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            this.interpolator = interpolator
        }
    }
}