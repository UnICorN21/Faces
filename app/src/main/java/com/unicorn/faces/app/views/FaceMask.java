package com.unicorn.faces.app.views;

/**
 * Created by Huxley on 5/7/15.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import com.unicorn.faces.app.natives.FaceDetector;

public class FaceMask extends View {
    Paint localPaint = null;
    FaceDetector.Face[] faces = null;
    Rect rect = null;

    public FaceMask(Context context) {
        super(context);
        rect = new Rect();
        localPaint = new Paint();
        localPaint.setColor(0xff00b4ff);
        localPaint.setStrokeWidth(5);
        localPaint.setStyle(Paint.Style.STROKE);
    }

    public void setFaceInfo(FaceDetector.Face[] faceinfos)
    {
        this.faces = faceinfos;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (faces == null)
            return;
        for (FaceDetector.Face face : faces) {
           rect.set(face.x, face.y, face.x + face.width, face.y + face.height);
            canvas.drawRect(rect, localPaint);
        }
    }
}