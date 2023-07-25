package org.woheller69.level.view;

// taken from https://github.com/SecUSo/privacy-friendly-ruler, published under GPL3.0 license

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import androidx.core.content.ContextCompat;
import org.woheller69.level.R;

public class RulerView extends View {

    double dpmm;
    double heightPx;
    double heightmm;
    double heightFracInch;
    double widthPx;
    double dpfi;
    float lineWidth;
    int textSize ;
    int db;

    public RulerView(Context context, double ydpmm, double ydpi) {
        super(context);

        dpmm = ydpmm;
        dpfi = ydpi;

        db = ContextCompat.getColor(context, R.color.black);
        textSize = (int)(dpmm *2.5);
        lineWidth = (float)(dpmm*0.15);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        heightPx = this.getHeight();
        widthPx = this.getWidth();
        heightmm = heightPx/dpmm;
        heightFracInch = heightPx/dpfi;

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(db);
        paint.setAlpha(255);
        paint.setTextSize(textSize);
        paint.setStrokeWidth(lineWidth);

        drawLeftCm(canvas, paint);
        drawRightIn(canvas, paint);

    }

    private void drawLeftCm(Canvas canvas, Paint paint){
        for (int i = 0; i < heightmm; i++){
            if (i%10 == 0) {
                //draw 8mm line every cm
                canvas.drawLine(0, (float)dpmm*i, (float)dpmm*8, (float)dpmm*i, paint);
                //draw a number every cm
                canvas.drawText(""+i/10, (float)dpmm*8+(textSize/5), (float)(dpmm*i+textSize), paint);
            } else if (i%5 == 0) {
                //draw 5mm line every 5mm
                canvas.drawLine(0, (float)dpmm*i, (float)dpmm*5, (float)dpmm*i, paint);
            } else {
                //draw 3mm line every mm
                canvas.drawLine(0, (float)dpmm*i, (float)dpmm*3, (float)dpmm*i, paint);
            }
        }
    }

    private void drawRightIn(Canvas canvas, Paint paint){
        Path path = new Path();

        for (int i = 0; i < (heightFracInch); i++){
            if (i%32 == 0) {
                //draw 8mm line every inch
                canvas.drawLine((float)(widthPx-dpmm*(8)), (float)(heightPx- dpfi *(i)),
                        (float)(widthPx), (float)(heightPx-dpfi*(i)), paint);
                //draw a number every inch
                path.reset();
                path.moveTo((float)(widthPx-dpmm*(8)-textSize/5), (float)(heightPx- dpfi *(i)-textSize*0.25));
                path.lineTo((float)(widthPx-dpmm*(8)-textSize/5), (float)(heightPx- dpfi *(i)-textSize));
                canvas.drawTextOnPath(""+i/32, path, 0, 0, paint);
            } else if (i%16 == 0) {
                //draw 6mm line every 1/2 inch
                canvas.drawLine((float)(widthPx-dpmm*(6)), (float)(heightPx- dpfi *(i)),
                        (float)(widthPx), (float)(heightPx- dpfi *(i)), paint);
            } else if (i%8 == 0) {
                //draw 4mm line every 1/4 inch
                canvas.drawLine((float)(widthPx-dpmm*(4)), (float)(heightPx- dpfi *(i)),
                        (float)(widthPx), (float)(heightPx- dpfi *(i)), paint);
            } else if (i%4 == 0) {
                //draw 3mm line every 1/8 inch
                canvas.drawLine((float)(widthPx-dpmm*(3)), (float)(heightPx- dpfi *(i)),
                        (float)(widthPx), (float)(heightPx- dpfi *(i)), paint);
            } else if (i%2 == 0) {
                //draw 2mm line every 1/16 inch
                canvas.drawLine((float)(widthPx-dpmm*(2)), (float)(heightPx- dpfi *(i)),
                        (float)(widthPx), (float)(heightPx- dpfi *(i)), paint);
            } else {
                //draw 1.5mm line every 1/32 inch
                canvas.drawLine((float)(widthPx-dpmm*(1.5)), (float)(heightPx- dpfi *(i)),
                        (float)(widthPx), (float)(heightPx- dpfi *(i)), paint);
            }
        }
    }

    public void setCalib(double ydpmm, double ydpi) {
        dpmm=ydpmm;
        dpfi=ydpi;
        invalidate();
        requestLayout();
    }
}
