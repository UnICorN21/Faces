package com.unicorn.faces.app.views;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Huxley on 5/6/15.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback,
        Camera.PreviewCallback, Camera.AutoFocusCallback {

    private static final String TAG = "faces";

    private Context mContext;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private FaceMask mFaceMask;

    public static final String API_KEY = "aa558358150dfc9f4610010d4324b826";

//    private FaceDetecter mFaceDetecter;

    private ExecutorService executor = Executors.newSingleThreadExecutor();
//    private FutureTask<FaceDetecter.Face[]> mDetectFuture;
    private Long lastDetectTime;

    @Override
    public void onAutoFocus(boolean isSuccess, Camera camera) {
        if (!isSuccess) {
            Log.d(TAG, "Camera auto focus failed");
        }
    }

    public CameraPreview(Context context, FaceMask faceMask) {
        super(context);
        mContext = context;
        mFaceMask = faceMask;

        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setKeepScreenOn(true);

//        mFaceDetecter = new FaceDetecter();
//        mFaceDetecter.init(context, API_KEY);
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
        mCamera.autoFocus(this);

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
//            if (null == mDetectFuture && (null == lastDetectTime || 2000 < now - lastDetectTime)) {
//                lastDetectTime = now;
//                mDetectFuture = new FutureTask<FaceDetecter.Face[]>(new Callable<FaceDetecter.Face[]>() {
//                    @Override
//                    public FaceDetecter.Face[] call() throws Exception {
//                        YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
//                        ByteArrayOutputStream os = new ByteArrayOutputStream(data.length);
//                        if (!image.compressToJpeg(new Rect(0, 0, size.width, size.height), 100, os)) {
//                            throw new RuntimeException("Cannot cast camera preview to jpeg.");
//                        }
//                        Bitmap bitmap = BitmapFactory.decodeByteArray(os.toByteArray(), 0, os.toByteArray().length);
//
//                        FileOutputStream fos = null;
//                        try {
//                            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
//                                    Environment.DIRECTORY_PICTURES), "Faces");
//
//                            // Create the storage directory if it does not exist
//                            if (! mediaStorageDir.exists()){
//                                if (! mediaStorageDir.mkdirs()){
//                                    Log.d(TAG, "failed to create directory");
//                                    return null;
//                                }
//                            }
//
//                            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//                            fos = new FileOutputStream(mediaStorageDir.getPath() + File.separator + "IMG_"+ timeStamp +".jpg");
//                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
//                        } catch (Exception e) {
//                            Log.d(TAG, e.getMessage());
//                        }
//
//                        FaceDetecter.Face[] faces = mFaceDetecter.findFaces(bitmap);
//                        return faces;
//                    }
//                });
//                executor.execute(mDetectFuture);
//            } else if (mDetectFuture.isDone()) {
//                mFaceMask.setFaceInfo(mDetectFuture.get());
//                mDetectFuture = null;
//            }

        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
            // Ignored, the camera may be released.
        }
    }
}
