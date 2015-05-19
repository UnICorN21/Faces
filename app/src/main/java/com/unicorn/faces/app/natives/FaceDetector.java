package com.unicorn.faces.app.natives;

import android.graphics.*;

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
        public int x;
        public int y;
        public int width;
        public int height;
    }

    private int orientation; // set by native code
    private static FaceDetector instance;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

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
    public FutureTask<Face[]> saveImage(final File imgFile, final byte[] data, final int length, final boolean fixed)
            throws FileNotFoundException, RuntimeException {
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
