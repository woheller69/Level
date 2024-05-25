package me.greenrobot.apps.level.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatSeekBar

class VerticalSeekBar : AppCompatSeekBar {
    private var mOnSeekBarChangeListener: OnSeekBarChangeListener? = null

    constructor(context: Context?) : super(context!!)

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context!!, attrs, defStyle
    )

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    )

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(height, width, oldHeight, oldWidth)
    }

    @Synchronized
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec)
        setMeasuredDimension(measuredHeight, measuredWidth)
    }

    override fun onDraw(c: Canvas) {
        c.rotate(ROTATION_ANGLE.toFloat())
        c.translate(-height.toFloat(), 0f)

        super.onDraw(c)
    }

    override fun setOnSeekBarChangeListener(l: OnSeekBarChangeListener) {
        mOnSeekBarChangeListener = l
        super.setOnSeekBarChangeListener(l)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                progress = max - (max * event.y / height).toInt()
                mOnSeekBarChangeListener!!.onStartTrackingTouch(this)
            }

            MotionEvent.ACTION_MOVE -> progress = max - (max * event.y / height).toInt()
            MotionEvent.ACTION_UP -> {
                progress = max - (max * event.y / height).toInt()
                mOnSeekBarChangeListener!!.onStopTrackingTouch(this)
            }

            MotionEvent.ACTION_CANCEL -> mOnSeekBarChangeListener!!.onStopTrackingTouch(this)
            else -> {}
        }
        return true
    }

    override fun setProgress(progress: Int) {
        super.setProgress(progress)
        onSizeChanged(width, height, 0, 0)
    }

    companion object {
        private const val ROTATION_ANGLE = -90
    }
}

