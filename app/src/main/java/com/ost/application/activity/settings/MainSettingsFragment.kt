package com.ost.application.activity.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.preference.DropDownPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.Preference.SummaryProvider
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import androidx.preference.SeslSwitchPreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import com.ost.application.AboutActivity
import com.ost.application.R
import com.ost.application.activity.settings.base.AbsBasePreferencesFragment
import com.ost.application.data.StargazersRepo.Companion.PREF_ACTIONMODE_SEARCH
import com.ost.application.data.StargazersRepo.Companion.PREF_AUTO_DARK_MODE
import com.ost.application.data.StargazersRepo.Companion.PREF_DARK_MODE
import com.ost.application.data.StargazersRepo.Companion.PREF_INDEXSCROLL_ENABLE
import com.ost.application.data.StargazersRepo.Companion.PREF_SEARCHMODE_BACK_BEHAVIOR
import com.ost.application.data.util.determineDarkMode
import com.ost.application.ui.core.toast
import com.ost.application.ui.core.util.applyDarkMode
import dev.oneuiproject.oneui.ktx.onClick
import dev.oneuiproject.oneui.ktx.onNewValue
import dev.oneuiproject.oneui.ktx.provideSummary
import dev.oneuiproject.oneui.ktx.setSummaryUpdatable
import dev.oneuiproject.oneui.preference.HorizontalRadioPreference


class MainSettingsFragment : AbsBasePreferencesFragment(){
    private lateinit var mContext: Context
    private val REQUEST_WRITE_SETTINGS = 100

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initPreferences()
    }

    private fun initPreferences() {
        findPreference<HorizontalRadioPreference>(PREF_DARK_MODE.name)!!.apply {
            setDividerEnabled(false)
            setTouchEffectEnabled(false)
            onNewValue {
                value = it
                applyDarkModePrefs()
            }
        }

        findPreference<SwitchPreferenceCompat>(PREF_AUTO_DARK_MODE.name)!!.apply {
            onNewValue{
                isChecked = it
                applyDarkModePrefs()
            }
        }

        findPreference<SeslSwitchPreferenceScreen>(PREF_INDEXSCROLL_ENABLE.name)!!.apply {
            onClick {
                findNavController().navigate(R.id.to_indexscroll_preference_action)
            }
            provideSummary {
                "${getString(R.string.autohide)} • ${getString(R.string.show_letters)}"
            }
        }

        findPreference<DropDownPreference>(PREF_ACTIONMODE_SEARCH.name)!!.setSummaryUpdatable(true)

        findPreference<ListPreference>(PREF_SEARCHMODE_BACK_BEHAVIOR.name)!!.setSummaryUpdatable(true)


        findPreference<Preference>("about_app")!!.apply {
            onClick {
                startActivity(Intent(requireActivity(), AboutActivity::class.java))
            }
        }

        val brightnessControlPref: SwitchPreferenceCompat? = findPreference("brightness_control")
        brightnessControlPref?.setOnPreferenceChangeListener { _, newValue ->
            val isEnabled = newValue as Boolean
            if (isEnabled && !Settings.System.canWrite(requireContext())) {
                requestWriteSettingsPermission()
                false
            } else {
                true
            }
        }

        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)

        val durationTotalPref: SeekBarPreference? = findPreference("total_duration")
        durationTotalPref?.setOnPreferenceChangeListener { _, newValue ->
            sharedPreferences.edit().putInt("total_duration", newValue as Int).apply()
            true
        }

        val durationNoisePref: SeekBarPreference? = findPreference("noise_duration")
        durationNoisePref?.setOnPreferenceChangeListener { _, newValue ->
            sharedPreferences.edit().putInt("noise_duration", newValue as Int).apply()
            true
        }

        val durationBlackWhiteNoisePref: SeekBarPreference? = findPreference("noise_duration")
        durationBlackWhiteNoisePref?.setOnPreferenceChangeListener { _, newValue ->
            sharedPreferences.edit().putInt("black_white_noise_duration", newValue as Int).apply()
            true
        }

        val durationHorizontalLinesPref: SeekBarPreference? = findPreference("duration_horizontal_lines")
        durationHorizontalLinesPref?.setOnPreferenceChangeListener { _, newValue ->
            sharedPreferences.edit().putInt("duration_horizontal_lines", newValue as Int).apply()
            true
        }

        val durationVerticalLinesPref: SeekBarPreference? = findPreference("duration_vertical_lines")
        durationVerticalLinesPref?.setOnPreferenceChangeListener { _, newValue ->
            sharedPreferences.edit().putInt("duration_vertical_lines", newValue as Int).apply()
            true
        }
    }

    private fun requestWriteSettingsPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:${requireContext().packageName}")
        }
        startActivityForResult(intent, REQUEST_WRITE_SETTINGS)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_WRITE_SETTINGS) {
            val brightnessControlPref: SwitchPreferenceCompat? = findPreference("brightness_control")
            brightnessControlPref?.let {
                if (Settings.System.canWrite(requireContext())) {
                    it.isChecked = true
                } else {
                    it.isChecked = false
                    toast(getString(R.string.brightness_permission_r))
                }
            }
        }
    }

    private fun Preference.applyDarkModePrefs() {
        val darkMode = with(preferenceDataStore!!) {
            determineDarkMode(getString(PREF_DARK_MODE.name, "0")!!,
                getBoolean(PREF_AUTO_DARK_MODE.name, true))
        }
        applyDarkMode(darkMode)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }
}