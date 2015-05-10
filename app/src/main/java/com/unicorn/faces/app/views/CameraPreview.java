package com.unicorn.faces.app.views;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.nfc.Tag;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;
import com.unicorn.faces.app.R;
import com.unicorn.faces.app.natives.FaceDetector;
import com.unicorn.faces.app.views.activities.MainActivity;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
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
    private FaceMask mFaceMask;
    private boolean flashOn;

    private FaceDetector mDetector;

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private FutureTask<FaceDetector.Face[]> mDetectFuture;
    private Long lastDetectTime;

    public CameraPreview(Context context, FaceMask faceMask) {
        super(context);
        mContext = context;
        mFaceMask = faceMask;
        flashOn = false;

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
            int frontCameraIdx = -1;
            for (int i = 0; i < Camera.getNumberOfCameras(); ++i) {
                Camera.getCameraInfo(i, cameraInfo);
                if (Camera.CameraInfo.CAMERA_FACING_FRONT == cameraInfo.facing) frontCameraIdx = i;
            }

            mCamera = Camera.open(frontCameraIdx);
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();

            Camera.Parameters params = mCamera.getParameters();
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int w, int h) {
        if (null == mHolder.getSurface()) return;
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // Ignored.
        }

        mCamera.setDisplayOrientation(90);
        mCamera.setPreviewCallback(this);

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
            if (flashOn) switchFlash();
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
            if (null == mDetectFuture && (null == lastDetectTime || 2000 < now - lastDetectTime)) {
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

                        return mDetector.findFaces(bytes, bytes.length);
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

    public void switchFlash() {
        Camera.Parameters params = mCamera.getParameters();
        if (flashOn) params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        else params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(params);
        flashOn = !flashOn;
    }
}
