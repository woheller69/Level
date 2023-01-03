package org.woheller69.level.orientation;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Surface;

import androidx.appcompat.app.AppCompatActivity;

import org.woheller69.level.Level;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
public class OrientationProvider implements SensorEventListener {

    private static final int MIN_VALUES = 20;

    /**
     * Calibration
     */
    private static final String SAVED_PITCH = "pitch.";
    private static final String SAVED_ROLL = "roll.";
    private static final String SAVED_BALANCE = "balance.";
    private static final String SAVED_COUNT = "valuecount.";
    private static OrientationProvider provider;
    /**
     * Calibration
     */
    private final float[] calibratedPitch = new float[5];
    private final float[] calibratedRoll = new float[5];
    private final float[] calibratedBalance = new float[5];
    private final int[] calibratedValueCount = new int[5];
    /**
     * Rotation Matrix
     */
    private final float[] MAG = new float[]{1f, 1f, 1f};
    private final float[] I = new float[16];
    private final float[] R = new float[16];
    private final float[] outR = new float[16];
    private final float[] LOC = new float[3];
    /**
     * Orientation
     */
    public float pitch;
    public float roll;
    public int displayOrientation;
    private Sensor sensor;
    private SensorManager sensorManager;
    private OrientationListener listener;
    /**
     * indicates whether or not Accelerometer Sensor is supported
     */
    private Boolean supported;
    /**
     * indicates whether or not Accelerometer Sensor is running
     */
    private boolean running = false;
    private boolean calibrating = false;
    private float balance;
    private float tmp;
    private float oldPitch;
    private float oldRoll;
    private float oldBalance;
    private float minStep = 360;
    private float refValues = 0;
    private Orientation orientation;
    private boolean locked;

