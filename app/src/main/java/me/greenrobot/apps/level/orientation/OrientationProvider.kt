package me.greenrobot.apps.level.orientation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.Surface
import androidx.appcompat.app.AppCompatActivity
import java.util.Arrays
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.min
import kotlin.math.sqrt

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
class OrientationProvider(context: Context?) : SensorEventListener {
    /**
     * Calibration
     */
    private val calibratedPitch = FloatArray(5)
    private val calibratedRoll = FloatArray(5)
    private val calibratedBalance = FloatArray(5)

    /**
     * Rotation Matrix
     */
    private val MAG = floatArrayOf(1f, 1f, 1f)
    private val I = FloatArray(16)
    private val R = FloatArray(16)
    private val outR = FloatArray(16)
    private val LOC = FloatArray(3)

    /**
     * Orientation
     */
    private var pitch = 0f
    private var roll = 0f
    private var displayOrientation = 0
    private var sensor: Sensor? = null
    private var sensorManager: SensorManager? = null
    private var listener: OrientationListener? = null

    /**
     * indicates whether or not Accelerometer Sensor is supported
     */
    var isSupported: Boolean? = null
        /**
         * Returns true if at least one Accelerometer sensor is available
         */
        get() {
            if (field == null) {
                if (activity != null) {
                    sensorManager =
                        activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
                    var supported = true
                    for (sensorType in requiredSensors) {
                        val sensors = sensorManager!!.getSensorList(sensorType)
                        supported = (sensors.size > 0) && supported
                    }
                    field = supported
                    return supported
                }
            }
            return field
        }
        private set
    /**
     * Returns true if the manager is listening to orientation changes
     */
    /**
     * indicates whether or not Accelerometer Sensor is running
     */
    var isListening: Boolean = false
        private set
    private var calibrating = false
    private var balance = 0f
    private var tmp = 0f
    private var oldPitch = 0f
    private var oldRoll = 0f
    private var oldBalance = 0f
    private var minStep = 360f
    private var refValues = 0f
    private var orientation: Orientation? = null
    private var locked = false
    private val activity = context as AppCompatActivity?

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            this.displayOrientation = activity!!.display!!.rotation
        } else {
            this.displayOrientation = activity!!.windowManager.defaultDisplay.rotation
        }
    }

    /**
     * Unregisters listeners
     */
    fun stopListening() {
        isListening = false
        try {
            if (sensorManager != null) {
                sensorManager!!.unregisterListener(this)
            }
        } catch (e: Exception) {
        }
    }

    private val requiredSensors: List<Int>
        get() = listOf(
            Sensor.TYPE_ACCELEROMETER
        )


    /**
     * Registers a listener and start listening
     * callback for accelerometer events
     */
    fun startListening(orientationListener: OrientationListener?) {
        // load calibration
        calibrating = false
        Arrays.fill(calibratedPitch, 0f)
        Arrays.fill(calibratedRoll, 0f)
        Arrays.fill(calibratedBalance, 0f)
        val prefs = activity!!.getPreferences(Context.MODE_PRIVATE)
        for (orientation in Orientation.entries) {
            calibratedPitch[orientation.ordinal] =
                prefs.getFloat(SAVED_PITCH + orientation.toString(), 0f)
            calibratedRoll[orientation.ordinal] =
                prefs.getFloat(SAVED_ROLL + orientation.toString(), 0f)
            calibratedBalance[orientation.ordinal] =
                prefs.getFloat(SAVED_BALANCE + orientation.toString(), 0f)
        }
        // register listener and start listening
        sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        isListening = true
        for (sensorType in requiredSensors) {
            val sensors = sensorManager!!.getSensorList(sensorType)
            if (sensors.size > 0) {
                sensor = sensors[0]
                isListening = sensorManager!!.registerListener(
                    this,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                ) && isListening
            }
        }
        if (isListening) {
            listener = orientationListener
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent) {
        oldPitch = pitch
        oldRoll = roll
        oldBalance = balance

        SensorManager.getRotationMatrix(R, I, event.values, MAG)

        when (displayOrientation) {
            Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(
                R,
                SensorManager.AXIS_MINUS_Y,
                SensorManager.AXIS_X,
                outR
            )

            Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(
                R,
                SensorManager.AXIS_MINUS_X,
                SensorManager.AXIS_MINUS_Y,
                outR
            )

            Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(
                R,
                SensorManager.AXIS_Y,
                SensorManager.AXIS_MINUS_X,
                outR
            )

            Surface.ROTATION_0 -> SensorManager.remapCoordinateSystem(
                R,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Y,
                outR
            )

            else -> SensorManager.remapCoordinateSystem(
                R,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Y,
                outR
            )
        }
        SensorManager.getOrientation(outR, LOC)

        // normalize z on ux, uy
        tmp = sqrt((outR[8] * outR[8] + outR[9] * outR[9]).toDouble()).toFloat()
        tmp = (if (tmp == 0f) 0f else outR[8] / tmp)

        // LOC[0] compass
        pitch = Math.toDegrees(LOC[1].toDouble()).toFloat()
        roll = -Math.toDegrees(LOC[2].toDouble()).toFloat()
        balance = Math.toDegrees(asin(tmp.toDouble())).toFloat()

        // calculating minimal sensor step
        if (oldRoll != roll || oldPitch != pitch || oldBalance != balance) {
            if (oldPitch != pitch) {
                minStep = min(minStep.toDouble(), abs((pitch - oldPitch).toDouble()))
                    .toFloat()
            }
            if (oldRoll != roll) {
                minStep = min(minStep.toDouble(), abs((roll - oldRoll).toDouble()))
                    .toFloat()
            }
            if (oldBalance != balance) {
                minStep = min(minStep.toDouble(), abs((balance - oldBalance).toDouble()))
                    .toFloat()
            }
            if (refValues < MIN_VALUES) {
                refValues++
            }
        }

        if (!locked || orientation == null) {
            orientation = if (pitch < -45 && pitch > -135) {
                // top side up
                Orientation.TOP
            } else if (pitch > 45 && pitch < 135) {
                // bottom side up
                Orientation.BOTTOM
            } else if (roll > 45) {
                // right side up
                Orientation.RIGHT
            } else if (roll < -45) {
                // left side up
                Orientation.LEFT
            } else {
                // landing
                Orientation.LANDING
            }
        }

        if (calibrating) {
            calibrating = false
            val editor = activity!!.getPreferences(Context.MODE_PRIVATE).edit()
            editor.putFloat(SAVED_PITCH + orientation.toString(), pitch)
            editor.putFloat(SAVED_ROLL + orientation.toString(), roll)
            editor.putFloat(SAVED_BALANCE + orientation.toString(), balance)
            val success = editor.commit()
            if (success) {
                calibratedPitch[orientation!!.ordinal] = pitch
                calibratedRoll[orientation!!.ordinal] = roll
                calibratedBalance[orientation!!.ordinal] = balance
            }
            listener!!.onCalibrationSaved(success)
            pitch = 0f
            roll = 0f
            balance = 0f
        } else {
            pitch -= calibratedPitch[orientation!!.ordinal]
            roll -= calibratedRoll[orientation!!.ordinal]
            balance -= calibratedBalance[orientation!!.ordinal]
        }

        // propagation of the orientation
        listener!!.onOrientationChanged(orientation, pitch, roll, balance)
    }

    /**
     * Tell the provider to restore the calibration
     * to the default factory values
     */
    fun resetCalibration() {
        var success = false
        try {
            success = activity!!.getPreferences(
                Context.MODE_PRIVATE
            ).edit().clear().commit()
        } catch (e: Exception) {
        }
        if (success) {
            Arrays.fill(calibratedPitch, 0f)
            Arrays.fill(calibratedRoll, 0f)
            Arrays.fill(calibratedBalance, 0f)
        }
        if (listener != null) {
            listener!!.onCalibrationReset(success)
        }
    }


    /**
     * Tell the provider to save the calibration
     * The calibration is actually saved on the next
     * sensor change event
     */
    fun saveCalibration() {
        calibrating = true
    }

    fun setLocked(locked: Boolean) {
        this.locked = locked
    }

    val sensibility: Float
        /**
         * Return the minimal sensor step
         *
         * @return the minimal sensor step
         * 0 if not yet known
         */
        get() = if (refValues >= MIN_VALUES) {
            minStep
        } else {
            0f
        }

    companion object {
        private const val MIN_VALUES = 20

        /**
         * Calibration
         */
        private const val SAVED_PITCH = "pitch."
        private const val SAVED_ROLL = "roll."
        private const val SAVED_BALANCE = "balance."
        private var provider: OrientationProvider? = null
        fun getInstance(context: Context?): OrientationProvider? {
            if (provider == null) {
                provider = OrientationProvider(context)
            }
            return provider
        }
    }
}
