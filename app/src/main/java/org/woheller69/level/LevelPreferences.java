package org.woheller69.level;

import org.woheller69.level.config.DisplayType;
import org.woheller69.level.config.Viscosity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

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
public class LevelPreferences extends PreferenceActivity implements OnPreferenceChangeListener {
	
	public static final String KEY_SHOW_ANGLE 			= "preference_show_angle";
	public static final String KEY_DISPLAY_TYPE 		= "preference_display_type";
	public static final String KEY_SOUND 				= "preference_sound";
	public static final String KEY_LOCK				 	= "preference_lock";
	public static final String KEY_LOCK_LOCKED		 	= "preference_lock_locked";			// mémoriser le verouillage
	public static final String KEY_LOCK_ORIENTATION 	= "preference_lock_orientation";	// mémoriser l'orientation verouillée
	public static final String KEY_VISCOSITY			= "preference_viscosity";
	public static final String KEY_ECONOMY				= "preference_economy";
	
	private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    public void onResume() {
    	super.onResume();
    	// enregistrement des listerners
    	findPreference(KEY_DISPLAY_TYPE).setOnPreferenceChangeListener(this);
    	findPreference(KEY_VISCOSITY).setOnPreferenceChangeListener(this);
    	findPreference(KEY_ECONOMY).setOnPreferenceChangeListener(this);
    	// mise a jour de l'affichage
    	onPreferenceChange(findPreference(KEY_DISPLAY_TYPE), prefs.getString(LevelPreferences.KEY_DISPLAY_TYPE, "ANGLE")); 
    	findPreference(KEY_VISCOSITY).setSummary(Viscosity.valueOf(
    			prefs.getString(LevelPreferences.KEY_VISCOSITY, "MEDIUM")).getSummary());
    	findPreference(KEY_VISCOSITY).setEnabled(
				!((CheckBoxPreference) findPreference(KEY_ECONOMY)).isChecked());
    }

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String key = preference.getKey();
		if (KEY_DISPLAY_TYPE.equals(key)) {
		    CharSequence displaySummary = getText(DisplayType.valueOf((String) newValue).getSummary());
		    if (Build.VERSION.SDK_INT >= 11 /* 3.0 : HoneyComb */) {
		        // Fucking retro-compatibility !!!
		        displaySummary = String.valueOf(displaySummary).replaceAll("%", "%%");
		    }
			preference.setSummary(displaySummary);
		} else if (KEY_VISCOSITY.equals(key)) {
			preference.setSummary(Viscosity.valueOf((String) newValue).getSummary());
		} else if (KEY_ECONOMY.equals(key)) {
			findPreference(KEY_VISCOSITY).setEnabled(!((Boolean) newValue));
		}
		return true;
	}
    
}