package net.androgames.level.config;

import net.androgames.level.R;
import net.androgames.level.orientation.OrientationProvider;
import net.androgames.level.orientation.provider.ProviderAccelerometer;
import net.androgames.level.orientation.provider.ProviderOrientation;

/*
 *  This file is part of Level (an Android Bubble Level).
 *  <https://github.com/avianey/Level>
 *  
 *  Copyright (C) 2012 Antoine Vianey
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
public enum Provider {

	ORIENTATION(R.string.orientation, R.string.orientation_summary, R.string.orientation_sensor),
	ACCELEROMETER(R.string.accelerometer, R.string.accelerometer_summary, R.string.accelerometer_sensor);

	private int label;
	private int summary;
	private int name;
	
	private Provider(int label, int summary, int name) {
		this.label = label;
		this.name = name;
		this.summary = summary;
	}
	
	public int getSummary() {
		return summary;
	}

	public int getName() {
		return name;
	}
	
	public int getLabel() {
		return label;
	}

	public OrientationProvider getProvider() {
		switch (this) {
			case ACCELEROMETER : return ProviderAccelerometer.getInstance();
			case ORIENTATION : return ProviderOrientation.getInstance();
		}
		return null;
	}
	
}
