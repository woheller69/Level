package net.androgames.level.orientation.provider;

import java.util.Arrays;
import java.util.List;

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

    private float[] ACC;
    private float[] MAG;
    private float[] R = new float[16];
    private float[] outR = new float[16];
    private float[] I = new float[16];
    private float[] LOC = new float[3];
	
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
	    
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            ACC = event.values.clone();

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            MAG = event.values.clone();

        if (ACC != null && MAG != null) {
            boolean success = SensorManager.getRotationMatrix(R, I, ACC, MAG);
            if (success) {
                
                switch (displayOrientation) {
                case Surface.ROTATION_270:
                    SensorManager.remapCoordinateSystem(
                            R, 
                            SensorManager.AXIS_MINUS_Y, 
                            SensorManager.AXIS_X, 
                            outR);
                    break;
                case Surface.ROTATION_180:
                    SensorManager.remapCoordinateSystem(
                            R, 
                            SensorManager.AXIS_MINUS_X, 
                            SensorManager.AXIS_MINUS_Y, 
                            outR);
                    break;
                case Surface.ROTATION_90:
                    SensorManager.remapCoordinateSystem(
                            R, 
                            SensorManager.AXIS_Y, 
                            SensorManager.AXIS_MINUS_X, 
                            outR);
                    break;
                case Surface.ROTATION_0:
                default:
                    SensorManager.remapCoordinateSystem(
                            R, 
                            SensorManager.AXIS_X, 
                            SensorManager.AXIS_Y, 
                            outR);
                    break;
                }
                
                SensorManager.getOrientation(outR, LOC);
                
                pitch = (float) (LOC[1] * 180 / Math.PI);
                roll = - (float) (LOC[2] * 180 / Math.PI);
                
                ACC = null;
                MAG = null;

            }
        }
	}


    protected List<Integer> getRequiredSensors() {
        return Arrays.asList(Integer.valueOf(Sensor.TYPE_ACCELEROMETER), Integer.valueOf(Sensor.TYPE_MAGNETIC_FIELD));
	}
	
}