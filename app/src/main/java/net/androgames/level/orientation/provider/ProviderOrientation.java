package net.androgames.level.orientation.provider;

import java.util.Arrays;
import java.util.List;

import net.androgames.level.orientation.OrientationProvider;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.view.Surface;

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
public class ProviderOrientation extends OrientationProvider {
	
	private static OrientationProvider provider;
	
	private float tmp;
	
	private ProviderOrientation() {
		super();
	}
	
	public static OrientationProvider getInstance() {
		if (provider == null) {
			provider = new ProviderOrientation();
		}
		return provider;
	}

	protected void handleSensorChanged(SensorEvent event) {
        pitch = event.values[1];
        roll = event.values[2];

	    switch (displayOrientation) {
	    case Surface.ROTATION_270:
	    	pitch = - pitch;
	    	roll = - roll;
	    case Surface.ROTATION_90:
	    	tmp = pitch;
	    	pitch = - roll;
	    	roll = tmp;
	    	if (roll > 90) {
	    		roll = 180 - roll;
	    		pitch = - pitch - 180;
	    	} else if (roll < -90) {
	    		roll = - roll - 180;
	    		pitch = 180 - pitch;
	    	}
	    	break;
	    case Surface.ROTATION_180:
	    	pitch = - pitch;
	    	roll = - roll;
	    case Surface.ROTATION_0:
    	default:
	    	break;
	    }
	}


	protected List<Integer> getRequiredSensors() {
		return Arrays.asList(Integer.valueOf(Sensor.TYPE_ORIENTATION));
	}

}