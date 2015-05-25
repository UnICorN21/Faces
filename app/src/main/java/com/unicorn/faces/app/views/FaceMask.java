package com.unicorn.faces.app.views;

/**
 * Created by Huxley on 5/7/15.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.OrientationEventListener;
import android.view.View;
import com.unicorn.faces.app.natives.FaceDetector;

public class FaceMask extends View {
    private enum Origin { LEFTTOP, RIGHTTOP, LEFTBOTTOM, RIGHTBOTTOM }

    private Paint localPaint = null;
    private FaceDetector.Face[] faces = null;
    private RectF rect = null;
    private Origin origin = Origin.LEFTTOP;

    private OrientationEventListener mOrientationEventListener;

    public FaceMask(Context context) {
        super(context);
        rect = new RectF();
        localPaint = new Paint();
        localPaint.setColor(0xff00b4ff);
        localPaint.setStrokeWidth(3);
        localPaint.setStyle(Paint.Style.STROKE);

        mOrientationEventListener = new OrientationEventListener(getContext()) {
            @Override
            public void onOrientationChanged(int orientation) {
                orientation = (int)Math.round(orientation / 90.0) * 90;
                switch (orientation) {
                    case 0: origin = Origin.LEFTTOP; break;
                    case 90: origin = Origin.LEFTBOTTOM; break;
                    case 180: origin = Origin.RIGHTBOTTOM; break;
                    case 270: origin = Origin.RIGHTTOP; break;
                }
            }
        };
        mOrientationEventListener.enable();
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
        for (FaceDetector.Face face : faces) {
            switch (origin) {
                case LEFTTOP:
                    rect.set(canvas.getWidth() * face.left, canvas.getHeight() * face.top,
                            canvas.getWidth() * face.right, canvas.getHeight() * face.bottom);
                    break;
                case LEFTBOTTOM:
                    rect.set(canvas.getWidth() * face.top, canvas.getHeight() * (1 -face.right),
                            canvas.getWidth() * face.bottom, canvas.getHeight() * (1 - face.left));
                    break;
                case RIGHTBOTTOM:
                    rect.set(canvas.getWidth() * (1 - face.right), canvas.getHeight() * (1 -face.bottom),
                            canvas.getWidth() * (1 - face.left), canvas.getHeight() * (1 - face.top));
                    break;
                case RIGHTTOP:
                    rect.set(canvas.getWidth() * (1 - face.bottom), canvas.getHeight() * face.left,
                            canvas.getWidth() * (1 - face.top), canvas.getHeight() * face.right);
                    break;
            }
            canvas.drawRect(rect, localPaint);
        }
    }
}