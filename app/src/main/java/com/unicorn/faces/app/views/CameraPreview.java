package com.unicorn.faces.app.views;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.hardware.Camera;
import android.nfc.Tag;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;
import com.unicorn.faces.app.R;
import com.unicorn.faces.app.natives.FaceDetector;
import com.unicorn.faces.app.views.activities.MainActivity;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * Created by Huxley on 5/6/15.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback,
        Camera.PreviewCallback {

    private static final String TAG = "faces";

    private Context mContext;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Camera.CameraInfo mCameraInfo;
    private FaceMask mFaceMask;

    private FaceDetector mDetector;

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private FutureTask<FaceDetector.Face[]> mDetectFuture;
    private Long lastDetectTime;

    public CameraPreview(Context context, FaceMask faceMask) {
        super(context);
        mContext = context;
        mFaceMask = faceMask;
        mCameraInfo = new Camera.CameraInfo();

        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setKeepScreenOn(true);

        mDetector = FaceDetector.getSingleton();
        if (mDetector.empty()) {
            try {
                InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_default);
                File cascadeDir = ((Activity)context).getDir("cascade", Context.MODE_PRIVATE);
                File cascadeFile = new File(cascadeDir, "haarcascade_frontalface_default.xml");
                FileOutputStream os = new FileOutputStream(cascadeFile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                is.close();
                os.close();

                if (!mDetector.load(cascadeFile.getAbsolutePath()))
                    throw new RuntimeException("Native method failed");
            } catch (Exception e) {
                Log.d(TAG, "Load cascade file failed: " + e.getMessage());
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            int cameraIdx = 0;
            for (int i = 0; i < Camera.getNumberOfCameras(); ++i) {
                Camera.getCameraInfo(i, cameraInfo);
                Log.d("libfaces", String.format("Camera[%d]{orientation = %d}", i, cameraInfo.orientation));
                if (Camera.CameraInfo.CAMERA_FACING_FRONT == cameraInfo.facing) cameraIdx = i;
            }

            mCamera = Camera.open(cameraIdx);
            mCamera.setPreviewDisplay(surfaceHolder);

            Camera.Parameters params = mCamera.getParameters();
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            Camera.getCameraInfo(cameraIdx, mCameraInfo);
            mCamera.setParameters(params);

            int rotation = ((Activity)mContext).getWindowManager().getDefaultDisplay().getRotation();
            int degrees = 0;
            switch (rotation) {
                case Surface.ROTATION_0: degrees = 0; break;
                case Surface.ROTATION_90: degrees = 90; break;
                case Surface.ROTATION_180: degrees = 180; break;
                case Surface.ROTATION_270: degrees = 270; break;
            }
            if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                degrees = (mCameraInfo.orientation + degrees) % 360;
                degrees = (360 - degrees) % 360;  // compensate the mirror
            } else {  // back-facing
                degrees = (mCameraInfo.orientation - degrees + 360) % 360;
            }
            mCamera.setDisplayOrientation(degrees);

            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            mCamera.release();
            mCamera = null;
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double)h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int w, int h) {
        if (null == mHolder.getSurface()) return;
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // Ignored.
        }

        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        Camera.Size optSize = getOptimalPreviewSize(sizes, h, w);
        params.setPreviewSize(optSize.width, optSize.height);
        mCamera.setParameters(params);
        mCamera.setPreviewCallback(this);

        params = mCamera.getParameters();
        Log.d("libfaces", params.getPreviewSize().toString());

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        try {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        } catch (Exception e) {
            Log.d(TAG, "Error destroying camera: " + e.getMessage());
        }
    }

    public void takePicture(Camera.ShutterCallback shutter, Camera.PictureCallback raw, Camera.PictureCallback jpeg) {
        mCamera.takePicture(shutter, raw, jpeg);
        Toast.makeText(getContext(), "Captured", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPreviewFrame(final byte[] data, Camera camera) {
        try {
            final Camera.Size size = camera.getParameters().getPreviewSize();
            long now = System.currentTimeMillis();
            if (null == mDetectFuture && (null == lastDetectTime || 800 < now - lastDetectTime)) {
                lastDetectTime = now;
                mDetectFuture = new FutureTask<FaceDetector.Face[]>(new Callable<FaceDetector.Face[]>() {
                    @Override
                    public FaceDetector.Face[] call() throws Exception {
                        YuvImage img = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        if (!img.compressToJpeg(new Rect(0, 0, size.width, size.height), 100, baos)) {
                            throw new RuntimeException("Can't convert YUV to JPEG.");
                        }
                        byte[] bytes = baos.toByteArray();
//                        Following are debugging settings.
//                        File file = MainActivity.getCapturedImageFile();
//                        FileOutputStream fos = new FileOutputStream(file);
//                        fos.write(bytes);
//                        fos.close();

                        return mDetector.findFaces(bytes, bytes.length, mCameraInfo.orientation, false);
                    }
                });

                executor.execute(mDetectFuture);
            } else if (mDetectFuture.isDone()) {
                mFaceMask.setFaceInfo(mDetectFuture.get());
                mDetectFuture = null;
            }

        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
            // Ignored, the camera may be released.
        }
    }
}
