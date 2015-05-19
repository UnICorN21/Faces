package com.unicorn.faces.app.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

/**
 * Created by Huxley on 5/18/15.
 */

public class FocusView extends View {
    private static final int LEN = 50;

    private boolean haveTouch = false;
    private Rect touchArea;
    private Paint paint;

    public FocusView(Context context) {
        super(context);
        paint = new Paint();
        paint.setColor(0xeed7d7d7);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        haveTouch = false;
    }

    public void setHaveTouch(boolean val, Rect rect) {
        haveTouch = val;
        touchArea = rect;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if(haveTouch){
            canvas.drawLine(touchArea.left, touchArea.top+LEN, touchArea.left, touchArea.top, paint);
            canvas.drawLine(touchArea.left, touchArea.top, touchArea.left+LEN, touchArea.top, paint);

            canvas.drawLine(touchArea.right, touchArea.top+LEN, touchArea.right, touchArea.top, paint);
            canvas.drawLine(touchArea.right-LEN, touchArea.top, touchArea.right, touchArea.top, paint);

            canvas.drawLine(touchArea.left, touchArea.bottom-LEN, touchArea.left, touchArea.bottom, paint);
            canvas.drawLine(touchArea.left, touchArea.bottom, touchArea.left+LEN, touchArea.bottom, paint);

            canvas.drawLine(touchArea.right, touchArea.bottom-LEN, touchArea.right, touchArea.bottom, paint);
            canvas.drawLine(touchArea.right-LEN, touchArea.bottom, touchArea.right, touchArea.bottom, paint);
        }
    }

}