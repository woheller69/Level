package me.greenrobot.apps.level.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import androidx.core.content.ContextCompat
import me.greenrobot.apps.level.R // taken from https://github.com/SecUSo/privacy-friendly-ruler, published under GPL3.0 license

class RulerView(context: Context?, var dpmm: Double, var dpfi: Double) : View(context) {
    var heightPx: Double = 0.0
    var heightmm: Double = 0.0
    var heightFracInch: Double = 0.0
    var widthPx: Double = 0.0
    var lineWidth: Float
    var textSize: Int
    var db: Int = ContextCompat.getColor(context!!, R.color.black)

    init {
        textSize = (dpmm * 2.5).toInt()
        lineWidth = (dpmm * 0.15).toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        heightPx = this.height.toDouble()
        widthPx = this.width.toDouble()
        heightmm = heightPx / dpmm
        heightFracInch = heightPx / dpfi

        val paint = Paint()
        paint.isAntiAlias = true
        paint.color = db
        paint.alpha = 255
        paint.textSize = textSize.toFloat()
        paint.strokeWidth = lineWidth

        drawLeftCm(canvas, paint)
        drawRightIn(canvas, paint)
    }

    private fun drawLeftCm(canvas: Canvas, paint: Paint) {
        var i = 0
        while (i < heightmm) {
            if (i % 10 == 0) {
                //draw 8mm line every cm
                canvas.drawLine(
                    0f,
                    dpmm.toFloat() * i,
                    dpmm.toFloat() * 8,
                    dpmm.toFloat() * i,
                    paint
                )
                //draw a number every cm
                canvas.drawText(
                    "" + i / 10,
                    dpmm.toFloat() * 8 + (textSize / 5f),
                    (dpmm * i + textSize).toFloat(),
                    paint
                )
            } else if (i % 5 == 0) {
                //draw 5mm line every 5mm
                canvas.drawLine(
                    0f,
                    dpmm.toFloat() * i,
                    dpmm.toFloat() * 5,
                    dpmm.toFloat() * i,
                    paint
                )
            } else {
                //draw 3mm line every mm
                canvas.drawLine(
                    0f,
                    dpmm.toFloat() * i,
                    dpmm.toFloat() * 3,
                    dpmm.toFloat() * i,
                    paint
                )
            }
            i++
        }
    }

    private fun drawRightIn(canvas: Canvas, paint: Paint) {
        val path = Path()

        var i = 0
        while (i < (heightFracInch)) {
            if (i % 32 == 0) {
                //draw 8mm line every inch
                canvas.drawLine(
                    (widthPx - dpmm * (8)).toFloat(), (heightPx - dpfi * (i)).toFloat(),
                    widthPx.toFloat(), (heightPx - dpfi * (i)).toFloat(), paint
                )
                //draw a number every inch
                path.reset()
                path.moveTo(
                    (widthPx - dpmm * 8 - textSize / 5).toFloat(),
                    (heightPx - dpfi * i - textSize * 0.25).toFloat()
                )
                path.lineTo(
                    (widthPx - dpmm * 8 - textSize / 5).toFloat(),
                    (heightPx - dpfi * (i) - textSize).toFloat()
                )
                canvas.drawTextOnPath("" + i / 32, path, 0f, 0f, paint)
            } else if (i % 16 == 0) {
                //draw 6mm line every 1/2 inch
                canvas.drawLine(
                    (widthPx - dpmm * (6)).toFloat(), (heightPx - dpfi * (i)).toFloat(),
                    widthPx.toFloat(), (heightPx - dpfi * (i)).toFloat(), paint
                )
            } else if (i % 8 == 0) {
                //draw 4mm line every 1/4 inch
                canvas.drawLine(
                    (widthPx - dpmm * (4)).toFloat(), (heightPx - dpfi * (i)).toFloat(),
                    widthPx.toFloat(), (heightPx - dpfi * (i)).toFloat(), paint
                )
            } else if (i % 4 == 0) {
                //draw 3mm line every 1/8 inch
                canvas.drawLine(
                    (widthPx - dpmm * (3)).toFloat(), (heightPx - dpfi * (i)).toFloat(),
                    widthPx.toFloat(), (heightPx - dpfi * (i)).toFloat(), paint
                )
            } else if (i % 2 == 0) {
                //draw 2mm line every 1/16 inch
                canvas.drawLine(
                    (widthPx - dpmm * (2)).toFloat(), (heightPx - dpfi * (i)).toFloat(),
                    widthPx.toFloat(), (heightPx - dpfi * (i)).toFloat(), paint
                )
            } else {
                //draw 1.5mm line every 1/32 inch
                canvas.drawLine(
                    (widthPx - dpmm * (1.5)).toFloat(), (heightPx - dpfi * (i)).toFloat(),
                    widthPx.toFloat(), (heightPx - dpfi * (i)).toFloat(), paint
                )
            }
            i++
        }
    }

    fun setCalib(ydpmm: Double, ydpi: Double) {
        dpmm = ydpmm
        dpfi = ydpi
        invalidate()
        requestLayout()
    }
}
