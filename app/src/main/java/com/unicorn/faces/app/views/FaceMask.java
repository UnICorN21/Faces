package com.unicorn.faces.app.views;

/**
 * Created by Huxley on 5/7/15.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import com.unicorn.faces.app.natives.FaceDetector;

public class FaceMask extends View {
    Paint localPaint = null;
    FaceDetector.Face[] faces = null;
    RectF rect = null;
    boolean portrait = true;

    public FaceMask(Context context) {
        super(context);
        rect = new RectF();
        localPaint = new Paint();
        localPaint.setColor(0xff00b4ff);
        localPaint.setStrokeWidth(3);
        localPaint.setStyle(Paint.Style.STROKE);
    }

    public void setFaceInfo(FaceDetector.Face[] faceinfos) {
        this.faces = faceinfos;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (faces == null)
            return;
        int width = canvas.getWidth(), height = canvas.getHeight();
        if (!portrait) {
            width = width ^ height;
            height = width ^ height;
            width = width ^ height;
        }
        for (FaceDetector.Face face : faces) {
            rect.set(width * face.left, height * face.top,
                    width * face.right, height * face.bottom);
            canvas.drawRect(rect, localPaint);
        }
    }
}