package me.greenrobot.apps.level.view

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.View.OnTouchListener
import me.greenrobot.apps.level.orientation.Orientation
import me.greenrobot.apps.level.painter.LevelPainter

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
class LevelView(context: Context?, attrs: AttributeSet?) : SurfaceView(context, attrs),
    SurfaceHolder.Callback, OnTouchListener {
    private var painter: LevelPainter? = null

    init {
        holder.addCallback(this)
        isFocusable = true
        setOnTouchListener(this)
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        if (painter != null) {
            painter!!.pause(!hasWindowFocus)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (painter != null) {
            painter!!.setSurfaceSize(width, height)
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (painter == null) {
            painter = LevelPainter(holder, context, Handler(Looper.getMainLooper()))
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (painter != null) {
            painter!!.pause(true)
            painter!!.clean()
            painter = null
        }
        // free resources
        System.gc()
    }

    fun onOrientationChanged(orientation: Orientation?, pitch: Float, roll: Float, balance: Float) {
        if (painter != null) {
            painter!!.onOrientationChanged(orientation!!, pitch, roll, balance)
        }
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && painter != null) {
            painter!!.onTouch(event.x.toInt(), event.y.toInt())
        }
        return true
    }
}
