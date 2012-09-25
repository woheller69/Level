package net.androgames.level.orientation.provider;

import net.androgames.level.orientation.OrientationProvider;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
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
public class ProviderAccelerometer extends OrientationProvider {
	
	private static OrientationProvider provider;
	
	private static final float[] GEOMAGNETIC_FIELD = new float[] {1, 1, 1};
	
	private ProviderAccelerometer() {
		super();
	}
	
	public static OrientationProvider getInstance() {
		if (provider == null) {
			provider = new ProviderAccelerometer();
		}
		return provider;
	}
 

	/**
	 * Calculate pitch and roll according to
	 * http://android-developers.blogspot.com/2010/09/one-screen-turn-deserves-another.html
	 * @param event
	 */
	protected void handleSensorChanged(SensorEvent event) {
	    float[] R = new float[16];
	    float[] I = new float[16];

	    SensorManager.getRotationMatrix(R, I, event.values, GEOMAGNETIC_FIELD);

	    float[] actual_orientation = new float[3];
	    float[] outR = new float[16];
	    
	    switch (displayOrientation) {
	    case Surface.ROTATION_270:
		    SensorManager.remapCoordinateSystem(R, 
		    		SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, outR);
	    	break;
	    case Surface.ROTATION_180:
		    SensorManager.remapCoordinateSystem(R, 
		    		SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, outR);
	    	break;
	    case Surface.ROTATION_90:
		    SensorManager.remapCoordinateSystem(R, 
		    		SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, outR);
	    	break;
	    case Surface.ROTATION_0:
    	default:
		    SensorManager.remapCoordinateSystem(R, 
		    		SensorManager.AXIS_X, SensorManager.AXIS_Y, outR);
	    	break;
	    }
	    
	    SensorManager.getOrientation(outR, actual_orientation);
	    pitch = (float) (actual_orientation[1] * 180 / Math.PI);
        roll = - (float) (actual_orientation[2] * 180 / Math.PI);
	}

	@Override
	protected int getSensorType() {
		return Sensor.TYPE_ACCELEROMETER;
	}
	
}