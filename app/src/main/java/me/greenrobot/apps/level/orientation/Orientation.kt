package me.greenrobot.apps.level.orientation

import kotlin.math.abs

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
enum class Orientation(@JvmField val reverse: Int, @JvmField val rotation: Int) {
    LANDING(1, 0),
    TOP(1, 0),
    RIGHT(1, 90),
    BOTTOM(-1, 180),
    LEFT(-1, -90);

    fun isLevel(pitch: Float, roll: Float, balance: Float, sensibility: Float): Boolean {
        var sensibility = sensibility
        if (sensibility < 0.2f) {
            //minimum sensibility for playing sound (play sound even if sensor sensibility is better)
            sensibility = 0.2f
        }
        return when (this) {
            BOTTOM, TOP -> (balance <= sensibility
                    && balance >= -sensibility)

            LANDING -> roll <= sensibility && roll >= -sensibility && (abs(
                pitch.toDouble()
            ) <= sensibility
                    || abs(pitch.toDouble()) >= 180 - sensibility)

            LEFT, RIGHT -> (abs(
                pitch.toDouble()
            ) <= sensibility
                    || abs(pitch.toDouble()) >= 180 - sensibility)
        }
        return false
    }
}
