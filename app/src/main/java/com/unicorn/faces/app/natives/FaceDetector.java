package com.unicorn.faces.app.natives;

/**
 * Created by Huxley on 5/9/15.
 */
public class FaceDetector {
    static {
        System.loadLibrary("libfaces");
    }

    public class Face {
        public float topLeft;
        public float topRight;
        public float bottomLeft;
        public float bottomRight;
    }

    public static native void load(String cascadeFile);
    public static native Face[] findFaces(byte[] data, int width, int height);
}