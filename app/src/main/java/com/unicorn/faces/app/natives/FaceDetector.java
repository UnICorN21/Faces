package com.unicorn.faces.app.natives;

import android.graphics.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by Huxley on 5/9/15.
 */
public class FaceDetector {
    static {
        System.loadLibrary("faces");
    }

    public class Face {
        public int x;
        public int y;
        public int width;
        public int height;
    }

    private static FaceDetector instance;

    private FaceDetector() { /* null */ }

    public static FaceDetector getSingleton() {
        if (null == instance) instance = new FaceDetector();
        return instance;
    }

    public native boolean load(String cascadeFile);
    public native boolean empty();
    public native Face[] findFaces(byte[] data, int length, int orientation, boolean fixed);

    /*
     * Save Image with faces found on it.
     * @param imgFile: The File object using for saving image.
     */
    public Face[] saveImage(File imgFile, byte[] data, int length, int orientation, boolean fixed)
            throws FileNotFoundException, RuntimeException {
        Face[] faces = findFaces(data, length, orientation, fixed);
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, length);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(0xff00b4ff);
        paint.setStrokeWidth(3);
        paint.setStyle(Paint.Style.STROKE);
        for (Face face: faces) {
            Rect rect = new Rect();
            rect.set(face.x, face.y, face.x + face.width, face.y + face.height);
            canvas.drawRect(rect, paint);
        }
        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();
        FileOutputStream fos = new FileOutputStream(imgFile);
        if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)) {
            throw new RuntimeException("Save image file failed.");
        }

        return faces;
    }
}
