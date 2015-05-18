package com.unicorn.faces.app.views.activities;

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import com.unicorn.faces.app.R;
import com.unicorn.faces.app.natives.FaceDetector;
import com.unicorn.faces.app.views.CameraPreview;
import com.unicorn.faces.app.views.FaceMask;
import com.unicorn.faces.app.views.FocusView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends Activity {
    public static final String TAG = "faces";

    private CameraPreview mPreview;
    private FaceMask mFaceMask;
    private FocusView mFocusView;

    //count times of camera switch
    private int cameraSwitchTimes=-1;

    //camera switch animation
    private Animation mScaleInAnimation;
    private Animation mScaleOutAnimation;

    private FrameLayout preview;

    private PictureCallback mPicture = new PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getCapturedImageFile();
            if (pictureFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions.");
                return;
            }

            try {
                FaceDetector.getSingleton().saveImage(pictureFile, data, data.length, true);
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    public static File getCapturedImageFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Faces");

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator + "IMG_"+ timeStamp + ".jpg");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFaceMask = new FaceMask(this);
        mFocusView = new FocusView(this);
        mPreview = new CameraPreview(this, mFaceMask);
        mPreview.setFocusView(mFocusView);

        preview = (FrameLayout)findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        preview.addView(mFaceMask);
        preview.addView(mFocusView);

        mScaleInAnimation= AnimationUtils.loadAnimation(this, R.anim.scale_in);
        mScaleInAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mPreview.startAnimation(mScaleOutAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mScaleOutAnimation= AnimationUtils.loadAnimation(this,R.anim.scale_out);

        Button captureButton = (Button) findViewById(R.id.button_capture);
        Button cameraSwitchButton = (Button) findViewById(R.id.button_cameraSwitch);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mPreview.takePicture(null, null, mPicture);
                    }
                }
        );
        cameraSwitchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO
                cameraSwitchTimes++;
                preview.startAnimation(mScaleInAnimation);

                //switch camera
                mPreview.setCameraFaceDirection(cameraSwitchTimes % 2);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
