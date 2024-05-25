package me.greenrobot.apps.level.painter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Handler
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import me.greenrobot.apps.level.Level.Companion.provider
import me.greenrobot.apps.level.R
import me.greenrobot.apps.level.orientation.Orientation
import me.greenrobot.apps.level.util.PreferenceHelper
import me.greenrobot.apps.level.util.PreferenceHelper.displayTypeBackgroundText
import me.greenrobot.apps.level.util.PreferenceHelper.displayTypeFormat
import me.greenrobot.apps.level.util.PreferenceHelper.displayTypeMax
import me.greenrobot.apps.level.util.PreferenceHelper.isDisplayTypeInclination
import me.greenrobot.apps.level.util.PreferenceHelper.orientationLocked
import me.greenrobot.apps.level.util.PreferenceHelper.viscosityCoefficient
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

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
class LevelPainter(
    /**
     * Possesseur de la surface
     */
    private val surfaceHolder: SurfaceHolder, context: Context,
    /**
     * Animation
     */
    private val handler: Handler
) : Runnable {
    /**
     * Etats du thread
     */
    private var initialized: Boolean
    private var wait: Boolean
    private var canvasWidth = 0
    private var canvasHeight = 0
    private var minLevelX = 0
    private var maxLevelX = 0
    private var levelWidth = 0
    private var levelHeight = 0
    private var levelMinusBubbleWidth = 0
    private var levelMinusBubbleHeight = 0
    private var middleX = 0
    private var middleY = 0
    private var halfBubbleWidth = 0
    private var halfBubbleHeight = 0
    private var halfMarkerGap = 0
    private var minLevelY = 0
    private var maxLevelY = 0
    private var minBubble = 0
    private var maxBubble = 0
    private val markerThickness: Int
    private val levelBorderWidth: Int
    private val levelBorderHeight: Int
    private val infoHeight: Int
    private val lcdWidth: Int
    private val lcdHeight: Int
    private val lockWidth: Int
    private val lockHeight: Int
    private val displayPadding: Int
    private val displayGap: Int
    private var infoY = 0
    private var sensorY = 0
    private val sensorGap: Int
    private val arrowWidth: Int
    private val arrowPadding: Int
    private var levelMaxDimension = 0

    /**
     * Rect
     */
    private val displayRect: Rect
    private val lockRect: Rect

    /**
     * Angles
     */
    private var angle1 = 0f
    private var angle1raw = 0f
    private var angle2 = 0f
    private var angle2raw = 0f

    /**
     * Orientation
     */
    private var orientation: Orientation

    /**
     * Bubble physics
     */
    private var currentTime: Long = 0
    private var lastTime: Long = 0
    private var timeDiff = 0.0
    private var posX = 0.0
    private var posY = 0.0
    private var angleX = 0.0
    private var angleY = 0.0
    private var speedX = 0.0
    private var speedY = 0.0
    private var x = 0.0
    private var y = 0.0

    /**
     * Drawables
     */
    private var level1D: Drawable?
    private var bubble1D: Drawable?
    private var marker1D: Drawable?
    private var level2D: Drawable?
    private var bubble2D: Drawable?
    private var marker2D: Drawable?
    private var display: Drawable?

    /**
     * Info
     */
    private val infoText: String
    private val lockText: String

    /**
     * Ajustement de la vitesse
     */
    private var viscosityValue = 0.0

    /**
     * Format des angles
     */
    private val displayFormat: DecimalFormat
    private val displayBackgroundText: String
    private val lcdForegroundPaint: Paint
    private val lcdBackgroundPaint: Paint
    private val lockForegroundPaint: Paint
    private val lockBackgroundPaint: Paint
    private val infoPaint: Paint
    private val blackPaint: Paint
    private val backgroundColor: Int

    /**
     * Config angles
     */
    private val showAngle: Boolean
    private val lockEnabled: Boolean
    private var locked: Boolean

    // get handles to some important objects

    // economy mode
    private val frameRate = (1000 / context.resources.getInteger(R.integer.frame_rate)).toLong()

    init {
        // drawable
        this.level1D = ContextCompat.getDrawable(context, R.drawable.level_1d)
        this.level2D = ContextCompat.getDrawable(context, R.drawable.level_2d)
        this.bubble1D = ContextCompat.getDrawable(context, R.drawable.bubble)
        this.bubble2D = ContextCompat.getDrawable(context, R.drawable.bubble)
        this.marker1D = ContextCompat.getDrawable(context, R.drawable.marker_1d)
        this.marker2D = ContextCompat.getDrawable(context, R.drawable.marker_2d)
        this.display = ContextCompat.getDrawable(context, R.drawable.display)

        // config
        this.showAngle = PreferenceHelper.showAngle
        this.displayFormat = DecimalFormat(displayTypeFormat)
        this.displayBackgroundText = displayTypeBackgroundText

        // colors
        this.backgroundColor = ContextCompat.getColor(context, R.color.silver)

        // strings
        this.infoText = context.getString(R.string.calibrate_info)
        this.lockText = context.getString(R.string.lock_info)

        // typeface
        val lcd = Typeface.createFromAsset(context.assets, FONT_LCD)

        // paint
        this.infoPaint = Paint()
        infoPaint.color = ContextCompat.getColor(context, R.color.black)
        infoPaint.isAntiAlias = true
        infoPaint.textSize = context.resources.getDimensionPixelSize(R.dimen.info_text).toFloat()
        infoPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL))
        infoPaint.textAlign = Paint.Align.CENTER

        this.lcdForegroundPaint = Paint()
        lcdForegroundPaint.color = ContextCompat.getColor(context, R.color.lcd_front)
        lcdForegroundPaint.isAntiAlias = true
        lcdForegroundPaint.textSize =
            context.resources.getDimensionPixelSize(R.dimen.lcd_text).toFloat()
        lcdForegroundPaint.setTypeface(lcd)
        lcdForegroundPaint.textAlign = Paint.Align.CENTER

        this.lcdBackgroundPaint = Paint()
        lcdBackgroundPaint.color = ContextCompat.getColor(context, R.color.lcd_back)
        lcdBackgroundPaint.isAntiAlias = true
        lcdBackgroundPaint.textSize =
            context.resources.getDimensionPixelSize(R.dimen.lcd_text).toFloat()
        lcdBackgroundPaint.setTypeface(lcd)
        lcdBackgroundPaint.textAlign = Paint.Align.CENTER

        this.lockForegroundPaint = Paint()
        lockForegroundPaint.color = ContextCompat.getColor(context, R.color.lock_front)
        lockForegroundPaint.isAntiAlias = true
        lockForegroundPaint.textSize =
            context.resources.getDimensionPixelSize(R.dimen.lock_text).toFloat()
        lockForegroundPaint.setTypeface(lcd)
        lockForegroundPaint.textAlign = Paint.Align.CENTER

        this.lockBackgroundPaint = Paint()
        lockBackgroundPaint.color = ContextCompat.getColor(context, R.color.lock_back)
        lockBackgroundPaint.isAntiAlias = true
        lockBackgroundPaint.textSize =
            context.resources.getDimensionPixelSize(R.dimen.lock_text).toFloat()
        lockBackgroundPaint.setTypeface(lcd)
        lockBackgroundPaint.textAlign = Paint.Align.CENTER

        this.blackPaint = Paint()
        blackPaint.color = ContextCompat.getColor(context, R.color.black)
        blackPaint.isAntiAlias = true
        blackPaint.textSize = context.resources.getDimensionPixelSize(R.dimen.lcd_text).toFloat()
        blackPaint.setTypeface(lcd)
        blackPaint.textAlign = Paint.Align.CENTER

        // dimens
        val rect = Rect()
        infoPaint.getTextBounds(infoText, 0, infoText.length, rect)
        this.infoHeight = rect.height()
        lcdBackgroundPaint.getTextBounds(
            displayBackgroundText,
            0,
            displayBackgroundText.length,
            rect
        )
        this.lcdHeight = rect.height()
        this.lcdWidth = rect.width()
        lcdBackgroundPaint.getTextBounds("\u25b6", 0, 1, rect)
        this.arrowPadding = context.resources.getDimensionPixelSize(R.dimen.arrow_padding)
        this.arrowWidth = rect.width() + arrowPadding
        lockBackgroundPaint.getTextBounds(LOCKED, 0, LOCKED.length, rect)
        this.lockHeight = rect.height()
        this.lockWidth = rect.width()
        this.levelBorderWidth = context.resources.getDimensionPixelSize(R.dimen.level_border_width)
        this.levelBorderHeight =
            context.resources.getDimensionPixelSize(R.dimen.level_border_height)
        this.markerThickness = context.resources.getDimensionPixelSize(R.dimen.marker_thickness)
        this.displayGap = context.resources.getDimensionPixelSize(R.dimen.display_gap)
        this.sensorGap = context.resources.getDimensionPixelSize(R.dimen.sensor_gap)
        this.displayPadding = context.resources.getDimensionPixelSize(R.dimen.display_padding)
        this.displayRect = Rect()
        this.lockRect = Rect()

        // init
        this.locked = false
        provider!!.setLocked(this.locked)
        this.lockEnabled = orientationLocked
        this.orientation = Orientation.TOP
        this.wait = true
        this.initialized = false
    }

    fun clean() {
        // suppression des ressources
        // afin de bypasser les problemes
        // de cache des xml drawable
        synchronized(this.surfaceHolder) {
            level1D = null
            level2D = null
            bubble1D = null
            bubble2D = null
            marker1D = null
            marker2D = null
            display = null
        }
    }

    override fun run() {
        var c: Canvas? = null
        updatePhysics()
        try {
            c = surfaceHolder.lockCanvas(null)
            if (c != null) {
                synchronized(this.surfaceHolder) {
                    doDraw(c)
                }
            }
        } finally {
            // do this in a finally so that if an exception is thrown
            // during the above, we don't leave the Surface in an
            // inconsistent state
            if (c != null) {
                surfaceHolder.unlockCanvasAndPost(c)
            }
        }
        // lancement du traitement differe en mode eco
        handler.removeCallbacks(this)
        if (!wait) {
            handler.postDelayed(this, frameRate - System.currentTimeMillis() + lastTime)
        }
    }

    /**
     * Mise en pause du thread
     */
    fun pause(paused: Boolean) {
        wait = !initialized || paused
        // si on est en mode eco et
        // que la pause est supprimee
        // relance du traitement
        if (!wait) {
            handler.postDelayed(this, frameRate)
        }
    }

    /**
     * Modification / initialisation de la taille de l'ecran
     */
    fun setSurfaceSize(width: Int, height: Int) {
        canvasWidth = width
        canvasHeight = height

        levelMaxDimension = min(
            (min(height.toDouble(), width.toDouble()) - 2 * displayGap).toDouble(),
            (max(
                height.toDouble(),
                width.toDouble()
            ) - 2 * (sensorGap + 2 * infoHeight + 3 * displayGap + lcdHeight)).toDouble()
        ).toInt()

        setOrientation(orientation)
    }

    private fun updatePhysics() {
        currentTime = System.currentTimeMillis()
        if (lastTime > 0) {
            timeDiff = (currentTime - lastTime) / 1000.0
            posX = orientation.reverse * (2 * x - minLevelX - maxLevelX) / levelMinusBubbleWidth
            when (orientation) {
                Orientation.TOP, Orientation.BOTTOM -> speedX =
                    orientation.reverse * (2 * angleX - posX) * viscosityValue

                Orientation.LEFT, Orientation.RIGHT -> speedX =
                    orientation.reverse * (2 * angleY - posX) * viscosityValue

                Orientation.LANDING -> {
                    posY = (2 * y - minLevelY - maxLevelY) / levelMinusBubbleHeight
                    speedX = (2 * angleX - posX) * viscosityValue
                    speedY = (2 * angleY - posY) * viscosityValue
                    y += speedY * timeDiff
                }
            }
            x += speedX * timeDiff

            /*
             * Keep the bubble inside of the circle.
             */
            if (orientation == Orientation.LANDING) {
                val r = sqrt((middleX - x) * (middleX - x) + (middleY - y) * (middleY - y))
                val rm = (levelMaxDimension / 2.0f - halfBubbleWidth - levelBorderWidth).toDouble()
                if (r > rm) {
                    x = (x - middleX) * rm / r + middleX
                    y = (y - middleY) * rm / r + middleY
                }
            } else {
                val r = abs(middleX - x)
                val rm = (levelWidth / 2.0f - halfBubbleWidth - levelBorderWidth).toDouble()
                if (r > rm) {
                    x = (x - middleX) * rm / r + middleX
                }
            }
        }
        lastTime = currentTime
    }

    private fun doDraw(canvas: Canvas) {
        canvas.save()

        // decouple display speed from sensor speed
        if ((System.currentTimeMillis() - angleDispUpdateTime) > angleDispInterval) {
            angle1DispValue = angle1
            angle2DispValue = angle2
            angleDispUpdateTime = System.currentTimeMillis()
        }

        canvas.drawColor(backgroundColor)

        if (orientation == Orientation.LANDING) {
            //canvas.drawText(infoText, middleX, infoY, infoPaint);
            if (lockEnabled) {
                display!!.bounds = lockRect
                display!!.draw(canvas)
                canvas.drawText(
                    LOCKED_BACKGROUND,
                    middleX.toFloat(),
                    lockRect.centerY() + lockHeight / 2.0f,
                    lockBackgroundPaint
                )
                canvas.drawText(
                    lockText,
                    middleX.toFloat(),
                    (lockRect.bottom + displayGap).toFloat(),
                    infoPaint
                )
                if (locked) {
                    canvas.drawText(
                        LOCKED,
                        middleX.toFloat(),
                        lockRect.centerY() + lockHeight / 2.0f,
                        lockForegroundPaint
                    )
                }
            }
            if (showAngle) {
                display!!.setBounds(
                    middleX - (displayRect.width() + displayGap),
                    displayRect.top,
                    middleX - displayGap,
                    displayRect.bottom
                )
                display!!.draw(canvas)
                display!!.setBounds(
                    middleX + displayGap,
                    displayRect.top,
                    middleX + displayRect.width() + displayGap,
                    displayRect.bottom
                )
                display!!.draw(canvas)
                canvas.drawText(
                    displayBackgroundText,
                    middleX - (displayRect.width() + displayGap - arrowWidth) / 2.0f - displayPadding,
                    displayRect.centerY() + lcdHeight / 2.0f,
                    lcdBackgroundPaint
                )
                canvas.drawText(
                    displayFormat.format(angle2DispValue.toDouble()),
                    middleX - (displayRect.width() + displayGap - arrowWidth) / 2.0f - displayPadding,
                    displayRect.centerY() + lcdHeight / 2.0f,
                    lcdForegroundPaint
                )
                if (angle2raw > 0.1f) {
                    canvas.drawText( //left-right →
                        "\u25b6",
                        middleX - (displayRect.width() + displayGap) + arrowWidth / 2.0f + displayPadding / 2.0f,
                        displayRect.centerY() + lcdHeight / 2.0f,
                        lcdForegroundPaint
                    )
                } else if (angle2raw < -0.1f) {
                    canvas.drawText( //left-right ←
                        "\u25c0",
                        middleX - (displayRect.width() + displayGap) + arrowWidth / 2.0f + displayPadding / 2.0f,
                        displayRect.centerY() + lcdHeight / 2.0f,
                        lcdForegroundPaint
                    )
                }
                canvas.drawText(
                    displayBackgroundText,
                    middleX + displayGap + (displayRect.width() - arrowWidth) / 2.0f,
                    displayRect.centerY() + lcdHeight / 2.0f,
                    lcdBackgroundPaint
                )
                canvas.drawText(
                    displayFormat.format(angle1DispValue.toDouble()),
                    middleX + displayGap + (displayRect.width() - arrowWidth) / 2.0f,
                    displayRect.centerY() + lcdHeight / 2.0f,
                    lcdForegroundPaint
                )
                if (angle1raw > 0.1f) {
                    canvas.drawText( //up-down ↓
                        "\u25bc",
                        middleX + displayGap + (displayRect.width() - arrowWidth) / 2.0f + displayRect.width() / 2.0f - displayPadding / 2.0f,
                        displayRect.centerY() + lcdHeight / 2.0f,
                        lcdForegroundPaint
                    )
                } else if (angle1raw < -0.1f) {
                    canvas.drawText( //up-down ↑
                        "\u25b2",
                        middleX + displayGap + (displayRect.width() - arrowWidth) / 2.0f + displayRect.width() / 2.0f - displayPadding / 2.0f,
                        displayRect.centerY() + lcdHeight / 2.0f,
                        lcdForegroundPaint
                    )
                }
            }
            bubble2D!!.setBounds(
                (x - halfBubbleWidth).toInt(),
                (y - halfBubbleHeight).toInt(),
                (x + halfBubbleWidth).toInt(),
                (y + halfBubbleHeight).toInt()
            )
            level2D!!.draw(canvas)
            bubble2D!!.draw(canvas)
            marker2D!!.draw(canvas)
            canvas.drawLine(
                minLevelX.toFloat(), middleY.toFloat(),
                (middleX - halfMarkerGap).toFloat(), middleY.toFloat(), infoPaint
            )
            canvas.drawLine(
                (middleX + halfMarkerGap).toFloat(), middleY.toFloat(),
                maxLevelX.toFloat(), middleY.toFloat(), infoPaint
            )
            canvas.drawLine(
                middleX.toFloat(), minLevelY.toFloat(),
                middleX.toFloat(), (middleY - halfMarkerGap).toFloat(), infoPaint
            )
            canvas.drawLine(
                middleX.toFloat(), (middleY + halfMarkerGap).toFloat(),
                middleX.toFloat(), maxLevelY.toFloat(), infoPaint
            )
        } else {
            canvas.rotate(orientation.rotation.toFloat(), middleX.toFloat(), middleY.toFloat())
            //canvas.drawText(infoText, middleX, infoY, infoPaint);
            if (lockEnabled) {
                display!!.bounds = lockRect
                display!!.draw(canvas)
                canvas.drawText(
                    LOCKED_BACKGROUND,
                    middleX.toFloat(),
                    lockRect.centerY() + lockHeight / 2.0f,
                    lockBackgroundPaint
                )
                canvas.drawText(
                    lockText,
                    middleX.toFloat(),
                    (lockRect.bottom + displayGap).toFloat(),
                    infoPaint
                )
                if (locked) {
                    canvas.drawText(
                        LOCKED,
                        middleX.toFloat(),
                        lockRect.centerY() + lockHeight / 2.0f,
                        lockForegroundPaint
                    )
                }
            }
            if (showAngle) {
                display!!.bounds = displayRect
                display!!.draw(canvas)
                canvas.drawText(
                    displayBackgroundText,
                    middleX - arrowWidth / 2.0f,
                    displayRect.centerY() + lcdHeight / 2.0f,
                    lcdBackgroundPaint
                )
                canvas.drawText(
                    displayFormat.format(angle1DispValue.toDouble()),
                    middleX - arrowWidth / 2.0f,
                    displayRect.centerY() + lcdHeight / 2.0f,
                    lcdForegroundPaint
                )

                if (angle1raw > 0.1f) {
                    if (orientation.reverse == 1) {
                        canvas.drawText(
                            "\u25bc",
                            middleX - arrowWidth / 2.0f + displayRect.width() / 2.0f - displayPadding / 2.0f,
                            displayRect.centerY() + lcdHeight / 2.0f,
                            lcdForegroundPaint
                        )
                    } else {
                        canvas.drawText(
                            "\u25b2",
                            middleX - arrowWidth / 2.0f + displayRect.width() / 2.0f - displayPadding / 2.0f,
                            displayRect.centerY() + lcdHeight / 2.0f,
                            lcdForegroundPaint
                        )
                    }
                } else if (angle1raw < -0.1f) {
                    if (orientation.reverse == 1) {
                        canvas.drawText(
                            "\u25b2",
                            middleX - arrowWidth / 2.0f + displayRect.width() / 2.0f - displayPadding / 2.0f,
                            displayRect.centerY() + lcdHeight / 2.0f,
                            lcdForegroundPaint
                        )
                    } else {
                        canvas.drawText(
                            "\u25bc",
                            middleX - arrowWidth / 2.0f + displayRect.width() / 2.0f - displayPadding / 2.0f,
                            displayRect.centerY() + lcdHeight / 2.0f,
                            lcdForegroundPaint
                        )
                    }
                }
            }
            // level
            level1D!!.draw(canvas)
            // bubble
            canvas.clipRect(
                minLevelX + levelBorderWidth,
                minLevelY + levelBorderHeight,
                maxLevelX - levelBorderWidth,
                maxLevelY - levelBorderHeight
            )
            bubble1D!!.setBounds(
                (x - halfBubbleWidth).toInt(),
                minBubble,
                (x + halfBubbleWidth).toInt(),
                maxBubble
            )
            bubble1D!!.draw(canvas)
            // marker
            marker1D!!.setBounds(
                middleX - halfMarkerGap - markerThickness,
                minLevelY,
                middleX - halfMarkerGap,
                maxLevelY
            )
            marker1D!!.draw(canvas)
            marker1D!!.setBounds(
                middleX + halfMarkerGap,
                minLevelY,
                middleX + halfMarkerGap + markerThickness,
                maxLevelY
            )
            marker1D!!.draw(canvas)
        }

        canvas.restore()
    }

    private fun setOrientation(newOrientation: Orientation) {
        if (!(lockEnabled && locked) || !initialized) {
            synchronized(this.surfaceHolder) {
                orientation = newOrientation
                /**
                 * Dimensions
                 */
                /**
                 * Dimensions
                 */
                val height: Int
                val width: Int
                when (newOrientation) {
                    Orientation.LEFT, Orientation.RIGHT -> {
                        height = canvasWidth
                        width = canvasHeight
                        infoY = (canvasHeight - canvasWidth) / 2 + canvasWidth - infoHeight
                    }

                    Orientation.TOP, Orientation.BOTTOM -> {
                        height = canvasHeight
                        width = canvasWidth
                        infoY = canvasHeight - infoHeight
                    }

                    else -> {
                        height = canvasHeight
                        width = canvasWidth
                        infoY = canvasHeight - infoHeight
                    }
                }
                sensorY = infoY - infoHeight - sensorGap

                middleX = canvasWidth / 2
                middleY = canvasHeight / 2

                when (newOrientation) {
                    Orientation.LANDING -> {
                        levelWidth = levelMaxDimension
                        levelHeight = levelMaxDimension
                    }

                    Orientation.TOP, Orientation.BOTTOM, Orientation.LEFT, Orientation.RIGHT -> {
                        levelWidth = (width - 2 * displayGap)
                        levelHeight = (levelWidth * LEVEL_ASPECT_RATIO).toInt()
                    }
                }
                viscosityValue = levelWidth * viscosityCoefficient

                minLevelX = middleX - levelWidth / 2
                maxLevelX = middleX + levelWidth / 2
                minLevelY = middleY - levelHeight / 2
                maxLevelY = middleY + levelHeight / 2

                // bubble
                halfBubbleWidth = (levelWidth * BUBBLE_WIDTH / 2).toInt()
                halfBubbleHeight = (halfBubbleWidth * BUBBLE_ASPECT_RATIO).toInt()
                val bubbleWidth = 2 * halfBubbleWidth
                val bubbleHeight = 2 * halfBubbleHeight
                maxBubble = (maxLevelY - bubbleHeight * BUBBLE_CROPPING).toInt()
                minBubble = maxBubble - bubbleHeight

                // display
                if (orientation == Orientation.LANDING) {
                    displayRect[middleX - lcdWidth / 2 - arrowWidth / 2 - displayPadding, sensorY - displayGap - 2 * displayPadding - lcdHeight - infoHeight / 2, middleX + lcdWidth / 2 + displayPadding + arrowWidth / 2] =
                        sensorY - displayGap - infoHeight / 2
                } else {
                    displayRect[middleX - arrowWidth / 2 - lcdWidth / 2 - displayPadding, sensorY - displayGap - 2 * displayPadding - lcdHeight - infoHeight / 2, middleX + lcdWidth / 2 + displayPadding + arrowWidth / 2] =
                        sensorY - displayGap - infoHeight / 2
                }
                // lock
                lockRect[middleX - lockWidth / 2 - displayPadding, middleY - height / 2 + displayGap, middleX + lockWidth / 2 + displayPadding] =
                    middleY - height / 2 + displayGap + 2 * displayPadding + lockHeight

                // marker
                halfMarkerGap = (levelWidth * MARKER_GAP / 2).toInt()

                // autres
                levelMinusBubbleWidth = levelWidth - bubbleWidth - 2 * levelBorderWidth
                levelMinusBubbleHeight = levelHeight - bubbleHeight - 2 * levelBorderWidth

                // positionnement
                level1D!!.setBounds(minLevelX, minLevelY, maxLevelX, maxLevelY)
                level2D!!.setBounds(minLevelX, minLevelY, maxLevelX, maxLevelY)
                marker2D!!.setBounds(
                    middleX - halfMarkerGap - markerThickness,
                    middleY - halfMarkerGap - markerThickness,
                    middleX + halfMarkerGap + markerThickness,
                    middleY + halfMarkerGap + markerThickness
                )

                x = ((maxLevelX + minLevelX).toDouble()) / 2
                y = ((maxLevelY + minLevelY).toDouble()) / 2
                if (!initialized) {
                    initialized = true
                    pause(false)
                }
            }
        }
    }

    fun onOrientationChanged(
        newOrientation: Orientation,
        newPitch: Float,
        newRoll: Float,
        newBalance: Float
    ) {
        if (orientation != newOrientation) {
            setOrientation(newOrientation)
        }
        if (!wait) {
            when (orientation) {
                Orientation.TOP, Orientation.BOTTOM -> {
                    angle1raw = angle1raw * 0.7f + newBalance * 0.3f
                    angle1 = abs(angle1raw.toDouble()).toFloat()
                    angleX =
                        angleX * 0.7f + (sin(Math.toRadians(newBalance.toDouble())) / MAX_SINUS) * 0.3f
                }

                Orientation.LANDING -> {
                    angle2raw = angle2raw * 0.7f + newRoll * 0.3f
                    angle2 = abs(angle2raw.toDouble()).toFloat()
                    angleX =
                        angleX * 0.7f + (sin(Math.toRadians(newRoll.toDouble())) / MAX_SINUS) * 0.3f
                    angle1raw = angle1raw * 0.7f + newPitch * 0.3f
                    angle1 = abs(angle1raw.toDouble()).toFloat()
                    angleY =
                        angleY * 0.7f + (sin(Math.toRadians(newPitch.toDouble())) / MAX_SINUS) * 0.3f
                    if (angle1 > 90) {
                        angle1 = 180 - angle1
                    }
                }

                Orientation.RIGHT, Orientation.LEFT -> {
                    angle1raw = angle1raw * 0.7f + newPitch * 0.3f
                    angle1 = abs(angle1raw.toDouble()).toFloat()
                    angleY =
                        angleY * 0.7f + (sin(Math.toRadians(newPitch.toDouble())) / MAX_SINUS) * 0.3f
                    if (angle1 > 90) {
                        angle1 = 180 - angle1
                    }
                }
            }
            if (isDisplayTypeInclination) {
                angle1 = (100 * tan(angle1 / 360 * 2 * Math.PI)).toFloat()
                angle2 = (100 * tan(angle2 / 360 * 2 * Math.PI)).toFloat()
            }
            // correction des angles affiches
            val angleTypeMax = displayTypeMax
            if (angle1 > angleTypeMax) {
                angle1 = angleTypeMax
            }
            if (angle2 > angleTypeMax) {
                angle2 = angleTypeMax
            }
            // correction des angles aberrants
            // pour ne pas que la bulle sorte de l'ecran
            if (angleX > 1) {
                angleX = 1.0
            } else if (angleX < -1) {
                angleX = -1.0
            }
            if (angleY > 1) {
                angleY = 1.0
            } else if (angleY < -1) {
                angleY = -1.0
            }
        }
    }

    fun onTouch(touchX: Int, touchY: Int) {
        if (lockEnabled) {
            if (((orientation == Orientation.TOP || orientation == Orientation.LANDING)
                        && lockRect.contains(touchX, touchY))
                || (orientation == Orientation.BOTTOM
                        && lockRect.contains(touchX, canvasHeight - touchY))
                || (orientation == Orientation.RIGHT
                        && lockRect.contains(
                    middleX - (middleY - touchY),
                    middleY - (touchX - middleX)
                ))
                || (orientation == Orientation.LEFT
                        && lockRect.contains(
                    middleX - (middleY - touchY),
                    canvasHeight - (middleY - (touchX - middleX))
                ))
            ) {
                locked = !locked
                provider!!.setLocked(locked)
            }
        }
    }

    companion object {
        private const val LEVEL_ASPECT_RATIO = 0.150
        private const val BUBBLE_WIDTH = 0.150
        private const val BUBBLE_ASPECT_RATIO = 1.000
        private const val BUBBLE_CROPPING = 0.500
        private const val MARKER_GAP = BUBBLE_WIDTH + 0.020

        /**
         * Angle max
         */
        private val MAX_SINUS = sin(Math.PI / 4)

        /**
         * Fonts and colors
         */
        private const val FONT_LCD = "fonts/lcd.ttf"

        /**
         * Locked
         */
        private const val LOCKED = "LOCKED"
        private const val LOCKED_BACKGROUND = "888888"
        private var angle1DispValue = 0f
        private var angle2DispValue = 0f
        private var angleDispUpdateTime = 0L
        private const val angleDispInterval = 300L
    }
}