    public OrientationProvider() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            this.displayOrientation = Level.getContext().getDisplay().getRotation();
        } else {
            this.displayOrientation = Level.getContext().getWindowManager().getDefaultDisplay().getRotation();
        }
    }

    public static OrientationProvider getInstance() {
        if (provider == null) {
            provider = new OrientationProvider();
        }
        return provider;
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
        } catch (Exception e) {
        }
    }

    private List<Integer> getRequiredSensors() {
        return Collections.singletonList(
                Sensor.TYPE_ACCELEROMETER
        );
    }

    /**
     * Returns true if at least one Accelerometer sensor is available
     */
    public boolean isSupported() {
        boolean supported = false;
        if (Level.getContext() != null) {
            sensorManager = (SensorManager) Level.getContext().getSystemService(Context.SENSOR_SERVICE);
            supported = true;
            for (int sensorType : getRequiredSensors()) {
                List<Sensor> sensors = sensorManager.getSensorList(sensorType);
                supported = (sensors.size() > 0) && supported;
            }
        }
        return supported;
    }


    /**
     * Registers a listener and start listening
     * callback for accelerometer events
     */
    public void startListening(OrientationListener orientationListener) {
        final AppCompatActivity context = Level.getContext();
        // load calibration
        calibrating = false;
        Arrays.fill(calibratedPitch, 0);
        Arrays.fill(calibratedRoll, 0);
        Arrays.fill(calibratedBalance, 0);
        Arrays.fill(calibratedValueCount, 0);
        SharedPreferences prefs = context.getPreferences(Context.MODE_PRIVATE);
        for (Orientation orientation : Orientation.values()) {
            calibratedPitch[orientation.ordinal()] =
                    prefs.getFloat(SAVED_PITCH + orientation.toString(), 0);
            calibratedRoll[orientation.ordinal()] =
                    prefs.getFloat(SAVED_ROLL + orientation.toString(), 0);
            calibratedBalance[orientation.ordinal()] =
                    prefs.getFloat(SAVED_BALANCE + orientation.toString(), 0);
            calibratedValueCount[orientation.ordinal()] =
                    prefs.getInt(SAVED_COUNT + orientation.toString(), 0);
        }

        // register listener and start listening
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        running = true;
        for (int sensorType : getRequiredSensors()) {
            List<Sensor> sensors = sensorManager.getSensorList(sensorType);
            if (sensors.size() > 0) {
                Sensor sensor = sensors.get(0);
                running = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL) && running;
            }
        }
        if (running) {
            listener = orientationListener;
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {

        oldPitch = pitch;
        oldRoll = roll;
        oldBalance = balance;

        SensorManager.getRotationMatrix(R, I, event.values, MAG);

        // compute pitch, roll & balance
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

        // normalize z on ux, uy
        tmp = (float) Math.sqrt(outR[8] * outR[8] + outR[9] * outR[9]);
        tmp = (tmp == 0 ? 0 : outR[8] / tmp);

        // LOC[0] compass
        pitch = (float) Math.toDegrees(LOC[1]);
        roll = -(float) Math.toDegrees(LOC[2]);
        balance = (float) Math.toDegrees(Math.asin(tmp));

        // calculating minimal sensor step
        if (oldRoll != roll || oldPitch != pitch || oldBalance != balance) {
            if (oldPitch != pitch) {
                minStep = Math.min(minStep, Math.abs(pitch - oldPitch));
            }
            if (oldRoll != roll) {
                minStep = Math.min(minStep, Math.abs(roll - oldRoll));
            }
            if (oldBalance != balance) {
                minStep = Math.min(minStep, Math.abs(balance - oldBalance));
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

            // Allow for 2 calibration values to be entered.
            float cpitch = calibratedPitch[orientation.ordinal()];
            float croll = calibratedRoll[orientation.ordinal()];
            float cbalance = calibratedBalance[orientation.ordinal()];
            int ccount = calibratedValueCount[orientation.ordinal()];

            // More than 2 values is not allowed - reset calibration first.
            if (ccount >= 2) {
                ccount = 0;
                cpitch = 0;
                croll = 0;
                cbalance = 0;
            }
            ccount += 1;
            if (cpitch != 0.0f) {
                pitch = (pitch + cpitch) / (float)ccount;
            }
            if (croll != 0.0f) {
                roll = (roll + croll) / (float)ccount;
            }
            if (cbalance != 0.0f) {
                balance = (balance + cbalance) / (float)ccount;
            }

            Editor editor = Level.getContext().getPreferences(Context.MODE_PRIVATE).edit();
            editor.putFloat(SAVED_PITCH + orientation.toString(), pitch);
            editor.putFloat(SAVED_ROLL + orientation.toString(), roll);
            editor.putFloat(SAVED_BALANCE + orientation.toString(), balance);
            editor.putInt(SAVED_COUNT + orientation.toString(), ccount);
            final boolean success = editor.commit();
            if (success) {
                calibratedPitch[orientation.ordinal()] = pitch;
                calibratedRoll[orientation.ordinal()] = roll;
                calibratedBalance[orientation.ordinal()] = balance;
                calibratedValueCount[orientation.ordinal()] = ccount;
            }
            listener.onCalibrationSaved(success,ccount);
        }

        //  apply calibration to return values (keep internal values untouched!)
        float opitch = pitch - calibratedPitch[orientation.ordinal()];
        float oroll = roll - calibratedRoll[orientation.ordinal()];
        float obalance = balance - calibratedBalance[orientation.ordinal()];

        // propagation of the orientation
        listener.onOrientationChanged(orientation, opitch, oroll, obalance);
    }

    /**
     * Tell the provider to restore the calibration
     * to the default factory values
     */
    public final void resetCalibration() {
        boolean success = false;
        try {
            success = Level.getContext().getPreferences(
                    Context.MODE_PRIVATE).edit().clear().commit();
        } catch (Exception e) {
        }
        if (success) {
            Arrays.fill(calibratedPitch, 0);
            Arrays.fill(calibratedRoll, 0);
            Arrays.fill(calibratedBalance, 0);
            Arrays.fill(calibratedValueCount, 0);
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
     *
     * @return the minimal sensor step
     * 0 if not yet known
     */
    public float getSensibility() {
        if (refValues >= MIN_VALUES) {
            return minStep;
        } else {
            return 0;
        }
    }
}
