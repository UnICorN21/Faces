package com.unicorn.faces.app.natives;

import android.graphics.Bitmap;

/**
 * Created by Huxley on 5/9/15.
 */
public class FaceDetector {
    public class Face {
        public float topLeft;
        public float topRight;
        public float bottomLeft;
        public float bottomRight;
    }

    private native boolean init(String cascadeFile);

    public FaceDetector() {
        // TODO
    }

    public native Face[] findFaces(Bitmap bitmap);
}
