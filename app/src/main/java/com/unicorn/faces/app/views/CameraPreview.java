package com.unicorn.faces.app.views;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.view.*;
import android.widget.Toast;
import com.unicorn.faces.app.R;
import com.unicorn.faces.app.natives.FaceDetector;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
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
    private int cameraOrientation;

    private boolean focusViewSet = false;
    private FocusView focusView;
    private FaceMask mFaceMask;

    private FaceDetector mDetector;
    private int detectOrientation = 0;
    private FutureTask<FaceDetector.Face[]> mDetectFuture;
    private Long lastDetectTime;

    private int pictureWidth,pictureHeight;

    private OrientationEventListener mOrientationEventListener;

    Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback(){

        @Override
        public void onAutoFocus(boolean arg0, Camera arg1) {
            if (arg0){
                mCamera.cancelAutoFocus();
            }
        }
    };

    public CameraPreview(Context context, FaceMask faceMask) {
        super(context);
        mContext = context;
        mFaceMask = faceMask;

        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setKeepScreenOn(true);

        mOrientationEventListener = new OrientationEventListener(mContext) {
            @Override
            public void onOrientationChanged(int orientation) {
                orientation = (int)Math.round(orientation / 90.0) * 90;
                if (orientation > 180) detectOrientation = 0;
                else detectOrientation = (cameraOrientation - orientation + 360) % 360;
                Log.d("faceori", String.format("Current Detect Orientation := %d", detectOrientation));
            }
        };

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
        setCameraFaceDirection(1);
        mOrientationEventListener.enable();
    }

    // Swicth the direction of the camera.
    public void setCameraFaceDirection(int index){
        if(mCamera != null){
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
        try {
            mCamera = Camera.open(index);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.setPreviewCallback(this);

            Camera.Parameters params = mCamera.getParameters();
            if(pictureWidth != 0 && pictureHeight != 0) params.setPictureSize(pictureWidth, pictureHeight);
            if (params.getMaxNumFocusAreas() == 0 && focusViewSet) focusViewSet = false;
            else if (params.getMaxNumFocusAreas() > 0 && null != focusView) focusViewSet = true;
            mCamera.setParameters(params);

            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(index, cameraInfo);
            int rotation = ((Activity)mContext).getWindowManager().getDefaultDisplay().getRotation();
            int degrees = 0;
            switch (rotation) {
                case Surface.ROTATION_0: degrees = 0; break;
                case Surface.ROTATION_90: degrees = 90; break;
                case Surface.ROTATION_180: degrees = 180; break;
                case Surface.ROTATION_270: degrees = 270; break;
            }
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                degrees = (cameraInfo.orientation + degrees) % 360;
                degrees = (360 - degrees) % 360;  // compensate the mirror
            } else {  // back-facing
                degrees = (cameraInfo.orientation - degrees + 360) % 360;
            }
            cameraOrientation = cameraInfo.orientation;
            mCamera.setDisplayOrientation(degrees);

            mCamera.startPreview();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

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
        pictureWidth=optSize.width;
        pictureHeight=optSize.height;
        params.setPreviewSize(pictureWidth,  pictureHeight);
        params.setPictureSize(pictureWidth,  pictureHeight);
        if (params.getMaxNumFocusAreas() > 0) {
            ArrayList<Camera.Area> focusAreas = new ArrayList<Camera.Area>(1);
            focusAreas.add(new Camera.Area(new Rect(-1000, -1000, 1000, 0), 750));
            params.setFocusAreas(focusAreas);
        }

        mCamera.setParameters(params);
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
            mOrientationEventListener.disable();
            if (mDetectFuture != null && !mDetectFuture.isDone()) mDetectFuture.cancel(true);
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

                        return mDetector.findFaces(bytes, bytes.length, detectOrientation, false);
                    }
                });

                mDetector.submit(mDetectFuture);
            } else if (mDetectFuture.isDone()) {
                mFaceMask.setFaceInfo(mDetectFuture.get());
                mDetectFuture = null;
            }

        } catch (Exception e) {
            Log.d(TAG, "" + e.getMessage());
            // Ignored, the camera may be released.
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            float x = event.getX();
            float y = event.getY();

            Rect touchRect = new Rect(
                    (int)(x - 100),
                    (int)(y - 100),
                    (int)(x + 100),
                    (int)(y + 100));

            final Rect targetFocusRect = new Rect(
                    touchRect.left * 2000/this.getWidth() - 1000,
                    touchRect.top * 2000/this.getHeight() - 1000,
                    touchRect.right * 2000/this.getWidth() - 1000,
                    touchRect.bottom * 2000/this.getHeight() - 1000);

            doTouchFocus(targetFocusRect);
            if (focusViewSet) {
                focusView.setHaveTouch(true, touchRect);
                focusView.invalidate();

                // Remove the square after some time
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        focusView.setHaveTouch(false, new Rect(0, 0, 0, 0));
                        focusView.invalidate();
                    }
                }, 1000);
            }
        }
        return false;
    }

    /**
     * Called from PreviewSurfaceView to set touch focus.
     * @param - Rect - new area for auto focus
     */
    public void doTouchFocus(final Rect tfocusRect) {
        try {
            List<Camera.Area> focusList = new ArrayList<Camera.Area>();
            Camera.Area focusArea = new Camera.Area(tfocusRect, 1000);
            focusList.add(focusArea);

            Camera.Parameters param = mCamera.getParameters();
            param.setFocusAreas(focusList);
            param.setMeteringAreas(focusList);
            mCamera.setParameters(param);

            mCamera.autoFocus(autoFocusCallback);
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "Unable to autofocus");
        }
    }

    public void setFocusView(FocusView fView) {
        focusView = fView;
        focusViewSet = true;
    }
}
