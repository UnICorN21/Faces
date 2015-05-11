package com.unicorn.faces.app.views;

import android.content.Context;
import android.graphics.*;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;
import com.faceplusplus.api.FaceDetecter;
import com.unicorn.faces.app.views.activities.MainActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
    private Camera.CameraInfo mCameraInfo;
    private FaceMask mFaceMask;

    public static final String API_KEY = "aa558358150dfc9f4610010d4324b826";

    private FaceDetecter mFaceDetecter;

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private FutureTask<FaceDetecter.Face[]> mDetectFuture;
    private Long lastDetectTime;

    public CameraPreview(Context context, FaceMask faceMask) {
        super(context);
        mContext = context;
        mFaceMask = faceMask;
        mCameraInfo = new Camera.CameraInfo();

        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setKeepScreenOn(true);

        mFaceDetecter = new FaceDetecter();
        mFaceDetecter.init(context, API_KEY);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            int cameraIdx = 0;
            Camera.getCameraInfo(cameraIdx, mCameraInfo);
            mCamera = Camera.open(cameraIdx);
            mCamera.setPreviewDisplay(surfaceHolder);
            Camera.Parameters params = mCamera.getParameters();
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            mCamera.setParameters(params);
            mCamera.startPreview();
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
                mDetectFuture = new FutureTask<FaceDetecter.Face[]>(new Callable<FaceDetecter.Face[]>() {
                    @Override
                    public FaceDetecter.Face[] call() throws Exception {
                        YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                        ByteArrayOutputStream os = new ByteArrayOutputStream(data.length);
                        if (!image.compressToJpeg(new Rect(0, 0, size.width, size.height), 100, os)) {
                            throw new RuntimeException("Cannot cast camera preview to jpeg.");
                        }
                        Bitmap bitmap = BitmapFactory.decodeByteArray(os.toByteArray(), 0, os.toByteArray().length);

                        Matrix matrix = new Matrix();
                        matrix.postRotate(mCameraInfo.orientation);
                        Bitmap procImage = Bitmap.createBitmap(bitmap, 0, 0,
                                bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                        FaceDetecter.Face[] faces = mFaceDetecter.findFaces(procImage);

                        bitmap.recycle();
                        procImage.recycle();
                        return faces;
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
