package me.greenrobot.apps.level;

import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import org.woheller69.level.R;

import me.greenrobot.apps.level.util.PrefKeys;
import me.greenrobot.apps.level.util.PreferenceHelper;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(final Bundle savedInstanceState, final String rootKey) {
        addPreferencesFromResource(R.xml.preferences);

        final ListPreference displayType = findPreference(getString(PrefKeys.PREF_DISPLAY_TYPE));
        if (displayType != null) {
            displayType.setSummaryProvider(preference -> {
                if (PreferenceHelper.isDisplayTypeInclination()) {
                    return getString(R.string.inclination_summary);
                }
                // default display type
                return getString(R.string.angle_summary);
            });
        }

        final ListPreference viscosity = findPreference(getString(PrefKeys.PREF_VISCOSITY));
        if (viscosity != null) {
            viscosity.setSummaryProvider(preference -> {
                if (PreferenceHelper.isViscosityLow()) {
                    return getString(R.string.viscosity_low_summary);
                }
                if (PreferenceHelper.isViscosityHigh()) {
                    return getString(R.string.viscosity_high_summary);
                }
                // default viscosity
                return getString(R.string.viscosity_medium_summary);
            });
        }
    }
}
