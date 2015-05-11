package com.unicorn.faces.app.views;

/**
 * Created by Huxley on 5/7/15.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import com.faceplusplus.api.FaceDetecter;

public class FaceMask extends View {
    Paint localPaint = null;
    FaceDetecter.Face[] faces = null;
    RectF rect = null;

    public FaceMask(Context context) {
        super(context);
        rect = new RectF();
        localPaint = new Paint();
        localPaint.setColor(0xff00b4ff);
        localPaint.setStrokeWidth(3);
        localPaint.setStyle(Paint.Style.STROKE);
    }

    public void setFaceInfo(FaceDetecter.Face[] faceinfos)
    {
        this.faces = faceinfos;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (faces == null)
            return;
        for (FaceDetecter.Face localFaceInfo : faces) {
            rect.set(getWidth() * localFaceInfo.left, getHeight()
                            * localFaceInfo.top, getWidth() * localFaceInfo.right,
                    getHeight()
                            * localFaceInfo.bottom);
            canvas.drawRect(rect, localPaint);
        }
    }
}