package net.androgames.level.orientation;

import java.util.Arrays;
import java.util.List;

import net.androgames.level.Level;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

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
public abstract class OrientationProvider implements SensorEventListener {
	
	private static final int MIN_VALUES = 20;

	/** Calibration */
	private static final String SAVED_PITCH = "net.androgames.level.pitch.";
	private static final String SAVED_ROLL = "net.androgames.level.roll.";
	
    private Sensor sensor;
    private SensorManager sensorManager;
	private OrientationListener listener;

    /** indicates whether or not Accelerometer Sensor is supported */
    private Boolean supported;
    
    /** indicates whether or not Accelerometer Sensor is running */
    private boolean running = false;

	/** Calibration */
	private float[] calibratedPitch = new float[5];
	private float[] calibratedRoll = new float[5];
	private boolean calibrating = false;
	
	/** Orientation */
    protected float pitch;
    protected float roll;
    protected float tmp;
    private float oldPitch;
    private float oldRoll;
    private float minStep = 360;
    private float refValues = 0;
	private Orientation orientation;
	private boolean locked;
	protected int displayOrientation;
 
    protected OrientationProvider() {
		this.displayOrientation = Level.getContext().getWindowManager().getDefaultDisplay().getOrientation();
	}

	/**
     * Returns true if the manager is listening to orientation changes
     */
    public boolean isListening() {
        return running;
    }
 
    /**
     * Unregisters listeners
     */
    public void stopListening() {
        running = false;
        try {
            if (sensorManager != null) {
                sensorManager.unregisterListener(this);
            }
        } catch (Exception e) {}
    }

    protected abstract int getSensorType();

    /**
     * Returns true if at least one Accelerometer sensor is available
     */
    public boolean isSupported() {
        if (supported == null) {
            if (Level.getContext() != null) {
                sensorManager = (SensorManager) Level.getContext().getSystemService(Context.SENSOR_SERVICE);
                List<Sensor> sensors = sensorManager.getSensorList(getSensorType());
                return sensors.size() > 0;
            }
        }
        return false;
    }
    
    /**
     * Registers a listener and start listening
     * @param accelerometerListener
     *             callback for accelerometer events
     */
    public void startListening(OrientationListener orientationListener) {
    	final Activity context = Level.getContext();
    	// load calibration
    	calibrating = false;
		Arrays.fill(calibratedPitch, 0);
		Arrays.fill(calibratedRoll, 0);
    	SharedPreferences prefs = context.getPreferences(Context.MODE_PRIVATE);
    	for (Orientation orientation : Orientation.values()) {
    		calibratedPitch[orientation.ordinal()] = 
    			prefs.getFloat(SAVED_PITCH + orientation.toString(), 0);
    		calibratedRoll[orientation.ordinal()] = 
    			prefs.getFloat(SAVED_ROLL + orientation.toString(), 0);
    	}
    	// register listener and start listening
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> sensors = sensorManager.getSensorList(getSensorType());
        if (sensors.size() > 0) {
            sensor = sensors.get(0);
            running = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            listener = orientationListener;
        }
    }

	public void onAccuracyChanged(Sensor sensor, int accuracy) {}

	public void onSensorChanged(SensorEvent event) {
		
		oldPitch = pitch;
		oldRoll = roll;
		
		handleSensorChanged(event);

		// calculating minimal sensor step
		if (oldRoll != roll || oldPitch != pitch) {
			if (oldPitch != pitch) {
				minStep = Math.min(minStep, Math.abs(pitch - oldPitch));
			}
			if (oldRoll != roll) {
				minStep = Math.min(minStep, Math.abs(roll - oldRoll));
			}
			if (refValues < MIN_VALUES) {
				refValues++;
			}
		}
		
		if (!locked || orientation == null) {
	        if (pitch < -45 && pitch > -135) {
	            // top side up
	            orientation = Orientation.TOP;
	        } else if (pitch > 45 && pitch < 135) {
	            // bottom side up
	            orientation = Orientation.BOTTOM;
	        } else if (roll > 45) {
	            // right side up
	            orientation = Orientation.RIGHT;
	        } else if (roll < -45) {
	            // left side up
	            orientation = Orientation.LEFT;
	        } else {
	        	// landing
	        	orientation = Orientation.LANDING;
	        }
		}
		
		if (calibrating) {
			calibrating = false;
			Editor editor = Level.getContext().getPreferences(Context.MODE_PRIVATE).edit();
			editor.putFloat(SAVED_PITCH + orientation.toString(), pitch);
			editor.putFloat(SAVED_ROLL + orientation.toString(), roll);
			final boolean success = editor.commit();
			if (success) {
				calibratedPitch[orientation.ordinal()] = pitch;
				calibratedRoll[orientation.ordinal()] = roll;
			}
			listener.onCalibrationSaved(success);
			pitch = 0;
			roll = 0;
		} else {
	        pitch -= calibratedPitch[orientation.ordinal()];
	        roll -= calibratedRoll[orientation.ordinal()];
		}

		// propagation of the orientation
        listener.onOrientationChanged(orientation, pitch, roll);
	}
    
	protected abstract void handleSensorChanged(SensorEvent event);

	/**
	 * Tell the provider to restore the calibration
	 * to the default factory values
	 */
	public final void resetCalibration() {
		boolean success = false;
		try {
			success = Level.getContext().getPreferences(
					Context.MODE_PRIVATE).edit().clear().commit();
		} catch (Exception e) {}
		if (success) {
			Arrays.fill(calibratedPitch, 0);
			Arrays.fill(calibratedRoll, 0);
		}
		if (listener != null) {
			listener.onCalibrationReset(success);
		}
	}
    
	/**
	 * Tell the provider to save the calibration
	 * The calibration is actually saved on the next
	 * sensor change event
	 */
	public final void saveCalibration() {
		calibrating = true;
	}
	
	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	/**
	 * Return the minimal sensor step
	 * @return
	 * 		the minimal sensor step
	 * 		0 if not yet known
	 */
	public float getSensibility() {
		if (refValues >= MIN_VALUES) {
			return minStep;
		} else {
			return 0;
		}
	}
	
}
