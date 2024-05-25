package me.greenrobot.apps.level

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.Preference.SummaryProvider
import androidx.preference.PreferenceFragmentCompat
import me.greenrobot.apps.level.util.PrefKeys
import me.greenrobot.apps.level.util.PreferenceHelper

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        val displayType = findPreference<ListPreference>(getString(PrefKeys.PREF_DISPLAY_TYPE))
        if (displayType != null) {
            displayType.summaryProvider = SummaryProvider<ListPreference> { preference: Preference? ->
                if (PreferenceHelper.isDisplayTypeInclination()) {
                    return@SummaryProvider getString(R.string.inclination_summary)
                }
                getString(R.string.angle_summary)
            }
        }

        val viscosity = findPreference<ListPreference>(getString(PrefKeys.PREF_VISCOSITY))
        if (viscosity != null) {
            viscosity.summaryProvider = SummaryProvider<ListPreference> { preference: Preference? ->
                if (PreferenceHelper.isViscosityLow()) {
                    return@SummaryProvider getString(R.string.viscosity_low_summary)
                }
                if (PreferenceHelper.isViscosityHigh()) {
                    return@SummaryProvider getString(R.string.viscosity_high_summary)
                }
                getString(R.string.viscosity_medium_summary)
            }
        }
    }
}
