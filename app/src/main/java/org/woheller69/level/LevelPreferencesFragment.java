package org.woheller69.level;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import org.woheller69.level.config.DisplayType;
import org.woheller69.level.config.Viscosity;

/*
 *  This file is part of Level (an Android Bubble Level).
 *  <https://github.com/avianey/Level>
 *
 *  Copyright (C) 2014 Antoine Vianey
 *
 *  Level is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Level is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Level. If not, see <http://www.gnu.org/licenses/>
 */
public class LevelPreferencesFragment extends PreferenceFragmentCompat implements OnPreferenceChangeListener {

    public static final String KEY_SHOW_ANGLE = "preference_show_angle";
    public static final String KEY_DISPLAY_TYPE = "preference_display_type";
    public static final String KEY_SOUND = "preference_sound";
    public static final String KEY_LOCK = "preference_lock";
    public static final String KEY_VISCOSITY = "preference_viscosity";

    private SharedPreferences prefs;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        prefs = PreferenceManager.getDefaultSharedPreferences(requireActivity());
    }

    public void onResume() {
        super.onResume();
        // enregistrement des listerners
        findPreference(KEY_DISPLAY_TYPE).setOnPreferenceChangeListener(this);
        findPreference(KEY_VISCOSITY).setOnPreferenceChangeListener(this);
        // mise a jour de l'affichage
        onPreferenceChange(findPreference(KEY_DISPLAY_TYPE), prefs.getString(LevelPreferencesFragment.KEY_DISPLAY_TYPE, "ANGLE"));
        findPreference(KEY_VISCOSITY).setSummary(Viscosity.valueOf(
                prefs.getString(LevelPreferencesFragment.KEY_VISCOSITY, "MEDIUM")).getSummary());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (KEY_DISPLAY_TYPE.equals(key)) {
            CharSequence displaySummary = getText(DisplayType.valueOf((String) newValue).getSummary());
            // Fucking retro-compatibility !!!
            displaySummary = String.valueOf(displaySummary).replaceAll("%", "%%");
            preference.setSummary(displaySummary);
        } else if (KEY_VISCOSITY.equals(key)) {
            preference.setSummary(Viscosity.valueOf((String) newValue).getSummary());
        }
        return true;
    }
}
