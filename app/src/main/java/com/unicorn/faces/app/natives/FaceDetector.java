package com.unicorn.faces.app.natives;

import android.content.Context;
import android.graphics.*;
import android.view.OrientationEventListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * Created by Huxley on 5/9/15.
 */
public class FaceDetector {
    static {
        System.loadLibrary("faces");
    }

    public class Face {
        public float left;
        public float top;
        public float right;
        public float bottom;
    }

    private int orientation; // set by native code
    private static FaceDetector instance;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public static int face_count=0;

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
    public FutureTask<Face[]> saveImage(final File imgFile, final byte[] data, final int length, final boolean fixed,
                                        final int deviceOrientation) throws FileNotFoundException, RuntimeException {
        FutureTask<Face[]> futureTask = new FutureTask<Face[]>(new Callable<Face[]>() {
            @Override
            public Face[] call() throws Exception {
                Face[] faces = findFaces(data, length, orientation, fixed);
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, length).copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(bitmap);
                Paint paint = new Paint();
                paint.setColor(0xff00b4ff);
                paint.setStrokeWidth(3);
                paint.setStyle(Paint.Style.STROKE);
                for (Face face: faces) {
                    RectF rect = new RectF();
                    if (0 == deviceOrientation || 180 == deviceOrientation) {
                        rect.set(canvas.getHeight() * face.left, canvas.getWidth() * face.top,
                                canvas.getHeight() * face.right, canvas.getWidth() * face.bottom);
                    } else {
                        rect.set(canvas.getWidth() * face.left, canvas.getHeight() * face.top,
                                canvas.getWidth() * face.right, canvas.getHeight() * face.bottom);
                    }
                    canvas.drawRect(rect, paint);
                    face_count++;
                }
                canvas.save(Canvas.ALL_SAVE_FLAG);
                canvas.restore();
                FileOutputStream fos = new FileOutputStream(imgFile);
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)) {
                    throw new RuntimeException("Save image file failed.");
                }
                bitmap.recycle();
                return faces;
            }
        });
        submit(futureTask);
        return futureTask;
    }

    public void submit(FutureTask<Face[]> task) {
        executor.submit(task);
    }
}
