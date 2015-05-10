package com.unicorn.faces.app.natives;

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
    public native Face[] findFaces(byte[] data, int width, int height);

    public static void main(String [] args) {
        try {
            FaceDetector detector = FaceDetector.getSingleton();
            boolean ok = detector.load("haarcascade_frontalface_default.xml");
            System.out.println(!detector.empty());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
