package org.woheller69.level.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatSeekBar;


public class VerticalSeekBar extends AppCompatSeekBar {
    private static final int ROTATION_ANGLE = -90;

    private OnSeekBarChangeListener mOnSeekBarChangeListener;

    public VerticalSeekBar(final Context context) {
        super(context);
    }

    public VerticalSeekBar(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    public VerticalSeekBar(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected final void onSizeChanged(final int width, final int height, final int oldWidth, final int oldHeight) {
        super.onSizeChanged(height, width, oldHeight, oldWidth);
    }

    @Override
    protected final synchronized void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    @Override
    protected final void onDraw(final Canvas c) {
        c.rotate(ROTATION_ANGLE);
        c.translate(-getHeight(), 0);

        super.onDraw(c);
    }

    @Override
    public final void setOnSeekBarChangeListener(final OnSeekBarChangeListener l) {
        mOnSeekBarChangeListener = l;
        super.setOnSeekBarChangeListener(l);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public final boolean onTouchEvent(final MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setProgress(getMax() - (int) (getMax() * event.getY() / getHeight()));
                mOnSeekBarChangeListener.onStartTrackingTouch(this);
                break;

            case MotionEvent.ACTION_MOVE:
                setProgress(getMax() - (int) (getMax() * event.getY() / getHeight()));
                break;

            case MotionEvent.ACTION_UP:
                setProgress(getMax() - (int) (getMax() * event.getY() / getHeight()));
                mOnSeekBarChangeListener.onStopTrackingTouch(this);
                break;

            case MotionEvent.ACTION_CANCEL:
                mOnSeekBarChangeListener.onStopTrackingTouch(this);
                break;

            default:
                break;
        }

        return true;
    }

    @Override
    public final void setProgress(final int progress) {
        super.setProgress(progress);
        onSizeChanged(getWidth(), getHeight(), 0, 0);
    }
}

